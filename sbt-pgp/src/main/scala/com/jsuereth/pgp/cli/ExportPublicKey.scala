package com.jsuereth.pgp
package cli

case class ExportPublicKey(id: String) extends PgpCommand {
  def run(ctx: PgpCommandContext): Unit = {
    val key = ctx.publicKeyRing.findPubKeyRing(id) getOrElse
      sys.error("Could not find key: " + id)
    ctx.output(key.saveToString)
  }
  override def isReadOnly: Boolean = true
}

