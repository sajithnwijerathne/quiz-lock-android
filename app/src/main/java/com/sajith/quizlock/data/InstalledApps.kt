package com.sajith.quizlock.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap

data class LaunchableApp(val packageName: String, val label: String, val icon: Bitmap?)

/**
 * Apps that show in the launcher. Needs the MAIN/LAUNCHER `<queries>` entry in the manifest
 * (instead of the restricted QUERY_ALL_PACKAGES permission).
 */
object InstalledApps {

    fun launchable(context: Context): List<LaunchableApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName }
            .map { info ->
                LaunchableApp(
                    packageName = info.packageName,
                    label = pm.getApplicationLabel(info).toString(),
                    icon = runCatching { pm.getApplicationIcon(info).toBitmap(96, 96) }.getOrNull(),
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun label(context: Context, packageName: String): String =
        runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
}
