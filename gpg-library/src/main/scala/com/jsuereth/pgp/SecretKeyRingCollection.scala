package com.jsuereth.pgp

import org.bouncycastle.bcpg._
import org.bouncycastle.openpgp._
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator

import java.io._
import scala.collection.JavaConverters._

/** A secret PGP key ring. Can be used to decrypt messages and to sign files/messages. */
class SecretKeyRingCollection(val nested: PGPSecretKeyRingCollection) extends StreamingSaveable {

  /** A collection of all the nested secret key rings. */
  val keyRings: List[SecretKeyRing] =
    nested.getKeyRings.asScala.map(SecretKeyRing.apply).toList

  /** A collection of all the secret keys from all the key rings. */
  def secretKeys: List[SecretKey] = keyRings flatMap (_.secretKeys)

  /** The default secret key ring to use. */
  def default: SecretKeyRing = keyRings.head

  /** Finds the first secret key ring that has a public key that:
    *   - A keyID containing the given hex code
    *   - A userID containing the given string
    */
  def findSecretKeyRing(value: String): Option[SecretKeyRing] =
    keyRings.find(ring => ring.secretKeys.exists(key => PGP.isPublicKeyMatching(value)(key.publicKey)))

  /** Finds the first secret key that has:
    *   - A keyID containing the given hex code
    *   - A userID containing the given string
    */
  def findSecretKey(value: String): Option[SecretKey] =
    secretKeys find { key =>
      PGP.isPublicKeyMatching(value)(key.publicKey)
    }

  def saveTo(output: OutputStream): Unit = {
    val armoredOut = new ArmoredOutputStream(output)
    nested.encode(armoredOut)
    armoredOut.close()
  }

  override def toString = "SecretKeyRingCollection(" + secretKeys.mkString(",") + ")"
}

object SecretKeyRingCollection extends StreamingLoadable[SecretKeyRingCollection] {
  def apply(nested: PGPSecretKeyRingCollection) = new SecretKeyRingCollection(nested)
  def load(input: InputStream) =
    apply(new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(input), new BcKeyFingerprintCalculator))
}
