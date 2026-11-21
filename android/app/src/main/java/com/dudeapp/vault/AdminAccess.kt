package com.dudeapp.vault

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import kotlinx.coroutines.launch

/**
 * The owner's view of the names on the server: who is registered, and what each name may use.
 *
 * This is the only screen that reads the usernames table, and it can only do so with the
 * passphrase, whose SHA-256 the database compares. Nothing is granted by default beyond the
 * two free looks: a name starts at "No access" and the owner decides.
 */
@Composable
internal fun AdminAccessCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var passphrase by remember { mutableStateOf("") }
    var unlocked by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    var names by remember { mutableStateOf<List<Supabase.RemoteName>>(emptyList()) }
    var saving by remember { mutableStateOf<String?>(null) }

    fun load() {
        busy = true
        problem = null
        scope.launch {
            val list = Supabase.adminNames(context, passphrase)
            busy = false
            if (list == null) {
                problem = "The passphrase was not accepted, or the server could not be reached."
                unlocked = false
            } else {
                names = list
                unlocked = true
            }
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "Names on the server",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Only the passphrase opens this list. It is not stored in the app and not " +
                "kept on this phone — type it each time you look.",
            color = TextSoft,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = passphrase,
            onValueChange = {
                passphrase = it
                problem = null
            },
            label = { Text("Owner passphrase") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            trailingIcon = {
                if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { load() },
                enabled = passphrase.isNotBlank() && !busy,
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(if (unlocked) "Reload" else "Unlock")
            }
            if (unlocked) {
                IconButton(onClick = { load() }, enabled = !busy) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Reload the list")
                }
            }
        }

        val note = problem
        if (note != null) {
            Spacer(Modifier.height(8.dp))
            Text(note, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }

        if (unlocked) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (names.isEmpty()) {
                    "No names have been claimed yet."
                } else {
                    "${names.size} name${if (names.size == 1) "" else "s"} claimed"
                },
                color = TextSoft,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(8.dp))
            names.forEach { name ->
                NameAccessRow(
                    name = name,
                    saving = saving == name.username,
                    onSave = { level, features ->
                        saving = name.username
                        problem = null
                        scope.launch {
                            val ok = Supabase.setAccess(
                                context = context,
                                passphrase = passphrase,
                                username = name.username,
                                level = level,
                                features = features,
                            )
                            saving = null
                            if (ok) {
                                names = names.map {
                                    if (it.username == name.username) {
                                        it.copy(level = level, features = features)
                                    } else {
                                        it
                                    }
                                }
                            } else {
                                problem = "Could not save @${name.username}. Check the connection."
                            }
                        }
                    },
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

/** One claimed name: its level, its switches, and when it was taken. */
@Composable
private fun NameAccessRow(
    name: Supabase.RemoteName,
    saving: Boolean,
    onSave: (String, List<String>) -> Unit,
) {
    var level by remember(name.username, name.level) { mutableStateOf(name.level) }
    var features by remember(name.username, name.features) { mutableStateOf(name.features.toSet()) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("@${name.username}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (name.claimedAt.length >= 10) {
                            "claimed " + name.claimedAt.take(10)
                        } else {
                            "claimed"
                        },
                        color = TextSoft,
                        fontSize = 11.sp,
                    )
                }
                if (saving) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            }
            Spacer(Modifier.height(10.dp))

            // The levels, in the same order the app understands them.
            val levels = listOf(
                "NONE" to "No access",
                "THEME" to "Theme",
                "ADVANCED" to "Advanced",
                "EVERYTHING" to "Everything",
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                levels.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        pair.forEach { (key, label) ->
                            val active = level == key
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (active) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        level = key
                                        onSave(key, features.toList())
                                    },
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (active) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            AccessFeature.entries.forEach { feature ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(feature.label, fontSize = 13.sp)
                        Text(feature.detail, color = TextSoft, fontSize = 11.sp)
                    }
                    Switch(
                        checked = feature.key in features,
                        onCheckedChange = { on ->
                            features = if (on) features + feature.key else features - feature.key
                            onSave(level, features.toList())
                        },
                    )
                }
            }
        }
    }
}
