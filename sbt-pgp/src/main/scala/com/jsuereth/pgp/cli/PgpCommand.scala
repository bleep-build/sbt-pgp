package com.jsuereth.pgp
package cli

import nosbt.internal.util.complete.Parser

/** Represents a PgpCommand */
trait PgpCommand {
  def run(ctx: PgpCommandContext): Unit

  /** Returns true if the command will not modify the public/private keys. */
  def isReadOnly: Boolean = false
}
object PgpCommand {
  def parser(ctx: PgpStaticContext): Parser[PgpCommand] =
    GeneratePgpKey.parser |
      ListKeys.parser |
      ListSigs.parser |
      SendKey.parser(ctx) |
      ReceiveKey.parser |
      ImportKey.parser |
      EncryptMessage.parser(ctx) |
      EncryptFile.parser(ctx) |
      SignKey.parser(ctx) |
      ExportPublicKey.parser(ctx)
}
