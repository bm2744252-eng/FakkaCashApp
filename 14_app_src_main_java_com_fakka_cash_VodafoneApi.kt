package com.fakka.cash

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.FormBody
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

object VodafoneApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val deviceId = "b26ba335813fad21"

    private fun commonHeaders(): Map<String, String> = mapOf(
        "User-Agent" to "okhttp/4.12.0",
        "Accept-Encoding" to "gzip",
        "x-agent-operatingsystem" to "16",
        "clientId" to "AnaVodafoneAndroid",
        "Accept-Language" to "ar",
        "x-agent-device" to "Samsung SM-A165F",
        "x-agent-version" to "2025.11.1",
        "x-agent-build" to "1063",
        "digitalId" to "",
        "device-id" to deviceId
    )

    data class SeamlessResult(val seamlessToken: String, val msisdn: String)

    fun getSeamlessAndMsisdn(): SeamlessResult {
        val url = "http://mobile.vodafone.com.eg/checkSeamless/realms/vf-realm/protocol/openid-connect/auth?client_id=cash-app"
        val builder = Request.Builder().url(url).get()
            .addHeader("Connection", "Keep-Alive")
            .addHeader("If-Modified-Since", "Thu, 02 Apr 2026 09:09:07 GMT")
        commonHeaders().forEach { (k, v) -> builder.addHeader(k, v) }

        client.newCall(builder.build()).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("فشل seamlessToken: HTTP ${resp.code}")
            val body = resp.body?.string() ?: throw RuntimeException("استجابة فارغة")
            val json = JSONObject(body)
            val raw = json.optString("msisdn", "")
            val msisdn = if (raw.startsWith("1")) "0$raw" else raw
            val token = json.optString("seamlessToken", "")
            if (token.isEmpty()) throw RuntimeException("لم يتم استلام seamlessToken - تأكد أنك على شبكة فودافون")
            return SeamlessResult(token, msisdn)
        }
    }

    fun getAccessToken(seamlessToken: String): String {
        val url = "https://mobile.vodafone.com.eg/auth/realms/vf-realm/protocol/openid-connect/token"
        val form = FormBody.Builder()
            .add("grant_type", "password")
            .add("client_secret", "b86e30a8-ae29-467a-a71f-65c73f2ff5e3")
            .add("client_id", "cash-app")
            .build()

        val builder = Request.Builder().url(url).post(form)
            .addHeader("Accept", "application/json, text/plain, */*")
            .addHeader("silentLogin", "true")
            .addHeader("CRP", "false")
            .addHeader("seamlessToken", seamlessToken)
            .addHeader("firstTimeLogin", "true")
        commonHeaders().forEach { (k, v) -> builder.addHeader(k, v) }

        client.newCall(builder.build()).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("فشل access_token: HTTP ${resp.code}")
            val body = resp.body?.string() ?: throw RuntimeException("استجابة فارغة")
            return JSONObject(body).optString("access_token", "")
                .ifEmpty { throw RuntimeException("لم يتم استلام access_token") }
        }
    }

    data class OrderResult(val success: Boolean, val message: String, val raw: String)

    fun placeOrder(
        productId: String,
        sender: String,
        receiver: String,
        pin: String,
        accessToken: String
    ): OrderResult {
        val url = "https://mobile.vodafone.com.eg/services/dxl/pom/productOrder"

        val payload = JSONObject().apply {
            put("channel", JSONObject().put("name", "MobileApp"))
            put("orderItem", org.json.JSONArray().put(JSONObject().apply {
                put("action", "insert")
                put("id", productId)
                put("product", JSONObject().apply {
                    put("characteristic", org.json.JSONArray().apply {
                        put(JSONObject().put("name", "PaymentMethod").put("value", "VFCash"))
                        put(JSONObject().put("name", "USE_EMONEY").put("value", "False"))
                        put(JSONObject().put("name", "MerchantCode").put("value", "81841829"))
                    })
                    put("id", productId)
                    put("relatedParty", org.json.JSONArray().apply {
                        put(JSONObject().put("id", sender).put("name", "MSISDN").put("role", "Subscriber"))
                        put(JSONObject().put("id", receiver).put("name", "Receiver").put("role", "Receiver"))
                    })
                })
                put("@type", productId)
                put("eCode", 0)
            }))
            put("relatedParty", org.json.JSONArray().put(
                JSONObject().put("id", pin).put("name", "pin").put("role", "Requestor")
            ))
            put("@type", "CashFakkaAndMared")
        }

        val body = payload.toString().toRequestBody("application/json; charset=UTF-8".toMediaType())

        val builder = Request.Builder().url(url).post(body)
            .addHeader("Accept", "application/json")
            .addHeader("api-host", "ProductOrderingManagement")
            .addHeader("useCase", "CashFakkaAndMared")
            .addHeader("X-Request-ID", UUID.randomUUID().toString())
            .addHeader("api-version", "v2")
            .addHeader("msisdn", sender)
            .addHeader("Authorization", "Bearer $accessToken")
        commonHeaders().forEach { (k, v) -> builder.addHeader(k, v) }

        client.newCall(builder.build()).execute().use { resp ->
            val raw = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                return OrderResult(false, "فشل الاتصال - كود: ${resp.code}", raw)
            }
            return try {
                val json = JSONObject(raw)
                if (json.has("code") && json.optString("code") != "0000") {
                    OrderResult(false, json.optString("reason", "خطأ غير معروف"), raw)
                } else {
                    OrderResult(true, "تم إرسال الطلب بنجاح", raw)
                }
            } catch (e: Exception) {
                OrderResult(true, "تم الاستلام بنجاح", raw)
            }
        }
    }
}
