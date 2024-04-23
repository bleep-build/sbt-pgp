package bleep.plugin.pgp
package cli

import scala.concurrent._
import scala.concurrent.duration._

/** Helper for running HKP protocol commands. */
trait HkpCommand extends PgpCommand {
  def hkpUrl: String
  def hkpClient = hkp.Client(hkpUrl)
}

case class SendKey(pubKey: String, hkpUrl: String) extends HkpCommand {
  def run(ctx: PgpCommandContext): Unit = {
    val key = ctx.publicKeyRing findPubKeyRing pubKey getOrElse sys.error("Could not find public key: " + pubKey)
    val client = hkpClient
    ctx.log.info("Sending " + key + " to " + client)
    client.pushKeyRing(
      key,
      (s: String) => ctx.log.debug(s)
    )
  }
  override def isReadOnly: Boolean = true
}

case class ReceiveKey(pubKeyId: Long, hkpUrl: String) extends HkpCommand {
  import scala.concurrent.ExecutionContext.Implicits._

  def run(ctx: PgpCommandContext): Unit = {
    val f = hkpClient
      .getKey(pubKeyId)
      .transform(
        identity,
        e => new RuntimeException("Could not find key: " + pubKeyId + " on server " + hkpUrl, e)
      )
    val key: PublicKeyRing = Await.result(f, Duration.Inf)
    ctx.log.info("Adding public key: " + key)
    // TODO - Remove if key already exists...
    ctx addPublicKeyRing key
  }
}
