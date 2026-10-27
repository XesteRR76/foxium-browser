package com.foxium.browser.sync

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {

    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789abcdef"
        val result = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val i = b.toInt() and 0xFF
            result.append(hexChars[i shr 4])
            result.append(hexChars[i and 0x0F])
        }
        return result.toString()
    }

    fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    fun quickStretch(password: String, email: String): ByteArray {
        val salt = "identity.mozilla.com/picl/v1/quickStretch:$email".toByteArray(StandardCharsets.UTF_8)
        val spec = PBEKeySpec(password.toCharArray(), salt, 1000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    fun deriveAuthPW(stretched: ByteArray): String {
        val info = "identity.mozilla.com/picl/v1/authPW".toByteArray(StandardCharsets.UTF_8)
        val derived = hmacSha256(stretched, info)
        return bytesToHex(derived)
    }

    fun deriveUnwrapBKey(stretched: ByteArray): ByteArray {
        val info = "identity.mozilla.com/picl/v1/unwrapBKey".toByteArray(StandardCharsets.UTF_8)
        return hmacSha256(stretched, info)
    }

    fun hkdf(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
        val prk = if (salt.isEmpty()) {
            hmacSha256(ByteArray(32), ikm)
        } else {
            hmacSha256(salt, ikm)
        }

        val result = ByteArray(length)
        var t = ByteArray(0)
        var offset = 0
        var counter = 1

        while (offset < length) {
            val input = ByteArray(t.size + info.size + 1)
            System.arraycopy(t, 0, input, 0, t.size)
            System.arraycopy(info, 0, input, t.size, info.size)
            input[input.size - 1] = counter.toByte()

            t = hmacSha256(prk, input)
            val todo = minOf(t.size, length - offset)
            System.arraycopy(t, 0, result, offset, todo)
            offset += todo
            counter++
        }

        return result
    }

    fun decryptAes256Cbc(ciphertext: ByteArray, iv: ByteArray, key: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return cipher.doFinal(ciphertext)
    }

    fun generateHawkHeader(
        id: String,
        key: ByteArray,
        method: String,
        path: String,
        host: String,
        port: Int = 443
    ): String {
        val ts = (System.currentTimeMillis() / 1000).toString()
        val random = SecureRandom()
        val nonceBytes = ByteArray(6)
        random.nextBytes(nonceBytes)
        val nonce = bytesToHex(nonceBytes)

        val normalized = "hawk.1.header\n$ts\n$nonce\n$method\n$path\n$host\n$port\n\n\n"
        val mac = Base64.encodeToString(
            hmacSha256(key, normalized.toByteArray(StandardCharsets.UTF_8)),
            Base64.NO_WRAP
        )

        return """Hawk id="$id", ts="$ts", nonce="$nonce", mac="$mac""""
    }
}
