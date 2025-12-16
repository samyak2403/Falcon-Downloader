package com.samyak.falcondownloader

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.samyak.falcondownloader.ui.screens.MainScreen
import com.samyak.falcondownloader.ui.theme.FalconDownloaderTheme

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val sharedUrl = handleIntent(intent)
        
        setContent {
            var currentSharedUrl by remember { mutableStateOf(sharedUrl) }
            
            FalconDownloaderTheme {
                MainScreen(sharedUrl = currentSharedUrl)
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)?.let {
            recreate()
        }
    }
    
    private fun handleIntent(intent: Intent?): String? {
        return when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                        extractUrl(text)
                    }
                } else null
            }
            else -> null
        }
    }
    
    private fun extractUrl(text: String): String? {
        val urlPattern = Regex(
            """https?://[^\s<>"{}|\\^`\[\]]+""",
            RegexOption.IGNORE_CASE
        )
        return urlPattern.find(text)?.value ?: text.takeIf { 
            it.startsWith("http://") || it.startsWith("https://") 
        }
    }
}