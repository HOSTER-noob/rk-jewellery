package com.rkjewellery.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = TextOnInk,
    secondary = Gold,
    onSecondary = Color.White,
    tertiary = Success,
    background = Background,
    onBackground = Ink,
    surface = SurfaceWhite,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEFECE7),
    onSurfaceVariant = InkVariant,
    outline = OutlineSoft,
    error = Danger,
    onError = Color.White
)

private val RKShapes = Shapes(
    extraSmall = 8.dp,
    small = 14.dp,
    medium = 20.dp,
    large = 26.dp,
    extraLarge = 32.dp
)

private val RKTypography = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp),
    headlineSmall = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    bodySmall = TextStyle(fontSize = 11.sp, lineHeight = 15.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun RKJewelleryTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    // Light theme is the identity of the app (reference design); force light chrome.
    val window = (context as? Activity)?.window
    if (window != null) WindowCompat.getDecorView(window).isAppearanceLightStatusBars = true

    MaterialTheme(
        colorScheme = LightColors,
        typography = RKTypography,
        shapes = RKShapes,
        content = content
    )
}
