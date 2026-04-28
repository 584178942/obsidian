package com.siyu.mdm.custom.device.util

import android.content.Context
import com.blankj.utilcode.util.LogUtils
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

class OkHttpManager private constructor() {

    companion object {
        private const val DEFAULT_TIMEOUT = 30L
        val instance: OkHttpManager by lazy { OkHttpManager() }
    }

    private var okHttpClient: OkHttpClient = OkHttpClient()
    private val gson = Gson()

    // 初始化配置
    fun init(
        context: Context,
        config: OkHttpConfig.() -> Unit = {}
    ) {
        val okHttpConfig = OkHttpConfig(context).apply(config)
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(okHttpConfig.connectTimeout, TimeUnit.SECONDS)
            .readTimeout(okHttpConfig.readTimeout, TimeUnit.SECONDS)
            .writeTimeout(okHttpConfig.writeTimeout, TimeUnit.SECONDS)
            .apply {
                okHttpConfig.interceptors.forEach { addInterceptor(it) }
                okHttpConfig.networkInterceptors.forEach { addNetworkInterceptor(it) }
                if (okHttpConfig.enableCache) {
                    cache(Cache(File(context.cacheDir, "okhttp_cache"), okHttpConfig.cacheSize))
                }
            }
            .build()
    }

    // 构建请求（DSL风格）
    inline fun <reified T : Any> request(block: RequestBuilder.() -> Unit) {
        val requestBuilder = RequestBuilder().apply(block)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = executeRequest(requestBuilder)
                val parsedData = parseResponse<T>(response, requestBuilder.responseType)

                withContext(Dispatchers.Main) {
                    requestBuilder.onSuccess?.invoke(parsedData)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    requestBuilder.onError?.invoke(e)
                }
            }
        }
    }

    // 执行请求
    public suspend fun executeRequest(builder: RequestBuilder): Response {
        return withContext(Dispatchers.IO) {
            val request = buildRequest(builder)
            okHttpClient.newCall(request).execute()
        }
    }

    // 构建Request对象
    private fun buildRequest(builder: RequestBuilder): Request {
        return Request.Builder()
            .url(builder.url)
            .apply {
                when (builder.method) {
                    "GET" -> get()
                    "POST" -> post(builder.buildRequestBody())
                    "PUT" -> put(builder.buildRequestBody())
                    "DELETE" -> delete(builder.buildRequestBody())
                }
                headers(builder.buildHeaders())
            }
            .build()
    }


    // 解析响应
    public fun <T> parseResponse(response: Response, type: Class<*>): Any {
        if (!response.isSuccessful) throw IOException("HTTP error code: ${response.code}")
        return response.body?.string()?.let {
            gson.fromJson(it, type)
        } ?: throw IOException("Response body is null")
    }

    // 文件上传进度监听
    fun uploadFileWithProgress(
        url: String,
        file: File,
        onProgress: (percentage: Float) -> Unit
    ) {
        var requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", file.name,
                file.asRequestBody("multipart/form-data".toMediaType()).withProgress(onProgress)
            )
            .build()

        request<Any> {
            this.url = url
            method = "POST"
        }
    }

    // 配置类
    data class OkHttpConfig(
        val context: Context,
        var connectTimeout: Long = DEFAULT_TIMEOUT,
        var readTimeout: Long = DEFAULT_TIMEOUT,
        var writeTimeout: Long = DEFAULT_TIMEOUT,
        var enableCache: Boolean = false,
        var cacheSize: Long = 10 * 1024 * 1024, // 10MB
        val interceptors: MutableList<Interceptor> = mutableListOf(),
        val networkInterceptors: MutableList<Interceptor> = mutableListOf()
    )

    // 请求构建器
    inner class RequestBuilder {
        var url: String = ""
        var method: String = "GET"
        var headers: MutableMap<String, String> = mutableMapOf()
        var params: MutableMap<String, Any> = mutableMapOf()
        var requestBody: RequestBody? = null
        var responseType: Class<*> = Any::class.java
        var onSuccess: ((Any?) -> Unit)? = null
        var onError: ((Exception) -> Unit)? = null

        fun buildHeaders(): Headers {
            return Headers.Builder().apply {
                headers.forEach { (key, value) -> add(key, value) }
            }.build()
        }

        fun buildRequestBody(): RequestBody {
            return requestBody ?: params.toFormBody()
        }
        fun setEncryptedRequestBody(body: RequestBody) {
            this.requestBody = body
        }
        private fun Map<String, Any>.toFormBody(): RequestBody {
            return FormBody.Builder().apply {
                this@toFormBody.forEach { (key, value) -> add(key, value.toString()) }
            }.build()
        }
    }
}

// 文件上传进度扩展
private fun RequestBody.withProgress(
    onProgress: (Float) -> Unit
): RequestBody = object : RequestBody() {
    override fun contentType() = this@withProgress.contentType()

    override fun writeTo(sink: BufferedSink) {
        val countingSink = object : ForwardingSink(sink) {
            private var bytesWritten = 0L
            override fun write(source: Buffer, byteCount: Long) {
                super.write(source, byteCount)
                bytesWritten += byteCount
                onProgress(bytesWritten.toFloat() / contentLength())
            }
        }
        this@withProgress.writeTo(countingSink.buffer())
    }
}

// 日志拦截器
class LoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val t1 = System.nanoTime()
        LogUtils.i("Request ${request.url}")

        val response = chain.proceed(request)
        val t2 = System.nanoTime()
        LogUtils.i("Response: ${response.code} in ${(t2 - t1)/1e6}ms")

        return response
    }
}

// 公共请求头拦截器
class HeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("Device-ID", UUID.randomUUID().toString())
            .build()
        return chain.proceed(request)
    }
}