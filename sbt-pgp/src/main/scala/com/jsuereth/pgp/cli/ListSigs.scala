package com.jsuereth.pgp
package cli

/** Lists Signatures on a file. */
case class ListSigs() extends PgpCommand {
  def run(ctx: PgpCommandContext): Unit = {
    import Display._
    ctx.log.info("Looking for sigs")
    ctx output {
      printFileHeader(ctx.publicKeyRingFile) +
        (ctx.publicKeyRing.keyRings map printRingWithSignatures mkString "\n")
    }
  }
  override def isReadOnly = true
}
