package dev.mopuri.qsactions.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun QsActionsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // minSdk is 33, so dynamic colour is always available.
    val colorScheme = runCatching {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }.getOrElse {
        if (darkTheme) darkColorScheme() else lightColorScheme()
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
