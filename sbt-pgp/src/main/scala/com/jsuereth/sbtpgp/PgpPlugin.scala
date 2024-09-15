package bleep.plugin.pgp

import bleep.RelPath
import bleep.logging.Logger
import bleep.nosbt.librarymanagement.ivy.DirectCredentials
import bleep.nosbt.{FileOps, InteractionService}

import java.io.File

/** SBT Keys for the PGP plugin. */
class PgpPlugin(val logger: Logger, val maybeCredentials: Option[DirectCredentials], val interactionService: InteractionService) {
  val gnuPGHome: File = scala.util.Properties.envOrNone("GNUPGHOME") match {
    case Some(dir) => new File(dir)
    case None      => new File(System.getProperty("user.home")) / ".gnupg"
  }

  def fallbackFiles(fs: File*): File = {
    require(fs.nonEmpty)
    val (h, t) = (fs.head, fs.tail)
    if (t.isEmpty) h
    else if (h.exists) h
    else fallbackFiles(t: _*)
  }

  /* "The location of the key ring, passed to gpg command as --no-default-keyring --keyring <value>. */
  lazy val pgpKeyRing: Option[File] = None

  /* "The location of the secret key ring. Only needed if using Bouncy Castle. */
  lazy val pgpSecretRing: File =
    fallbackFiles(
      gnuPGHome / "secring.gpg",
      new File(System.getProperty("user.home")) / ".sbt" / "gpg" / "secring.asc"
    )

  /* "The location of the secret key ring. Only needed if using Bouncy Castle. */
  lazy val pgpPublicRing: File =
    fallbackFiles(
      gnuPGHome / "pubring.gpg",
      new File(System.getProperty("user.home")) / ".sbt" / "gpg" / "pubring.asc"
    )

  /* "The passphrase associated with the secret used to sign artifacts. */
  lazy val pgpPassphrase: Option[Array[Char]] = None

  /* "Context used for auto-completing PGP commands. */
  lazy val pgpStaticContext: cli.PgpStaticContext =
    SbtPgpStaticContext(pgpPublicRing, pgpSecretRing)

  /* "The path of the GPG command to run */
  lazy val gpgCommand: String =
    if (isWindows) "gpg.exe" else "gpg"

  /* "If this is set to true, the GPG command line will be used. */
  lazy val useGpg: Boolean =
    sys.props.get("SBT_PGP_USE_GPG") match {
      case Some(_) => java.lang.Boolean.getBoolean("SBT_PGP_USE_GPG")
      case None    => true
    }

  /* "If this is set to true, the GPG command line will expect a GPG agent for the password. */
  lazy val useGpgAgent: Boolean = true

  /* "If this is set to true, the GPG command line will expect pinentry will be used with gpg-agent. */
  lazy val useGpgPinentry: Boolean = false

  /* The helper class to run gpg commands. */
  def pgpSigner(): PgpSigner =
    if (useGpg)
      if (useGpgPinentry)
        new CommandLineGpgPinentrySigner(gpgCommand, useGpgAgent, pgpKeyRing, pgpSigningKey(), pgpSelectPassphrase())
      else
        new CommandLineGpgSigner(gpgCommand, useGpgAgent, pgpKeyRing, pgpSigningKey(), pgpSelectPassphrase())
    else new BouncyCastlePgpSigner(pgpCmdContext(), pgpSigningKey())

  /* The passphrase associated with the secret used to sign artifacts. */
  def pgpSelectPassphrase(): Option[Array[Char]] =
    pgpPassphrase
      .orElse(maybeCredentials.map(_.passwd.toCharArray))
      .orElse(scala.util.Properties.envOrNone("PGP_PASSPHRASE").map(_.toCharArray))

  /* The key used to sign artifacts in this project, passed to gpg command as --default-key <value>. */
  def pgpSigningKey(): Option[String] =
    maybeCredentials.map(_.userName)

  /* Context used to run PGP commands. */
  def pgpCmdContext(): cli.PgpCommandContext =
    SbtPgpCommandContext(
      pgpStaticContext,
      interactionService,
      pgpSelectPassphrase(),
      logger
    )

  /* Packages all artifacts for publishing and maps the Artifact definition to the generated file. */
  def signedArtifacts(files: Map[RelPath, Array[Byte]]): Map[RelPath, Array[Byte]] = {
    val signer = pgpSigner()
    files.flatMap { case in @ (relPath, content) =>
      val signedArtifact = relPath.withLast(_ + gpgExtension)
      val signedContent = signer.sign(content, logger)
      Map(in, (signedArtifact, signedContent))
    }
  }
}
