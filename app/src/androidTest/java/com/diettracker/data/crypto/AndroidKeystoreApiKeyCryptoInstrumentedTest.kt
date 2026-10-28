package com.diettracker.data.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 仪器化测试：真实 Android Keystore 上验证 AES/GCM 加密回环与篡改检测。
 */
@RunWith(AndroidJUnit4::class)
class AndroidKeystoreApiKeyCryptoInstrumentedTest {

    private val crypto = AndroidKeystoreApiKeyCrypto()

    @Test
    fun 加密解密回环() {
        val plaintext = "sk-测试密钥-123"
        val encoded = crypto.encrypt(plaintext)

        assertTrue(!encoded.isNullOrBlank())
        assertNotEquals(plaintext, encoded)
        assertEquals(plaintext, crypto.decrypt(encoded!!))
    }

    @Test
    fun 密文不含明文且每次随机() {
        val encoded1 = crypto.encrypt("same-key")!!
        val encoded2 = crypto.encrypt("same-key")!!

        assertTrue("same-key" !in encoded1)
        assertNotEquals(encoded1, encoded2)
    }

    @Test
    fun 篡改密文解密失败() {
        val encoded = crypto.encrypt("sk-secret")!!
        val tampered = StringBuilder(encoded).also {
            it.setCharAt(it.length / 2, if (it[it.length / 2] == 'A') 'B' else 'A')
        }.toString()

        assertNull(crypto.decrypt(tampered))
    }

    @Test
    fun 空串加密返回空() {
        assertNull(crypto.encrypt(""))
    }
}
