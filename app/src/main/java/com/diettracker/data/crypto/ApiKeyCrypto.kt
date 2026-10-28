package com.diettracker.data.crypto

/**
 * API Key 的对称加解密抽象。真实实现基于 Android Keystore 的 AES/GCM。
 * 密文以 Base64 字符串落盘，任何场景不得输出明文。
 */
interface ApiKeyCrypto {

    /** 加密并返回 Base64(iv + ciphertext) 字符串；输入为空串时返回 null。 */
    fun encrypt(plaintext: String): String?

    /** 解密 Base64 密文；密钥缺失或密文被篡改时返回 null。 */
    fun decrypt(encoded: String): String?
}
