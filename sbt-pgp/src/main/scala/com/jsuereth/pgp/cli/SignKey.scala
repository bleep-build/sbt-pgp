package com.jsuereth.pgp
package cli

import com.jsuereth.pgp.cli.CommonParsers._
import nosbt.internal.util.complete.DefaultParsers._
import nosbt.internal.util.complete.Parser

case class SignKey(pubKey: String, notation: (String, String)) extends PgpCommand {
  def run(ctx: PgpCommandContext): Unit = {
    val matches = for {
      ring <- ctx.publicKeyRing.keyRings.toSeq
      key <- ring.publicKeys
      if PGP.isPublicKeyMatching(pubKey)(key)
    } yield ring -> key
    val newpubringcol = matches match {
      case Seq((ring, key), _*) =>
        val signingKey = ctx.secretKeyRing.secretKey
        val newkey = ctx.withPassphrase(signingKey.keyID) { pw =>
          ctx.log.info("Signing key: " + key)
          try
            signingKey.signPublicKey(key, notation, pw)
          catch {
            case t: Throwable =>
              ctx.log.error("Error signing key!", t)
              throw t
          }
        }
        val newpubring = ring :+ newkey
        (ctx.publicKeyRing removeRing ring) :+ newpubring
      case Seq()   => sys.error("Could not find key: " + pubKey)
      case matches => sys.error("Found more than on public key: " + matches.map(_._2).mkString(","))
    }
    newpubringcol saveToFile ctx.publicKeyRingFile
  }
}
object SignKey {
  def parser(ctx: PgpStaticContext): Parser[PgpCommand] =
    ((token("sign-key") ~ Space) ~> existingKeyIdOrUserOption(ctx) ~ (Space ~> attribute)) map { case key ~ attr =>
      SignKey(key, attr)
    }
}
