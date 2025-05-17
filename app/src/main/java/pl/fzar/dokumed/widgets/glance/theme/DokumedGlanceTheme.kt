package pl.fzar.dokumed.widgets.glance.theme

import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders
import pl.fzar.dokumed.R // Assuming your R file is accessible for colors
import androidx.compose.ui.graphics.Color // Correct import for Color
import androidx.core.content.ContextCompat
import androidx.glance.LocalContext
import androidx.compose.material3.ColorScheme // Added import

/**
 * A basic Glance theme for Dokumed widgets.
 * This can be expanded to include more sophisticated theming options.
 */
@Composable
fun DokumedGlanceTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Define Light Color Scheme
    val lightColorScheme = ColorScheme(
        primary = Color(ContextCompat.getColor(context, R.color.purple_500)),
        onPrimary = Color.White,
        primaryContainer = Color(ContextCompat.getColor(context, R.color.purple_700)),
        onPrimaryContainer = Color.White,
        secondary = Color(ContextCompat.getColor(context, R.color.teal_200)),
        onSecondary = Color.Black,
        secondaryContainer = Color(ContextCompat.getColor(context, R.color.teal_700)),
        onSecondaryContainer = Color.Black,
        tertiary = Color(ContextCompat.getColor(context, R.color.purple_200)),
        onTertiary = Color.Black,
        tertiaryContainer = Color(ContextCompat.getColor(context, R.color.purple_700)),
        onTertiaryContainer = Color.White, // Corrected from Black based on typical contrast with purple_700
        error = Color(ContextCompat.getColor(context, R.color.design_default_color_error)),
        onError = Color.White,
        errorContainer = Color(ContextCompat.getColor(context, R.color.design_default_color_error)),
        onErrorContainer = Color.White, // Corrected from Black for better contrast
        background = Color(ContextCompat.getColor(context, R.color.widget_background_color)),
        onBackground = Color(ContextCompat.getColor(context, R.color.glance_text_primary)),
        surface = Color(ContextCompat.getColor(context, R.color.widget_background_color)),
        onSurface = Color(ContextCompat.getColor(context, R.color.glance_text_primary)),
        surfaceVariant = Color(ContextCompat.getColor(context, R.color.widget_background_color)), // Consider a slightly different color if needed
        onSurfaceVariant = Color(ContextCompat.getColor(context, R.color.glance_text_secondary)),
        outline = Color(ContextCompat.getColor(context, R.color.glance_text_secondary)),
        outlineVariant = Color(ContextCompat.getColor(context, R.color.glance_text_secondary)), // Added
        inversePrimary = Color.Transparent, // Add dummy values for required fields if not used
        inverseSurface = Color.Transparent,
        inverseOnSurface = Color.Transparent,
        surfaceTint = Color.Transparent,
        scrim = Color.Transparent
    )

    // Define Dark Color Scheme
    val darkColorScheme = ColorScheme(
        primary = Color(ContextCompat.getColor(context, R.color.purple_200)),
        onPrimary = Color.Black,
        primaryContainer = Color(ContextCompat.getColor(context, R.color.purple_500)),
        onPrimaryContainer = Color.Black, // Corrected from White for better contrast with purple_500
        secondary = Color(ContextCompat.getColor(context, R.color.teal_200)),
        onSecondary = Color.White, // Corrected from Black
        // Assuming R.color.teal_500 is missing, using R.color.teal_700 as a fallback for dark theme secondaryContainer
        secondaryContainer = Color(ContextCompat.getColor(context, R.color.teal_700)),
        onSecondaryContainer = Color.White, // Corrected from Black
        tertiary = Color(ContextCompat.getColor(context, R.color.purple_200)),
        onTertiary = Color.White, // Corrected from Black
        tertiaryContainer = Color(ContextCompat.getColor(context, R.color.purple_500)),
        onTertiaryContainer = Color.Black, // Corrected from White
        error = Color(ContextCompat.getColor(context, R.color.design_default_color_error)),
        onError = Color.Black, // Corrected from White
        errorContainer = Color(ContextCompat.getColor(context, R.color.design_default_color_error)),
        onErrorContainer = Color.Black, // Corrected from White
        // Fallback for R.color.widget_background_color_dark
        background = Color(0xFF121212),
        // Fallback for R.color.glance_text_primary_dark
        onBackground = Color.White,
        // Fallback for R.color.widget_background_color_dark
        surface = Color(0xFF121212),
        // Fallback for R.color.glance_text_primary_dark
        onSurface = Color.White,
        // Fallback for R.color.widget_background_color_dark (using a slightly different shade for variant)
        surfaceVariant = Color(0xFF1E1E1E),
        // Fallback for R.color.glance_text_secondary_dark
        onSurfaceVariant = Color(0xFFB0B0B0), // Light Gray
        // Fallback for R.color.glance_text_secondary_dark (for outline)
        outline = Color(0xFF606060), // Medium Gray
        outlineVariant = Color(0xFF606060), // Added - Using the same as outline for dark theme as a fallback
        inversePrimary = Color.Transparent, // Add dummy values for required fields if not used
        inverseSurface = Color.Transparent,
        inverseOnSurface = Color.Transparent,
        surfaceTint = Color.Transparent,
        scrim = Color.Transparent
    )

    GlanceTheme(
        colors = ColorProviders(
            light = lightColorScheme,
            dark = darkColorScheme
        )
    ) {
        content()
    }
}
