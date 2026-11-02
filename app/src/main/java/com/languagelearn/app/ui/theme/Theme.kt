package com.languagelearn.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Light = lightColorScheme(
    primary = Color(0xFF2F6FDB),
    secondary = Color(0xFF00897B),
    tertiary = Color(0xFFE0A100),
    error = Color(0xFFC62828),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF9BBCFF),
    secondary = Color(0xFF6CD3C4),
    tertiary = Color(0xFFFFC94D),
    error = Color(0xFFFF8A80),
)

/** Colours for feedback states; error is never shown by colour alone (text and underline too). */
object FeedbackColors {
    val correct = Color(0xFF2E7D32)
    val hint = Color(0xFFE0A100)
}

@Composable
fun LanguageLearnTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
