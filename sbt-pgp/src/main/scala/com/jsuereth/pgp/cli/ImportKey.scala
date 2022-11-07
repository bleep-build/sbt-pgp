package bleep.plugin.pgp
package cli

import java.io.File

case class ImportKey(pubKey: File) extends PgpCommand {
  def run(ctx: PgpCommandContext): Unit = {
    val key = PGP loadPublicKeyRing pubKey
    ctx addPublicKeyRing key
  }
}
