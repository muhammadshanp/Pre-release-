package com.dudeapp.vault

import android.content.Context
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * The Supabase half of pre re: the only data that leaves the phone is a display name.
 *
 * Vault contents — photos, PDFs, notes, reels, chat messages — never go anywhere near this
 * file. The table it talks to holds two columns, a name and the install that claimed it, and
 * is closed to direct reads and writes: everything goes through three SECURITY DEFINER
 * functions, so the publishable key cannot list other people's names.
 *
 *   pre_re_check(name)            -> {"available": bool, "reason": "ok"|"taken"|...}
 *   pre_re_claim(name, device)    -> {"status": "ok"|"taken"|"already_yours"|...}
 *   pre_re_release(name, device)  -> {"released": bool}
 *
 * The key below is a publishable key: it is meant to ship inside the app and is useless
 * without the row-level security it is subject to.
 */
internal object Supabase {
    const val PROJECT_URL = "https://bjhluvplbozefgmjzefw.supabase.co"

    /** Publishable (anon) key — public by design, protected by RLS. */
    private const val PUBLISHABLE_KEY = "sb_publishable_uW3Qc4Ykym7E1X6_MkV8_g_XdbxRxOy"

    /** Legacy anon key, sent as the bearer so the request lands in the `anon` role. */
    private const val ANON_JWT =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJqaGx1dnBs" +
            "Ym96ZWZnbWp6ZWZ3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTUxODc3NDIsImV4cCI6MjExMDc2Mzc0Mn0" +
            ".y9-YPrQj5agmnUvLaULJ1JilbMPqIl55FGkY_W-oyz0"

    private const val PREF = "pre_re_supabase"
    private const val PREF_DEVICE = "device_id"

    /** A random id for this install, so a name can belong to it without an account. */
    fun deviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val existing = prefs.getString(PREF_DEVICE, null)
        if (existing != null && existing.length >= 8) return existing
        val fresh = UUID.randomUUID().toString()
        prefs.edit().putString(PREF_DEVICE, fresh).apply()
        return fresh
    }

    /** Blocking POST to a PostgREST function. Returns null when offline or on any error. */
    private fun rpc(function: String, body: JSONObject): JSONObject? {
        val connection = runCatching {
            URL("$PROJECT_URL/rest/v1/rpc/$function").openConnection() as HttpURLConnection
        }.getOrNull() ?: return null

        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 6_000
            connection.readTimeout = 6_000
            connection.doOutput = true
            connection.setRequestProperty("apikey", PUBLISHABLE_KEY)
            connection.setRequestProperty("Authorization", "Bearer $ANON_JWT")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            if (connection.responseCode !in 200..299) return null
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            // PostgREST answers a scalar-returning function with a bare JSON value.
            if (text.trim().startsWith("{")) JSONObject(text) else JSONObject("{\"value\":$text}")
        } catch (e: IOException) {
            null
        } finally {
            connection.disconnect()
        }
    }

    // ---- what the screens call ------------------------------------------------------ //

    sealed interface NameCheck {
        data object Free : NameCheck
        data object Taken : NameCheck
        data class Rejected(val reason: String) : NameCheck
        data object Offline : NameCheck
    }

    suspend fun checkName(context: Context, name: String): NameCheck = withContext(Dispatchers.IO) {
        val json = rpc("pre_re_check", JSONObject().put("p_username", normalizeUsername(name)))
            ?: return@withContext NameCheck.Offline
        parseCheck(json)
    }

    /** Split out from the network call so the mapping itself can be tested. */
    internal fun parseCheck(json: JSONObject): NameCheck = when {
        json.optBoolean("available") -> NameCheck.Free
        json.optString("reason") == "taken" -> NameCheck.Taken
        else -> NameCheck.Rejected(json.optString("reason"))
    }

    suspend fun claimName(context: Context, name: String): String? = withContext(Dispatchers.IO) {
        val json = rpc(
            "pre_re_claim",
            JSONObject()
                .put("p_username", normalizeUsername(name))
                .put("p_device", deviceId(context)),
        ) ?: return@withContext null
        json.optString("status").takeIf { it.isNotEmpty() }
    }

    suspend fun releaseName(context: Context, name: String): Boolean = withContext(Dispatchers.IO) {
        val json = rpc(
            "pre_re_release",
            JSONObject()
                .put("p_username", normalizeUsername(name))
                .put("p_device", deviceId(context)),
        ) ?: return@withContext false
        json.optBoolean("released")
    }

    /** Turns the server's terse codes into something a person can act on. */
    fun claimProblem(status: String?): String = when (status) {
        null -> "Could not reach the name service. Check your connection and try again."
        "ok", "already_yours" -> "" // success
        "taken" -> "That name is already taken. Try another."
        "reserved" -> "That name is kept by pre re. Pick another."
        "short" -> "Use at least 3 characters."
        "long" -> "Keep it to 24 characters or fewer."
        "chars" -> "Letters, numbers, dots and underscores only, and it must start and end with a letter or number."
        "empty" -> "Type a name first."
        "bad_device" -> "This install could not be identified. Restart pre re and try again."
        else -> "That name could not be saved ($status)."
    }
}
