package com.jsuereth.pgp
package cli

import java.io.File

case class EncryptFile(file: File, pubKey: String) extends PgpCommand {
  def run(ctx: PgpCommandContext): Unit = {
    val key = (for {
      keyring <- ctx.publicKeyRing findPubKeyRing pubKey
      encKey <- keyring.encryptionKeys.headOption
    } yield encKey) getOrElse sys.error("Could not find encryption key for: " + pubKey)
    key.encryptFile(file, new File(file.getAbsolutePath + ".asc"))
  }
  override def isReadOnly: Boolean = true
}
