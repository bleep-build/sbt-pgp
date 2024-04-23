package bleep.plugin.pgp

import bleep.logging.Logger
import bleep.plugin.pgp.cli.PgpCommandContext

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File}

/** The interface used to sign plugins. */
trait PgpSigner {

  /** Signs a given file and writes the output to the signature file specified. Returns the signature file, throws on errors.
    */
  def sign(file: Array[Byte], logger: Logger): Array[Byte]
}

object PgpSigner {
  // This is used to synchronize signing to work around
  // https://github.com/sbt/sbt-pgp/issues/168
  private[pgp] val lock = new Object
}

/** A GpgSigner that uses the command-line to run gpg. */
class CommandLineGpgSigner(
    command: String,
    agent: Boolean,
    optRing: Option[File],
    optKey: Option[String],
    optPassphrase: Option[Array[Char]]
) extends PgpSigner {
  lazy val gpgVersion: String = {
    val lines = sys.process.Process(command, List("--version")).!!.linesIterator.toList
    lines.headOption match {
      case Some(head) => head.split(" ").last
      case _          => "0.0.0"
    }
  }
  private val TaggedVersion = """(\d{1,14})([\.\d{1,14}]*)((?:-\w+)*)((?:\+.+)*)""".r
  lazy val gpgVersionNumber: (Long, Long) = gpgVersion match {
    case TaggedVersion(m, ns, ts @ _, es @ _) =>
      // null safe, empty string safe
      def splitOn[A](s: String, sep: Char): Vector[String] =
        if (s eq null) Vector()
        else s.split(sep).filterNot(_ == "").toVector
      def splitDot(s: String) = splitOn(s, '.') map (_.toLong)
      (m.toLong, splitDot(ns).headOption.getOrElse(0L))
    case _ => (0L, 0L)
  }
  def isLegacyGpg: Boolean = gpgVersionNumber._1 < 2L
  def sign(content: Array[Byte], logger: Logger): Array[Byte] = PgpSigner.lock.synchronized {
    val passargs: Seq[String] = (optPassphrase map { passArray =>
      passArray mkString ""
    } map { pass =>
      // https://github.com/sbt/sbt-pgp/issues/173
      // https://www.gnupg.org/documentation/manuals/gnupg/GPG-Esoteric-Options.html#GPG-Esoteric-Options
      // --passphrase
      // Since Version 2.1 the --pinentry-mode also needs to be set to loopback.
      if (isLegacyGpg) Seq("--batch", "--passphrase", pass)
      else Seq("--batch", "--pinentry-mode", "loopback", "--passphrase", pass)
    }) getOrElse Seq.empty
    val ringargs: Seq[String] =
      optRing match {
        case Some(ring) => Seq("--no-default-keyring", "--keyring", ring.getPath)
        case _          => Vector.empty
      }
    val keyargs: Seq[String] = optKey map (k => Seq("--default-key", k)) getOrElse Seq.empty
    val args = passargs ++ ringargs ++ Seq("--detach-sign", "--armor") ++ (if (agent) Seq("--use-agent") else Seq.empty) ++ keyargs
    val allArguments: Seq[String] = args ++ Seq("--output", "-")
    val ostream = new ByteArrayOutputStream(1024)
    val istream = new ByteArrayInputStream(content)
    sys.process.Process(command, allArguments) #< istream #> ostream ! logger.processLogger("signer") match {
      case 0 => ()
      case n => sys.error(s"Failure running '${command + " " + allArguments.mkString(" ")}'.  Exit code: " + n)
    }
    ostream.toByteArray
  }

  override val toString: String = "GPG-Command(" + command + ")"
}

/** A GpgSigner that uses the command-line to run gpg with a GPG smartcard.
  *
  * Yubikey 4 has OpenPGP support: https://developers.yubico.com/PGP/ so we can call it directly, and the secret key resides on the card. This means we need
  * pinentry to be used, and there is no secret key ring.
  */
class CommandLineGpgPinentrySigner(
    command: String,
    agent: Boolean,
    optRing: Option[File],
    optKey: Option[String],
    optPassphrase: Option[Array[Char]]
) extends PgpSigner {
  def sign(content: Array[Byte], logger: Logger): Array[Byte] = PgpSigner.lock.synchronized {
    // (the PIN code is the passphrase)
    // https://wiki.archlinux.org/index.php/GnuPG#Unattended_passphrase
    val pinentryargs: Seq[String] = Seq("--pinentry-mode", "loopback")
    val passargs: Seq[String] = (optPassphrase map { passArray =>
      passArray mkString ""
    } map { pass =>
      Seq("--batch", "--passphrase", pass)
    }) getOrElse Seq.empty
    val ringargs: Seq[String] =
      optRing match {
        case Some(ring) => Seq("--no-default-keyring", "--keyring", ring.getPath)
        case _          => Vector.empty
      }
    val keyargs: Seq[String] = optKey map (k => Seq("--default-key", k)) getOrElse Seq.empty
    val args = passargs ++ ringargs ++ pinentryargs ++ Seq("--detach-sign", "--armor") ++ (if (agent) Seq("--use-agent")
                                                                                           else Seq.empty) ++ keyargs
    val allArguments: Seq[String] = args ++ Seq("--output", "-")
    val ostream = new ByteArrayOutputStream(1024)
    val istream = new ByteArrayInputStream(content)
    sys.process.Process(command, allArguments) #< istream #> ostream ! logger.processLogger("signer") match {
      case 0 => ()
      case n => sys.error(s"Failure running '${command + " " + allArguments.mkString(" ")}'.  Exit code: " + n)
    }
    ostream.toByteArray
  }

  override val toString: String = "GPG-Agent-Command(" + command + ")"
}

/** A GpgSigner that uses bouncy castle. */
class BouncyCastlePgpSigner(ctx: PgpCommandContext, optKey: Option[String]) extends PgpSigner {
  val keyId = optKey match {
    case Some(x) => new java.math.BigInteger(x, 16).longValue
    case _       => ctx.secretKeyRing.secretKey.keyID
  }

  def sign(content: Array[Byte], logger: Logger): Array[Byte] =
    ctx.withPassphrase(keyId) { pw =>
      ctx.secretKeyRing(keyId).sign(content, pw)
    }
  override lazy val toString: String = "BC-PGP(" + ctx.secretKeyRing + ")"
}
