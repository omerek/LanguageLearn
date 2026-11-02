package com.languagelearn.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.languagelearn.app.ui.AppNavHost
import com.languagelearn.app.ui.theme.LanguageLearnTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as LanguageLearnApp).container
        setContent {
            LanguageLearnTheme {
                AppNavHost(container)
            }
        }
    }
}
