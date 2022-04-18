package com.jsuereth.pgp
package cli

import nosbt.internal.util.complete.Parser
import nosbt.internal.util.complete.DefaultParsers._

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
object ListSigs {
  def parser: Parser[PgpCommand] =
    token("list-sigs") map { _ =>
      ListSigs()
    }
}
