package app.elevon

import app.elevon.relay.RelayCrypto
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RelayCryptoTest {

    @Test
    fun `public keys round trip through raw encoding`() {
        val pair = RelayCrypto.generateKeyPair()
        val raw = RelayCrypto.encodePublicKey(pair.public as java.security.interfaces.ECPublicKey)
        assertEquals(65, raw.size)
        assertEquals(0x04, raw[0].toInt())
        val restored = RelayCrypto.decodePublicKey(raw)
        assertArrayEquals(raw, RelayCrypto.encodePublicKey(restored))
    }

    @Test
    fun `both sides derive the same session key`() {
        val phone = RelayCrypto.generateKeyPair()
        val laptop = RelayCrypto.generateKeyPair()
        val phonePub = RelayCrypto.encodePublicKey(phone.public as java.security.interfaces.ECPublicKey)
        val laptopPub = RelayCrypto.encodePublicKey(laptop.public as java.security.interfaces.ECPublicKey)

        val phoneKey = RelayCrypto.sessionKey(
            RelayCrypto.sharedSecret(phone.private, RelayCrypto.decodePublicKey(laptopPub)),
        )
        val laptopKey = RelayCrypto.sessionKey(
            RelayCrypto.sharedSecret(laptop.private, RelayCrypto.decodePublicKey(phonePub)),
        )
        assertArrayEquals(phoneKey.encoded, laptopKey.encoded)
    }

    @Test
    fun `comparison code is symmetric and five digits`() {
        val a = ByteArray(65) { it.toByte() }
        val b = ByteArray(65) { (it * 3).toByte() }
        val code1 = RelayCrypto.comparisonCode(a, b)
        val code2 = RelayCrypto.comparisonCode(b, a)
        assertEquals(code1, code2)
        assertEquals(5, code1.length)
        assertTrue(code1.all { it.isDigit() })
    }

    @Test
    fun `different pairings produce different codes`() {
        val a = ByteArray(65) { it.toByte() }
        val b = ByteArray(65) { (it * 3).toByte() }
        val c = ByteArray(65) { (it * 7).toByte() }
        assertNotEquals(RelayCrypto.comparisonCode(a, b), RelayCrypto.comparisonCode(a, c))
    }

    @Test
    fun `encrypt and decrypt round trip`() {
        val key = SecretKeySpec(ByteArray(32) { 7 }, "AES")
        val message = "hello relay, this is typed text ✓".toByteArray(Charsets.UTF_8)
        val sealed = RelayCrypto.encrypt(key, message)
        assertArrayEquals(message, RelayCrypto.decrypt(key, sealed))
    }

    @Test
    fun `tampered ciphertext fails to decrypt`() {
        val key = SecretKeySpec(ByteArray(32) { 7 }, "AES")
        val sealed = RelayCrypto.encrypt(key, "attack at dawn".toByteArray())
        sealed[sealed.size - 1] = (sealed[sealed.size - 1].toInt() xor 0x01).toByte()
        assertNull(RelayCrypto.decrypt(key, sealed))
    }

    @Test
    fun `wrong key fails to decrypt`() {
        val keyA = SecretKeySpec(ByteArray(32) { 1 }, "AES")
        val keyB = SecretKeySpec(ByteArray(32) { 2 }, "AES")
        val sealed = RelayCrypto.encrypt(keyA, "secret".toByteArray())
        assertNull(RelayCrypto.decrypt(keyB, sealed))
    }

    @Test
    fun `base64url round trip`() {
        val bytes = ByteArray(65) { (it * 31).toByte() }
        val text = RelayCrypto.toBase64Url(bytes)
        assertArrayEquals(bytes, RelayCrypto.fromBase64Url(text))
    }
}
