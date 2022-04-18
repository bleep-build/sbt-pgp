package com.jsuereth.pgp
package cli

import CommonParsers._
import nosbt.internal.util.complete.Parser
import nosbt.internal.util.complete.DefaultParsers._

case class EncryptMessage(msg: String, pubKey: String) extends PgpCommand {
  def run(ctx: PgpCommandContext): Unit = {
    val key = (for {
      keyring <- ctx.publicKeyRing findPubKeyRing pubKey
      encKey <- keyring.encryptionKeys.headOption
    } yield encKey) getOrElse sys.error("Could not find encryption key for: " + pubKey)
    ctx.output(key.encryptString(msg))
  }
}
object EncryptMessage {
  def parser(ctx: PgpStaticContext): Parser[PgpCommand] =
    // TODO - More robust/better parsing
    (token("encrypt-msg") ~ Space) ~> existingKeyIdOrUser(ctx) ~ (Space ~> message) map { case key ~ msg =>
      EncryptMessage(msg, key)
    }
}
