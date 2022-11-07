package bleep.plugin.pgp
package cli

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
