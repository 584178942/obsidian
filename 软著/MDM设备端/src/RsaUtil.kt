package com.siyu.mdm.custom.device.util

import android.util.Base64
import com.blankj.utilcode.util.LogUtils
import com.siyu.mdm.custom.device.App.Companion.instance
import com.siyu.mdm.custom.device.R
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.*
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException

/**
 * @author Z T
 * @date 20200928
 */
object RsaUtil {
    private const val TAG = "RsaUtil"
    private val PUBLIC_KEY: String = instance.getString(R.string.pu_key)
    private val PRIVATE_KEY: String = instance.getString(R.string.pr_key)
    /**
     * RSA最大加密明文大小
     */
    private const val MAX_ENCRYPT_BLOCK = 117

    /**
     * RSA最大解密密文大小
     */
    private const val MAX_DECRYPT_BLOCK = 128

    /**
     * 加密算法RSA
     */
    private const val KEY_ALGORITHM = "RSA"

    private const val RSA_ECB_PKCS1PADDING = "RSA/ECB/PKCS1Padding"

    /**
     * 公钥加密
     * @param data
     * @return
     *
     */
    fun encryptByPublicKey(data: String): String? {
        var encryptedData: ByteArray? = null
        try {
            val dataByte = data.toByteArray()
            val keyBytes = Base64.decode(PUBLIC_KEY, Base64.NO_WRAP)
            val x509KeySpec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)
            val publicK: Key = keyFactory.generatePublic(x509KeySpec)
            // 对数据加密
            val cipher = Cipher.getInstance(RSA_ECB_PKCS1PADDING)
            cipher.init(Cipher.ENCRYPT_MODE, publicK)
            val inputLen = dataByte.size
            val out = ByteArrayOutputStream()
            var offSet = 0
            var i = 0
            // 对数据分段加密
            while (inputLen - offSet > 0) {
                val cache = if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
                    cipher.doFinal(dataByte, offSet, MAX_ENCRYPT_BLOCK)
                } else {
                    cipher.doFinal(dataByte, offSet, inputLen - offSet)
                }
                out.write(cache, 0, cache.size)
                i++
                offSet = i * MAX_ENCRYPT_BLOCK
            }
            encryptedData = out.toByteArray()
            out.close()
        } catch (e: NoSuchAlgorithmException) {
            LogUtils.i(TAG, "encryptByPublicKey: ${e.message}")
        } catch (e: InvalidKeySpecException) {
            LogUtils.i(TAG, "encryptByPublicKey: ${e.message}")
        } catch (e: NoSuchPaddingException) {
            LogUtils.i(TAG, "encryptByPublicKey: ${e.message}")
        } catch (e: InvalidKeyException) {
            LogUtils.i(TAG, "encryptByPublicKey: ${e.message}")
        } catch (e: BadPaddingException) {
            LogUtils.i(TAG, "BadPaddingException: ${e.message}")
        } catch (e: IllegalBlockSizeException) {
            LogUtils.i(TAG, "IllegalBlockSizeException: ${e.message}")
        } catch (e: IOException) {
            LogUtils.i(TAG, "IOException: ${e.message}")
        }
        return encryptedData?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
    }

    /**
     * 公钥解密
     * @param data 传入字符串
     * @return
     * @throws Exception
     */
    fun decryptByPublicKey(data: String): String {
        LogUtils.i(TAG, data)
        var publicStr = ""
        val encryptedData = Base64.decode(data, Base64.DEFAULT)
        val keyBytes = Base64.decode(PUBLIC_KEY, Base64.DEFAULT)
        val pkcs8KeySpec = X509EncodedKeySpec(keyBytes)
        try {
            val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)
            val privateK: Key = keyFactory.generatePublic(pkcs8KeySpec)
            val cipher = Cipher.getInstance(RSA_ECB_PKCS1PADDING)
            cipher.init(Cipher.DECRYPT_MODE, privateK)
            val inputLen = encryptedData.size
            val out = ByteArrayOutputStream()
            var offSet = 0
            var i = 0
            // 对数据分段解密
            while (inputLen - offSet > 0) {
                val cache = if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
                    cipher.doFinal(encryptedData, offSet, MAX_DECRYPT_BLOCK)
                } else {
                    cipher.doFinal(encryptedData, offSet, inputLen - offSet)
                }
                out.write(cache, 0, cache.size)
                i++
                offSet = i * MAX_DECRYPT_BLOCK
            }
            val decryptedData = out.toByteArray()
            out.close()
            publicStr = String(decryptedData)
        } catch (e: NoSuchAlgorithmException) {
            LogUtils.i(TAG, "decryptByPublicKey: ${e.message}")
        } catch (e: InvalidKeySpecException) {
            LogUtils.i(TAG, "decryptByPublicKey: ${e.message}")
        } catch (e: NoSuchPaddingException) {
            LogUtils.i(TAG, "decryptByPublicKey: ${e.message}")
        } catch (e: InvalidKeyException) {
            LogUtils.i(TAG, "decryptByPublicKey: ${e.message}")
        } catch (e: IOException) {
            LogUtils.i(TAG, "decryptByPublicKey: ${e.message}")
        } catch (e: BadPaddingException) {
            e.printStackTrace()
        } catch (e: IllegalBlockSizeException) {
            e.printStackTrace()
        }
        return publicStr
    }

    /**
     * 私钥解密
     * @param data 传入字符串
     * @return
     * @throws Exception
     */
    fun decryptByPrivateKey(data: String): String {
        LogUtils.i(TAG, data)
        var publicStr = ""
        val encryptedData = Base64.decode(data, Base64.NO_WRAP)
        val keyBytes = Base64.decode(PRIVATE_KEY, Base64.NO_WRAP)
        try {
            val pkcs8KeySpec = PKCS8EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)
            val privateK: Key = keyFactory.generatePrivate(pkcs8KeySpec)
            val cipher = Cipher.getInstance(RSA_ECB_PKCS1PADDING)
            cipher.init(Cipher.DECRYPT_MODE, privateK)
            val inputLen = encryptedData.size
            val out = ByteArrayOutputStream()
            var offSet = 0
            var i = 0
            // 对数据分段解密
            while (inputLen - offSet > 0) {
                val cache = if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
                    cipher.doFinal(encryptedData, offSet, MAX_DECRYPT_BLOCK)
                } else {
                    cipher.doFinal(encryptedData, offSet, inputLen - offSet)
                }
                out.write(cache, 0, cache.size)
                i++
                offSet = i * MAX_DECRYPT_BLOCK
            }
            val decryptedData = out.toByteArray()
            out.close()
            publicStr = out.toString()
        } catch (e: NoSuchAlgorithmException) {
            LogUtils.i(TAG, "decryptByPublicKey: ${e.message}")
        } catch (e: InvalidKeySpecException) {
            LogUtils.i(TAG, "decryptByPublicKey: ${e.message}")
        } catch (e: NoSuchPaddingException) {
            LogUtils.i(TAG, "decryptByPublicKey: ${e.message}")
        } catch (e: InvalidKeyException) {
            LogUtils.i(TAG, "decryptByPublicKey: ${e.message}")
        } catch (e: IOException) {
            LogUtils.i(TAG, "decryptByPublicKey: ${e.message}")
        } catch (e: BadPaddingException) {
            e.printStackTrace()
        } catch (e: IllegalBlockSizeException) {
            e.printStackTrace()
        }
        return publicStr
    }

    /**
     * 私钥加密
     *
     * @param data    待加密数据
     * @param pr 密钥
     * @return byte[] 加密数据
     */
    fun encryptByPrivateKey(data: String, pr: String): String {
        var encryptByStr = ""
        try {
            // 得到私钥
            val encryptedData = Base64.decode(data, Base64.NO_WRAP)
            val privateKey = Base64.decode(pr, Base64.NO_WRAP)
            val keySpec = PKCS8EncodedKeySpec(privateKey)
            val kf = KeyFactory.getInstance(KEY_ALGORITHM)
            val keyPrivate: PrivateKey = kf.generatePrivate(keySpec)
            // 数据加密
            val cipher = Cipher.getInstance(RSA_ECB_PKCS1PADDING)
            cipher.init(Cipher.ENCRYPT_MODE, keyPrivate)
            encryptByStr = Base64.encodeToString(cipher.doFinal(encryptedData), Base64.NO_WRAP)
        } catch (e: Exception) {
            LogUtils.i(TAG, e.localizedMessage)
        }
        return encryptByStr
    }

    /**
     * 公钥解密
     *
     * @param data   待解密数据
     * @param pu 密钥
     * @return byte[] 解密数据
     */
    @Throws(Exception::class)
    fun decryptByPublicKey(data: String, pu: String): String {
        // 得到公钥
        val encryptedData = Base64.decode(data, Base64.NO_WRAP)
        val publicKey = Base64.decode(pu, Base64.NO_WRAP)
        val keySpec = X509EncodedKeySpec(publicKey)
        val kf = KeyFactory.getInstance(KEY_ALGORITHM)
        val keyPublic: PublicKey = kf.generatePublic(keySpec)
        // 数据解密
        val cipher = Cipher.getInstance(RSA_ECB_PKCS1PADDING)
        cipher.init(Cipher.DECRYPT_MODE, keyPublic)
        return Base64.encodeToString(cipher.doFinal(encryptedData), Base64.NO_WRAP)
    }

    @Throws(Exception::class)
    fun decryptByPriKey(data: String): ByteArray {
        val encryptedData = Base64.decode(data, Base64.NO_WRAP)
        val prikey = Base64.decode("MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAJFBnk78A4CN5gBpIV/pGOGqi/CzvwjvXoj2gYXtbEIg+ZxpRrVi7Is6dwIK4+xrDr35ExaN1s4GnyF3g88z93iYpM5URhQTRJ/GGENNlozkLNARRdTJfLuJxBMZHnAGOtuNTXIcIo5/k8klllBYHTqG6xIVnjRN0vsV2UGlnW7VAgMBAAECgYBMoT9xD8aRNUrXgJ7YyFIWCzEUZN8tSYqn2tPt4ZkxMdA9UdS5sFx1/vv1meUwPjJiylnlliJyQlAFCdYBo7qzmib8+3Q8EU3MDP9bNlpxxC1go57/q/TbaymWyOk3pK2VXaX+8vQmllgRZMQRi2JFBHVoep1f1x7lSsf2TpipgQJBANJlO+UDmync9X/1YdrVaDOi4o7g3w9u1eVq9B01+WklAP3bvxIoBRI97HlDPKHx+CZXeODx1xj0xPOK3HUz5FECQQCwvdagPPtWHhHx0boPF/s4ZrTUIH04afuePUuwKTQQRijnl0eb2idBe0z2VAH1utPps/p4SpuT3HI3PJJ8MlVFAkAFypuXdj3zLQ3k89A5wd4Ybcdmv3HkbtyccBFALJgs+MPKOR5NVaSuF95GiD9HBe4awBWnu4B8Q2CYg54F6+PBAkBKNgvukGyARnQGc6eKOumTTxzSjSnHDElIsjgbqdFgm/UE+TJqMHmXNyyjqbaA9YeRc67R35HfzgpvQxHG8GN5AkEAxSKOlfACUCQ/CZJovETMmaUDas463hbrUznp71uRMk8RP7DY/lBnGGMeUeeZLIVK5X2Ngcp9nJQSKWCGtpnfLQ==", Base64.NO_WRAP)
        val pkcs8KeySpec = PKCS8EncodedKeySpec(prikey)
        val keyFactory = KeyFactory.getInstance("RSA/ECB/PKCS1Padding")
        val privateKey: PrivateKey = keyFactory.generatePrivate(pkcs8KeySpec)
        val cipher = Cipher.getInstance(keyFactory.algorithm)
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return cipher.doFinal(data.toByteArray())
    }

    /**
     * 验签
     *
     * @param srcData 原始字符串
     * @param sign 签名
     * @return 是否验签通过
     */
    @Throws(Exception::class)
    fun verify(srcData: String, sign: String): Boolean {
        return try {
            val keyBytes = Base64.decode(PUBLIC_KEY, Base64.NO_WRAP)
            val x509KeySpec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)
            val key: PublicKey = keyFactory.generatePublic(x509KeySpec)
            val signature = Signature.getInstance("SHA256WITHRSA")
            signature.initVerify(key)
            signature.update(srcData.toByteArray())
            signature.verify(Base64.decode(sign.toByteArray(), Base64.NO_PADDING))
        } catch (e: Exception) {
            LogUtils.i(TAG, e.localizedMessage)
            false
        }

    }
}