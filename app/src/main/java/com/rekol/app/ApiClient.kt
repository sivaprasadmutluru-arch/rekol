package com.rekol.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AuthResult(
    val token: String,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val message: String? = null
)
data class RemoteMemory(val id: String, val title: String, val content: String, val reason: String?, val createdAt: String, val privateMemory: Boolean)

object ApiClient {
    // Android emulator reaches a backend running on the development machine through 10.0.2.2.
    private val baseUrl = BuildConfig.REKOL_API_URL.trimEnd('/')

    suspend fun login(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        post("/api/v1/auth/login", JSONObject().put("email", email).put("password", password))
    }

    suspend fun register(name: String, email: String, phone: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        post("/api/v1/auth/register", JSONObject().put("name", name).put("email", email).put("phone", phone).put("password", password))
    }

    suspend fun verifyOtp(phone: String, code: String): AuthResult = withContext(Dispatchers.IO) {
        post("/api/v1/auth/otp/verify", JSONObject().put("phone", phone).put("code", code))
    }

    suspend fun me(token: String): AuthResult = withContext(Dispatchers.IO) {
        val connection = (URL(baseUrl + "/api/v1/auth/me").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 5000
            setRequestProperty("Authorization", "Bearer $token")
        }
        try {
            val text = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(text) }.getOrNull()
            if (connection.responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) throw IllegalStateException("AUTH_INVALID")
            if (connection.responseCode !in 200..299) throw IllegalStateException("AUTH_SERVER_UNAVAILABLE")
            AuthResult(token, json?.optString("name"), json?.optString("email"), json?.optString("phone"))
        } finally { connection.disconnect() }
    }

    suspend fun memories(token: String): List<RemoteMemory> = withContext(Dispatchers.IO) {
        val connection = authorized("/api/v1/memories", token, "GET")
        try {
            val text = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (connection.responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) throw IllegalStateException("AUTH_INVALID")
            if (connection.responseCode !in 200..299) throw IllegalStateException("MEMORY_SERVER_UNAVAILABLE")
            val array = org.json.JSONArray(text)
            List(array.length()) { i -> val x=array.getJSONObject(i); RemoteMemory(x.optString("id"), x.optString("title"), x.optString("content"), x.optString("reason").takeIf { it.isNotBlank() }, x.optString("created_at"), x.optBoolean("private_memory")) }
        } finally { connection.disconnect() }
    }

    suspend fun createMemory(token: String, content: String, reason: String, privateMemory: Boolean): RemoteMemory = withContext(Dispatchers.IO) {
        val connection = authorized("/api/v1/memories", token, "POST").apply { doOutput = true; setRequestProperty("Content-Type", "application/json") }
        try {
            val body=JSONObject().put("content",content).put("reason",reason).put("privateMemory",privateMemory)
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            val text=(if(connection.responseCode in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if(connection.responseCode !in 200..299) throw IllegalStateException("MEMORY_SAVE_FAILED")
            val x=JSONObject(text); RemoteMemory(x.optString("id"),x.optString("title"),x.optString("content"),x.optString("reason").takeIf { it.isNotBlank() },x.optString("created_at"),x.optBoolean("private_memory"))
        } finally { connection.disconnect() }
    }

    private fun authorized(path: String, token: String, method: String) = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply { requestMethod=method; connectTimeout=5000; readTimeout=5000; setRequestProperty("Authorization", "Bearer $token") }

    private fun post(path: String, body: JSONObject): AuthResult {
        val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 5000
            readTimeout = 5000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }
        return try {
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            val text = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(text) }.getOrNull()
            if (connection.responseCode !in 200..299) {
                if (connection.responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    throw IllegalStateException("INVALID_CREDENTIALS")
                }
                if (connection.responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    throw IllegalStateException("ACCOUNT_NOT_FOUND")
                }
                if (connection.responseCode == HttpURLConnection.HTTP_CONFLICT) {
                    throw IllegalStateException("ACCOUNT_ALREADY_EXISTS")
                }
                val serverMessage = listOf("detail", "message", "error")
                    .asSequence()
                    .mapNotNull { key -> json?.optString(key)?.takeIf { it.isNotBlank() && it != "null" } }
                    .firstOrNull()
                throw IllegalStateException(serverMessage ?: "Account service returned HTTP ${connection.responseCode}")
            }
            val user = json?.optJSONObject("user")
            AuthResult(
                token = json?.optString("access_token").orEmpty(),
                name = user?.optString("name")?.takeIf { !it.isNullOrBlank() },
                email = user?.optString("email")?.takeIf { !it.isNullOrBlank() },
                phone = user?.optString("phone")?.takeIf { !it.isNullOrBlank() }
            )
        } catch (error: java.net.UnknownHostException) {
            throw IllegalStateException("Cannot reach wefyx.pro. Check the emulator internet connection and retry.", error)
        } catch (error: java.net.SocketTimeoutException) {
            throw IllegalStateException("The account service timed out. Check your connection and retry.", error)
        } finally {
            connection.disconnect()
        }
    }
}
