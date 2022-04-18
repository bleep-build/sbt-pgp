package com.jsuereth.pgp
package cli

import nosbt.internal.util.complete.Parser
import nosbt.internal.util.complete.DefaultParsers._

case class ListKeys() extends PgpCommand {
  def run(ctx: PgpCommandContext): Unit = {
    import Display._
    ctx output {
      printFileHeader(ctx.publicKeyRingFile) +
        (ctx.publicKeyRing.keyRings map printRing mkString "\n")
    }
  }
  override def isReadOnly = true
}
object ListKeys {
  def parser: Parser[ListKeys] =
    token("list-keys") map { _ =>
      ListKeys()
    }
}
