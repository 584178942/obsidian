package com.siyu.mdm.custom.device.util

import com.blankj.utilcode.util.LogUtils
import com.google.gson.Gson
import com.siyu.mdm.custom.device.App
import com.siyu.mdm.custom.device.R
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

class NetUtils private constructor() {

    private val mOkHttpClient: OkHttpClient
    private val TAG = "NetUtils"
    // 线程池管理下载任务，避免过多线程创建
    private val downloadExecutor by lazy {
        val coreCount = Runtime.getRuntime().availableProcessors()
        ThreadPoolExecutor(
            coreCount,
            coreCount * 2,
            60L,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(),
            DownloadThreadFactory()
        )
    }

    // 线程工厂，用于命名下载线程
    private class DownloadThreadFactory : ThreadFactory {
        private val count = AtomicInteger(1)
        override fun newThread(r: Runnable): Thread {
            return Thread(r, "download-thread-${count.getAndIncrement()}")
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: NetUtils? = null
        fun getInstance(): NetUtils =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: NetUtils().also { INSTANCE = it }
            }
    }

    init {
        // 初始化基础URL（修正原代码中未使用的问题）
        val baseUrl = if (AppConstants.IS_TEST) {
            App.instance.getString(R.string.api_test_url) // 建议添加测试环境URL
        } else {
            App.instance.getString(R.string.api_url)
        }
        LogUtils.i(TAG, "初始化网络工具类，基础URL: $baseUrl")

        val clientBuilder = OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS) // 延长读取超时时间
            .connectTimeout(10, TimeUnit.SECONDS) // 延长连接超时时间
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true) // 添加连接失败重试
            .hostnameVerifier { hostname, session ->
                // 注意：绕过主机名验证存在安全风险，生产环境应谨慎使用
                if (AppConstants.IS_TEST) {
                    true
                } else {
                    // 生产环境验证主机名
                    session.peerHost == hostname
                }
            }
        mOkHttpClient = clientBuilder.build()
    }

    /**
     * 网络请求回调接口
     */
    interface NetCallback {
        fun onSuccess(response: Response)
        fun onFailure(e: Exception)
    }

    /**
     * 文件下载回调接口
     */
    interface DownloadCallback : NetCallback {
        fun onProgress(progress: Int) // 下载进度 0-100
    }

    /**
     * 发送POST请求
     */
    fun post(url: String, params: Map<String, Any?>, callback: NetCallback) {
        try {
            val requestBody = createRequestBody(params) ?: run {
                callback.onFailure(IllegalArgumentException("请求参数加密失败"))
                return
            }

            val fullUrl = buildFullUrl(url)
            LogUtils.i(TAG, "POST请求: $fullUrl, 参数: ${Gson().toJson(params)}")

            val request = Request.Builder()
                .url(fullUrl)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("User-Agent", "SiyuMDM-Client/${AppConstants.VERSION}")
                .post(requestBody)
                .build()

            mOkHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    LogUtils.e(TAG, "POST请求失败: ${e.message}", e)
                    callback.onFailure(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        callback.onSuccess(response)
                    } else {
                        val errorMsg = "POST请求返回非成功状态码: ${response.code}"
                        LogUtils.e(TAG, errorMsg)
                        callback.onFailure(IOException(errorMsg))
                    }
                }
            })
        } catch (e: Exception) {
            LogUtils.e(TAG, "POST请求异常", e)
            callback.onFailure(e)
        }
    }

    /**
     * 发送GET请求
     */
    fun get(url: String, params: Map<String, Any?>? = null, callback: NetCallback) {
        try {
            val fullUrl = buildFullUrl(url, params)
            LogUtils.i(TAG, "GET请求: $fullUrl")

            val request = Request.Builder()
                .url(fullUrl)
                .addHeader("User-Agent", "SiyuMDM-Client/${AppConstants.VERSION}")
                .get()
                .build()

            mOkHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    LogUtils.e(TAG, "GET请求失败: ${e.message}", e)
                    callback.onFailure(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        callback.onSuccess(response)
                    } else {
                        val errorMsg = "GET请求返回非成功状态码: ${response.code}"
                        LogUtils.e(TAG, errorMsg)
                        callback.onFailure(IOException(errorMsg))
                    }
                }
            })
        } catch (e: Exception) {
            LogUtils.e(TAG, "GET请求异常", e)
            callback.onFailure(e)
        }
    }

    /**
     * 下载单个文件
     */
    fun downloadFile(url: String, filePath: String, callback: DownloadCallback) {
        try {
            val fullUrl = buildFullUrl(url)
            LogUtils.i(TAG, "开始下载文件: $fullUrl 到 $filePath")

            val request = Request.Builder()
                .url(fullUrl)
                .addHeader("User-Agent", "SiyuMDM-Client/${AppConstants.VERSION}")
                .build()

            mOkHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    LogUtils.e(TAG, "文件下载失败: ${e.message}", e)
                    callback.onFailure(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        val errorMsg = "文件下载返回非成功状态码: ${response.code}"
                        LogUtils.e(TAG, errorMsg)
                        callback.onFailure(IOException(errorMsg))
                        return
                    }

                    // 在后台线程处理文件写入
                    downloadExecutor.execute {
                        var inputStream: InputStream? = null
                        var outputStream: FileOutputStream? = null
                        try {
                            val file = File(filePath)
                            // 创建父目录
                            file.parentFile?.let {
                                if (!it.exists()) {
                                    it.mkdirs()
                                }
                            }

                            inputStream = response.body?.byteStream()
                            outputStream = FileOutputStream(file)
                            val totalLength = response.body?.contentLength() ?: -1L
                            val buffer = ByteArray(8192)
                            var bytesRead = 0L

                            var bytes: Int
                            while (inputStream!!.read(buffer).also { bytes = it } != -1) {
                                // 检查是否取消下载
                                if (call.isCanceled()) {
                                    throw IOException("下载已取消")
                                }

                                outputStream.write(buffer, 0, bytes)
                                bytesRead += bytes

                                // 计算并回调进度
                                if (totalLength > 0) {
                                    val progress = (bytesRead * 100 / totalLength).toInt()
                                    callback.onProgress(progress)
                                }
                            }
                            outputStream.flush()
                            LogUtils.i(TAG, "文件下载完成: $filePath")
                            callback.onSuccess(response)
                        } catch (e: Exception) {
                            LogUtils.e(TAG, "文件写入失败: ${e.message}", e)
                            // 清理不完整文件
                            File(filePath).takeIf { it.exists() }?.delete()
                            callback.onFailure(e)
                        } finally {
                            // 确保资源释放
                            inputStream?.close()
                            outputStream?.close()
                            response.body?.close()
                        }
                    }
                }
            })
        } catch (e: Exception) {
            LogUtils.e(TAG, "下载请求异常", e)
            callback.onFailure(e)
        }
    }

    /**
     * 下载多个APK文件
     */
    fun downloadApks(
        apkUrls: List<String>,
        destinationDir: String,
        onAllComplete: () -> Unit,
        onSingleComplete: (File) -> Unit = {},
        onError: (String, Exception) -> Unit = { _, _ -> }
    ) {
        if (apkUrls.isEmpty()) {
            onAllComplete()
            return
        }

        val totalCount = apkUrls.size
        val completedCount = AtomicInteger(0)

        apkUrls.forEach { apkUrl ->
            downloadExecutor.execute {
                try {
                    val fileName = apkUrl.substringAfterLast('/').takeIf { it.isNotEmpty() }
                        ?: "download_${System.currentTimeMillis()}.apk"
                    // 确保文件名以.apk结尾
                    val finalFileName = if (fileName.endsWith(".apk", ignoreCase = true)) {
                        fileName
                    } else {
                        "$fileName.apk"
                    }
                    val file = File(destinationDir, finalFileName)

                    // 如果文件已存在，直接回调完成
                    if (file.exists() && file.length() > 0) {
                        LogUtils.i(TAG, "文件已存在，跳过下载: ${file.absolutePath}")
                        onSingleComplete(file)
                        if (completedCount.incrementAndGet() == totalCount) {
                            onAllComplete()
                        }
                        return@execute
                    }

                    val request = Request.Builder()
                        .url(buildFullUrl(apkUrl))
                        .build()

                    mOkHttpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            // 下载失败，清理可能已创建的空文件
                            if (file.exists()) {
                                file.delete()
                                LogUtils.i(TAG, "删除不完整文件: ${file.absolutePath}")
                            }
                            throw IOException("下载APK失败，状态码: ${response.code}")
                        }

                        response.body?.byteStream()?.use { inputStream ->
                            FileOutputStream(file).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        } ?: throw IOException("响应体为空")

                        LogUtils.i(TAG, "APK下载完成: ${file.absolutePath}")
                        onSingleComplete(file)
                    }
                } catch (e: Exception) {
                    LogUtils.e(TAG, "下载APK异常: ${apkUrl}", e)
                    onError(apkUrl, e)
                } finally {
                    if (completedCount.incrementAndGet() == totalCount) {
                        onAllComplete()
                    }
                }
            }
        }
    }

    /**
     * 创建请求体（处理加密可能为null的情况）
     */
    private fun createRequestBody(params: Map<String, Any?>): RequestBody? {
        return try {
            val json = Gson().toJson(params)
            val encryptedJson = RsaUtil.encryptByPublicKey(json)
                ?: throw NullPointerException("RSA加密结果为null")
            LogUtils.i("加密后的请求参数: $encryptedJson")
            "application/json; charset=utf-8".toMediaType().let { mediaType ->
                encryptedJson.toRequestBody(mediaType)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "创建请求体失败", e)
            null
        }
    }

    /**
     * 构建完整URL（处理基础URL拼接）
     */
    private fun buildFullUrl(url: String, params: Map<String, Any?>? = null): String {
        // 如果是完整URL则直接使用
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return if (params.isNullOrEmpty()) url else buildUrlWithParams(url, params)
        }

        // 否则拼接基础URL
        val baseUrl = if (AppConstants.IS_TEST) {
            App.instance.getString(R.string.api_test_url)
        } else {
            App.instance.getString(R.string.api_url)
        }.trimEnd('/')

        val fullUrl = "$baseUrl/$url".replace("//", "/") // 处理可能的双斜杠
        return if (params.isNullOrEmpty()) fullUrl else buildUrlWithParams(fullUrl, params)
    }

    /**
     * 为URL添加查询参数
     */
    private fun buildUrlWithParams(url: String, params: Map<String, Any?>): String {
        return try {
            val urlBuilder = url.toHttpUrl().newBuilder()
            params.forEach { (key, value) ->
                if (value != null) {
                    urlBuilder.addQueryParameter(key, value.toString())
                }
            }
            urlBuilder.build().toString()
        } catch (e: Exception) {
            LogUtils.e(TAG, "构建带参数的URL失败: $url", e)
            url // 失败时返回原始URL
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        downloadExecutor.shutdown()
        try {
            if (!downloadExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                downloadExecutor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            downloadExecutor.shutdownNow()
        }
        INSTANCE = null
    }
}
