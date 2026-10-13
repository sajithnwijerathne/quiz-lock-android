package com.sajith.quizlock.ui.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sajith.quizlock.data.DebugLog
import com.sajith.quizlock.data.InstalledApps
import com.sajith.quizlock.data.LaunchableApp
import com.sajith.quizlock.data.LockStore
import com.sajith.quizlock.lock.MonitorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val store = remember { LockStore(context) }
    var apps by remember { mutableStateOf<List<LaunchableApp>?>(null) }
    var selected by remember { mutableStateOf(store.lockedPackages) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { InstalledApps.launchable(context) }
    }

    fun save() {
        store.lockedPackages = selected
        DebugLog.add(context, "app", "locked apps: ${selected.size}")
        if (store.lockingEnabled) MonitorService.start(context) // refresh notification text
        onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose apps (${selected.size})") },
                actions = { TextButton(onClick = { save() }) { Text("Save") } },
            )
        },
    ) { padding ->
        val list = apps
        if (list == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(Modifier.padding(padding)) {
                items(list, key = { it.packageName }) { app ->
                    val checked = app.packageName in selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = if (checked) selected - app.packageName else selected + app.packageName }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val icon = app.icon
                        if (icon != null) {
                            Image(icon.asImageBitmap(), contentDescription = null, modifier = Modifier.size(40.dp))
                        } else {
                            Spacer(Modifier.size(40.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(app.label, Modifier.weight(1f))
                        Checkbox(checked = checked, onCheckedChange = null)
                    }
                }
            }
        }
    }
}
