package com.sajith.quizlock.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.sajith.quizlock.data.DebugLog
import com.sajith.quizlock.data.LockStore
import com.sajith.quizlock.lock.MonitorService
import com.sajith.quizlock.lock.Permissions
import com.sajith.quizlock.ui.quiz.QuizActivity

private data class PermissionState(
    val usage: Boolean = false,
    val overlay: Boolean = false,
    val notifications: Boolean = false,
    val battery: Boolean = false,
)

private fun Context.readPermissions() = PermissionState(
    usage = Permissions.hasUsageAccess(this),
    overlay = Permissions.canDrawOverlays(this),
    notifications = Permissions.notificationsEnabled(this),
    battery = Permissions.ignoresBatteryOptimizations(this),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onChooseApps: () -> Unit, onOpenLog: () -> Unit) {
    val context = LocalContext.current
    val store = remember { LockStore(context) }

    var permissions by remember { mutableStateOf(context.readPermissions()) }
    var lockedCount by remember { mutableIntStateOf(store.lockedPackages.size) }
    var enabled by remember { mutableStateOf(store.lockingEnabled) }
    var grace by remember { mutableIntStateOf(store.graceMinutes) }

    // Re-check after the user comes back from a Settings screen.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissions = context.readPermissions()
        lockedCount = store.lockedPackages.size
        if (enabled && !Permissions.canLock(context)) {
            enabled = false
            store.lockingEnabled = false
            MonitorService.stop(context)
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissions = context.readPermissions()
    }

    fun open(intent: Intent) = context.startActivity(intent)

    Scaffold(topBar = { TopAppBar(title = { Text("QuizLock") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Section("Permissions") {
                PermissionRow("Usage access", "See which app is open", permissions.usage) {
                    open(Permissions.usageAccessIntent())
                }
                PermissionRow("Display over other apps", "Show the quiz on top", permissions.overlay) {
                    open(Permissions.overlayIntent(context))
                }
                PermissionRow("Notifications", "Status notification while locking", permissions.notifications) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        open(Permissions.appNotificationSettingsIntent(context))
                    }
                }
                PermissionRow("Unrestricted battery", "Stops the phone killing the lock", permissions.battery) {
                    open(Permissions.batteryIntent())
                }
            }

            Section("Locked apps") {
                Text(if (lockedCount == 0) "Nothing locked yet" else "$lockedCount apps locked")
                FilledTonalButton(onClick = onChooseApps) { Text("Choose apps to lock") }
            }

            Section("Locking") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Ask a question before locked apps open")
                        if (!Permissions.canLock(context)) {
                            Text(
                                "Needs usage access and display over other apps",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    Switch(
                        checked = enabled,
                        enabled = permissions.usage && permissions.overlay,
                        onCheckedChange = { on ->
                            enabled = on
                            store.lockingEnabled = on
                            DebugLog.add(context, "app", "locking ${if (on) "on" else "off"}")
                            if (on) MonitorService.start(context) else MonitorService.stop(context)
                        },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Open for $grace min after answering", Modifier.weight(1f))
                    OutlinedButton(onClick = { grace = (grace - 1).coerceAtLeast(1); store.graceMinutes = grace }) { Text("–") }
                    OutlinedButton(onClick = { grace = (grace + 1).coerceAtMost(30); store.graceMinutes = grace }) { Text("+") }
                }
            }

            Section("Debug") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { open(QuizActivity.intent(context, null)) }) { Text("Practice quiz") }
                    OutlinedButton(onClick = onOpenLog) { Text("Debug log") }
                }
                TextButton(onClick = {
                    store.clearUnlocks()
                    DebugLog.add(context, "app", "all unlocks cleared")
                }) { Text("Clear temporary unlocks") }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun PermissionRow(title: String, why: String, granted: Boolean, onGrant: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title)
            Text(why, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (granted) {
            Text("Allowed", color = MaterialTheme.colorScheme.primary)
        } else {
            TextButton(onClick = onGrant) { Text("Allow") }
        }
    }
}
