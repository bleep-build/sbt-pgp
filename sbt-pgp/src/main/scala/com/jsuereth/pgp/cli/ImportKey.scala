package com.jsuereth.pgp
package cli

import com.jsuereth.pgp.cli.CommonParsers._
import nosbt.internal.util.complete.DefaultParsers._
import nosbt.internal.util.complete.Parser

import java.io.File

case class ImportKey(pubKey: File) extends PgpCommand {
  def run(ctx: PgpCommandContext): Unit = {
    val key = PGP loadPublicKeyRing pubKey
    ctx addPublicKeyRing key
  }
}
object ImportKey {
  def parser: Parser[ImportKey] =
    (token("import-pub-key") ~ Space) ~> filename map ImportKey.apply
}
