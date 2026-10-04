package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

class ControllerSecurityManager {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "salim_controller_asymmetric_identity_v1"
        private const val SIGNATURE_ALGORITHM = "SHA256withRSA"

        @Volatile
        private var INSTANCE: ControllerSecurityManager? = null

        fun getInstance(): ControllerSecurityManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ControllerSecurityManager().also { INSTANCE = it }
            }
        }
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    init {
        ensureIdentityKeyPair()
    }

    private fun ensureIdentityKeyPair(): KeyPair {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA,
                ANDROID_KEYSTORE
            )
            val parameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .setKeySize(2048)
                .build()

            keyPairGenerator.initialize(parameterSpec)
            return keyPairGenerator.generateKeyPair()
        }

        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
        return KeyPair(entry.certificate.publicKey, entry.privateKey)
    }

    fun getPublicKey(): PublicKey {
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
        return entry.certificate.publicKey
    }

    private fun getPrivateKey(): PrivateKey {
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
        return entry.privateKey
    }

    fun getPublicKeyBase64(): String {
        return Base64.encodeToString(getPublicKey().encoded, Base64.NO_WRAP)
    }

    fun getControllerId(): String {
        val pubBytes = getPublicKey().encoded
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(pubBytes)
        val hex = hash.take(6).joinToString("") { "%02X".format(it) }
        return "SALIM-${hex.substring(0, 4)}-${hex.substring(4, 8)}-${hex.substring(8, 12)}"
    }

    fun getPublicKeyFingerprint(): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(getPublicKey().encoded)
        return hash.joinToString(":") { "%02X".format(it) }
    }

    fun signData(data: ByteArray): ByteArray {
        val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
        signature.initSign(getPrivateKey())
        signature.update(data)
        return signature.sign()
    }

    fun signDataToBase64(data: ByteArray): String {
        return Base64.encodeToString(signData(data), Base64.NO_WRAP)
    }

    fun verifyHostSignature(hostPublicKeyBase64: String, data: ByteArray, signatureBytes: ByteArray): Boolean {
        return try {
            val pubBytes = Base64.decode(hostPublicKeyBase64, Base64.NO_WRAP)
            val keyFactory = KeyFactory.getInstance("RSA")
            val pubKey = keyFactory.generatePublic(X509EncodedKeySpec(pubBytes))
            val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
            signature.initVerify(pubKey)
            signature.update(data)
            signature.verify(signatureBytes)
        } catch (e: Exception) {
            false
        }
    }
}
