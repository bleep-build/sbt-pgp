package bleep.plugin.pgp
package cli

import bleep.logging.Logger
import bleep.plugin.pgp.{PGP, PublicKey, PublicKeyRing, PublicKeyRingCollection, SecretKeyRing}

import java.io.File

/** A context for accepting user input. */
trait UICommandContext {

  /** Displays the message to the user and accepts their input. */
  def readInput(msg: String): String

  /** Displays the message to the user and accepts their input. Replaces characters entered with '*'.
    */
  def readHidden(msg: String): String

  /** Prints the given text to whatever output we're using. */
  def output[A](msg: => A): Unit

  /** Logs information */
  def log: Logger
}

/** Context usable by command parsers. */
trait PgpStaticContext {
  def publicKeyRingFile: File
  def secretKeyRingFile: File
  // Derived methods
  def publicKeyRing: PublicKeyRingCollection = PGP loadPublicKeyRingCollection publicKeyRingFile
  def secretKeyRing: SecretKeyRing = PGP loadSecretKeyRing secretKeyRingFile
}

trait DelegatingPgpStaticContext extends PgpStaticContext {
  def ctx: PgpStaticContext
  override def publicKeyRing = ctx.publicKeyRing
  override def publicKeyRingFile = ctx.publicKeyRingFile
  override def secretKeyRing = ctx.secretKeyRing
  override def secretKeyRingFile = ctx.secretKeyRingFile
}

/** The context used when running PGP commands. */
trait PgpCommandContext extends PgpStaticContext with UICommandContext {

  /** Prompts user to input a passphrase. */
  def inputPassphrase: Array[Char]

  /** Perform an action with a passphrase.  This will ensure caching or other magikz */
  def withPassphrase[U](keyId: Long)(f: Array[Char] => U): U
  def addPublicKeyRing(key: PublicKeyRing): Unit =
    key.masterKey match {
      case Some(mk) if publicKeyRing.publicKeys.map(_.keyID).toSet.apply(mk.keyID) =>
        val badring = publicKeyRing.keyRings.find(ring => ring.publicKeys.exists(_.keyID == mk.keyID))
        val newring = badring.foldLeft(publicKeyRing) { (col, ring) =>
          col removeRing ring
        }
        val newring2 = newring :+ key
        newring2 saveToFile publicKeyRingFile
      case _ =>
        val newring = publicKeyRing :+ key
        newring saveToFile publicKeyRingFile
    }
  def addPublicKey(key: PublicKey): Unit =
    addPublicKeyRing(PublicKeyRing from key)
}
