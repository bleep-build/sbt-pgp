package bleep.plugin.pgp
package cli

import bleep.nosbt.io.IO

/** Constructs a new PGP key from user input. */
case class GeneratePgpKey() extends PgpCommand {
  def run(ctx: PgpCommandContext): Unit = {
    if (ctx.publicKeyRingFile.exists) sys.error("Public key ring (" + ctx.publicKeyRingFile.getAbsolutePath + ") already exists!")
    if (ctx.secretKeyRingFile.exists) sys.error("Secret key ring (" + ctx.secretKeyRingFile.getAbsolutePath + ") already exists!")
    val pparent = ctx.publicKeyRingFile.getCanonicalFile.getParentFile
    val sparent = ctx.secretKeyRingFile.getCanonicalFile.getParentFile
    if (!pparent.exists) IO.createDirectory(pparent)
    if (!sparent.exists) IO.createDirectory(sparent)
    val name = ctx.readInput("Please enter the name associated with the key: ")
    val email = ctx.readInput("Please enter the email associated with the key: ")
    val pw = ctx.readHidden("Please enter the passphrase for the key: ")
    val pw2 = ctx.readHidden("Please re-enter the passphrase for the key: ")
    if (pw != pw2) sys.error("Passphrases do not match!")
    val id = "%s <%s>".format(name, email)
    ctx.log.info("Creating a new PGP key, this could take a long time.")
    PGP.makeKeys(id, pw.toCharArray, ctx.publicKeyRingFile, ctx.secretKeyRingFile)
    ctx.log.info("Public key := " + ctx.publicKeyRingFile.getAbsolutePath)
    ctx.log.info("Secret key := " + ctx.secretKeyRingFile.getAbsolutePath)
    ctx.log.info("Please do not share your secret key.   Your public key is free to share.")
  }
}
