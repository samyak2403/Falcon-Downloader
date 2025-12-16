package com.samyak.falcondownloader.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object AppIconManager {
    
    enum class AppIcon(val aliasName: String, val displayName: String) {
        DEFAULT("MainActivityDefault", "Default"),
        BLUE("MainActivityBlue", "Blue"),
        DARK("MainActivityDark", "Dark")
    }
    
    private const val PACKAGE_NAME = "com.samyak.falcondownloader"
    
    fun setIcon(context: Context, icon: AppIcon) {
        val pm = context.packageManager
        
        // Disable all icons first
        AppIcon.entries.forEach { appIcon ->
            pm.setComponentEnabledSetting(
                ComponentName(PACKAGE_NAME, "$PACKAGE_NAME.${appIcon.aliasName}"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
        
        // Enable selected icon
        pm.setComponentEnabledSetting(
            ComponentName(PACKAGE_NAME, "$PACKAGE_NAME.${icon.aliasName}"),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }
    
    fun getCurrentIcon(context: Context): AppIcon {
        val pm = context.packageManager
        
        return AppIcon.entries.firstOrNull { appIcon ->
            val state = pm.getComponentEnabledSetting(
                ComponentName(PACKAGE_NAME, "$PACKAGE_NAME.${appIcon.aliasName}")
            )
            state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
            (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && appIcon == AppIcon.DEFAULT)
        } ?: AppIcon.DEFAULT
    }
}
