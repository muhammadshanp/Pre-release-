package com.dudeapp.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * The one screen that talks to Supabase: claiming a display name.
 *
 * This is the only data pre re keeps in a database. Vault contents stay on the phone, so
 * the card says so, and the functions behind the button are the closed ones on the server
 * side — a name, the install holding it, nothing else.
 */
@Composable
fun PreReNameCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("pre_re_supabase", android.content.Context.MODE_PRIVATE) }
    val colors = preReColors()

    var claimed by remember { mutableStateOf(prefs.getString("claimed_name", null)) }
    var typing by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var good by remember { mutableStateOf(false) }

    fun claim() {
        val wanted = typing.trim()
        if (wanted.isEmpty()) {
            good = false
            message = "Type a name first."
            return
        }
        busy = true
        message = null
        scope.launch {
            when (val check = Supabase.checkName(context, wanted)) {
                is Supabase.NameCheck.Rejected -> {
                    good = false
                    message = Supabase.claimProblem(check.reason)
                }
                Supabase.NameCheck.Taken -> {
                    good = false
                    message = "That name is already taken. Try another."
                }
                Supabase.NameCheck.Offline -> {
                    good = false
                    message = Supabase.claimProblem(null)
                }
                Supabase.NameCheck.Free -> {
                    val status = Supabase.claimName(context, wanted)
                    if (status == "ok" || status == "already_yours") {
                        val stored = normalizeUsername(wanted)
                        prefs.edit().putString("claimed_name", stored).apply()
                        claimed = stored
                        typing = ""
                        good = true
                        message = "@$stored is yours."
                    } else {
                        good = false
                        message = Supabase.claimProblem(status)
                    }
                }
            }
            busy = false
        }
    }

    fun release() {
        val held = claimed ?: return
        busy = true
        message = null
        scope.launch {
            val done = Supabase.releaseName(context, held)
            if (done) {
                prefs.edit().remove("claimed_name").apply()
                claimed = null
                good = true
                message = "@$held is free again."
            } else {
                good = false
                message = "Could not release the name. Check your connection and try again."
            }
            busy = false
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.page)
            .border(1.dp, colors.divider, RoundedCornerShape(18.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = "Your name",
            color = colors.ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "One name, claimed once. Your photos, notes and chats stay on this phone.",
            color = colors.ink.copy(alpha = 0.6f),
            fontSize = 14.sp,
            lineHeight = 21.sp,
        )
        Spacer(Modifier.height(16.dp))

        val held = claimed
        if (held != null) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "@$held",
                    color = colors.ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(12.dp))
                PreReOutlineButton(
                    label = "Release",
                    onClick = { release() },
                    modifier = Modifier.width(120.dp),
                    enabled = !busy,
                )
            }
        } else {
            PreReField(
                value = typing,
                onValueChange = {
                    typing = it
                    message = null
                },
                placeholder = "yourname",
                enabled = !busy,
            )
            Spacer(Modifier.height(16.dp))
            PreRePrimaryButton(
                label = "Claim name",
                onClick = { claim() },
                busy = busy,
                busyLabel = "Checking…",
                enabled = !busy,
            )
        }

        val note = message
        if (note != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = note,
                color = if (good) colors.ink else androidx.compose.ui.graphics.Color(0xFFE05252),
                fontSize = 13.sp,
            )
        }
    }
}
