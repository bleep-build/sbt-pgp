package bleep.plugin.pgp
package cli

/** Represents a PgpCommand */
trait PgpCommand {
  def run(ctx: PgpCommandContext): Unit

  /** Returns true if the command will not modify the public/private keys. */
  def isReadOnly: Boolean = false
}

