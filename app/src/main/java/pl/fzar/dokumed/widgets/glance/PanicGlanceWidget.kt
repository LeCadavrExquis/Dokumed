package pl.fzar.dokumed.widgets.glance

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import pl.fzar.dokumed.R // Assuming R class is accessible
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.GlanceTheme
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.Button
import androidx.glance.ButtonColors
import androidx.glance.action.actionParametersOf
import pl.fzar.dokumed.widgets.glance.theme.DokumedGlanceTheme // Import the new theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Define a single, fixed key for the panic number in Glance preferences
val PANIC_NUMBER_KEY = stringPreferencesKey("panic_number_key")

class PanicGlanceWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val panicNumber = currentState(key = PANIC_NUMBER_KEY)

            DokumedGlanceTheme { // Use the centralized theme
                Box(
                    modifier = GlanceModifier.fillMaxSize().padding(8.dp).cornerRadius(16.dp), // Removed explicit background, theme provides it
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        text = context.getString(R.string.panic_button_widget_text),
                        onClick = actionRunCallback<PanicActionCallback>(
                            parameters = actionParametersOf(
                                ActionParameters.Key<String>("panic_number_param") to (panicNumber ?: "")
                            )
                        ),
                        // Optionally, add GlanceModifier.size() or other modifiers for the button
                    )
                }
            }
        }
    }
}

class PanicGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PanicGlanceWidget()

    override fun onUpdate(context: Context, appWidgetManager: android.appwidget.AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds) // Keep super call

        appWidgetIds.forEach { appWidgetId ->
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            // The key for the extra must match the one used in PanicButtonSettingsScreen.kt
            val phoneNumberFromSettings = options.getString("EXTRA_PHONE_NUMBER_FROM_SETTINGS")

            if (!phoneNumberFromSettings.isNullOrBlank()) {
                val pendingResult = goAsync() // Important for BroadcastReceiver async work
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // It's important to get the GlanceId associated with the appWidgetId
                        val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
                        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                            prefs.toMutablePreferences().apply {
                                this[PANIC_NUMBER_KEY] = phoneNumberFromSettings
                            }
                        }
                        // After updating the state, tell the widget to refresh its content
                        glanceAppWidget.update(context, glanceId)
                    } catch (e: Exception) {
                        // Log error or handle, e.g., if glanceId is not found
                        // Consider proper logging for production
                        e.printStackTrace() // For debugging purposes
                    } finally {
                        pendingResult.finish() // Ensure finish is called
                    }
                }
            }
        }
    }
}

class PanicActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // Retrieve the panic number using the fixed key from the parameters passed by the widget
        // Or, if not passed via parameters, it could be read from currentState again, but passing is cleaner.
        val phoneNumber = parameters[ActionParameters.Key<String>("panic_number_param")]

        if (phoneNumber.isNullOrBlank()) {
            // Attempt to read directly from Glance state as a fallback, though it should be in parameters
            val currentPrefs = androidx.glance.appwidget.state.getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
            val fallbackPhoneNumber = currentPrefs[PANIC_NUMBER_KEY]

            if (fallbackPhoneNumber.isNullOrBlank()) {
                Toast.makeText(context, context.getString(R.string.panic_widget_number_not_set), Toast.LENGTH_SHORT).show()
                return
            }
            // Use fallback number
            val intent = Intent(context, PanicHandlerActivity::class.java).apply {
                putExtra("PHONE_NUMBER", fallbackPhoneNumber)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
            return;
        }


        val intent = Intent(context, PanicHandlerActivity::class.java).apply {
            putExtra("PHONE_NUMBER", phoneNumber)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
}
