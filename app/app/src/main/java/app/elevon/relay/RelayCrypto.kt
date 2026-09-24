package app.elevon.relay

import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Elevon Relay session crypto (v1):
 *
 *  1. Both sides make an ECDH P-256 key pair and exchange raw public keys
 *     (65 bytes: 0x04 || X || Y).
 *  2. Session key = HKDF-SHA256(ECDH shared secret, salt="elevon-relay-v1",
 *     info="session"), 32 bytes.
 *  3. Comparison code = first 5 decimal digits of SHA-256(sorted public keys
 *     concatenated). Both sides show it; the user compares. This is what makes
 *     a man-in-the-middle detectable — it is the same idea as comparing a
 *     Bluetooth pairing number.
 *  4. Payloads are AES-256-GCM with a fresh 12-byte nonce per frame.
 *
 * Pure JVM code, unit-tested.
 */
object RelayCrypto {

    private const val HKDF_SALT = "elevon-relay-v1"
    private const val HKDF_INFO = "session"

    fun generateKeyPair(): KeyPair =
        KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"))
        }.generateKeyPair()

    /** 65-byte uncompressed point encoding (0x04 || X || Y). */
    fun encodePublicKey(key: ECPublicKey): ByteArray {
        val w = key.w
        val x = leftPad(w.affineX.toByteArray(), 32)
        val y = leftPad(w.affineY.toByteArray(), 32)
        val out = ByteArray(65)
        out[0] = 0x04
        x.copyInto(out, 1, 32 - x.size)
        y.copyInto(out, 33, 32 - y.size)
        return out
    }

    fun decodePublicKey(raw: ByteArray): ECPublicKey {
        require(raw.size == 65 && raw[0] == 0x04.toByte()) { "expected uncompressed EC point" }
        val x = raw.copyOfRange(1, 33)
        val y = raw.copyOfRange(33, 65)
        val point = ECPoint(java.math.BigInteger(1, x), java.math.BigInteger(1, y))
        val spec = ECPublicKeySpec(point, curveParams())
        return KeyFactory.getInstance("EC").generatePublic(spec) as ECPublicKey
    }

    private fun curveParams(): java.security.spec.ECParameterSpec {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"))
        val pub = kpg.generateKeyPair().public as ECPublicKey
        return pub.params
    }

    fun sharedSecret(privateKey: java.security.PrivateKey, otherPublic: ECPublicKey): ByteArray =
        KeyAgreement.getInstance("ECDH").run {
            init(privateKey)
            doPhase(otherPublic, true)
            generateSecret()
        }

    fun sessionKey(sharedSecret: ByteArray): SecretKey {
        val prk = hmacSha256(HKDF_SALT.toByteArray(), sharedSecret)
        val okm = hkdfExpand(prk, HKDF_INFO.toByteArray(), 32)
        return SecretKeySpec(okm, "AES")
    }

    /** The 5-digit code both sides display. */
    fun comparisonCode(pubA: ByteArray, pubB: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val (first, second) = if (pubA.size > pubB.size || (pubA.size == pubB.size && compareBytes(pubA, pubB) > 0)) {
            pubB to pubA
        } else {
            pubA to pubB
        }
        md.update(first); md.update(second)
        val digest = md.digest()
        var value = 0L
        for (i in 0..7) value = (value shl 8) or (digest[i].toLong() and 0xFF)
        return "%05d".format(value % 100000)
    }

    fun encrypt(key: SecretKey, plaintext: ByteArray): ByteArray {
        val nonce = ByteArray(12)
        java.security.SecureRandom().nextBytes(nonce)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, nonce))
        val ct = cipher.doFinal(plaintext)
        return nonce + ct
    }

    fun decrypt(key: SecretKey, nonceAndCiphertext: ByteArray): ByteArray? = runCatching {
        require(nonceAndCiphertext.size > 12)
        val nonce = nonceAndCiphertext.copyOfRange(0, 12)
        val ct = nonceAndCiphertext.copyOfRange(12, nonceAndCiphertext.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, nonce))
        cipher.doFinal(ct)
    }.getOrNull()

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun hkdfExpand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        val out = ByteArray(length)
        var t = ByteArray(0)
        var offset = 0
        var counter = 1
        while (offset < length) {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(prk, "HmacSHA256"))
            mac.update(t)
            mac.update(info)
            mac.update(counter.toByte())
            t = mac.doFinal()
            val n = minOf(t.size, length - offset)
            t.copyInto(out, offset, 0, n)
            offset += n
            counter++
        }
        return out
    }

    private fun leftPad(src: ByteArray, size: Int): ByteArray {
        // BigInteger.toByteArray() prepends a sign byte for positives whose
        // high bit is set (33 bytes for a 256-bit coordinate) - drop it.
        val s = if (src.size == size + 1 && src[0] == 0.toByte()) src.copyOfRange(1, src.size) else src
        require(s.size <= size)
        val out = ByteArray(size)
        s.copyInto(out, size - s.size)
        return out
    }

    private fun compareBytes(a: ByteArray, b: ByteArray): Int {
        for (i in 0 until minOf(a.size, b.size)) {
            val d = (a[i].toInt() and 0xFF) - (b[i].toInt() and 0xFF)
            if (d != 0) return d
        }
        return a.size - b.size
    }

    fun toBase64Url(bytes: ByteArray): String =
        java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    fun fromBase64Url(text: String): ByteArray =
        java.util.Base64.getUrlDecoder().decode(text)
}
