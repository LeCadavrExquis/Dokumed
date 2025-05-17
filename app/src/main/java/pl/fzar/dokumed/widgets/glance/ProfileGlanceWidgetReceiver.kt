package pl.fzar.dokumed.widgets.glance

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pl.fzar.dokumed.widgets.glance.ProfileGlanceWidget.Companion.DATA_STATUS_KEY
import pl.fzar.dokumed.widgets.glance.ProfileGlanceWidget.Companion.STATUS_NEEDS_UPDATE
import androidx.glance.GlanceId // Added import

class ProfileGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ProfileGlanceWidget()

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun triggerUpdate(context: Context, glanceId: GlanceId) {
        coroutineScope.launch {
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { currentPrefs ->
                currentPrefs.toMutablePreferences().apply {
                    this[DATA_STATUS_KEY] = STATUS_NEEDS_UPDATE
                }
            }
            glanceAppWidget.update(context, glanceId) // Triggers recomposition
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        appWidgetIds.forEach { appWidgetId ->
            val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
            glanceId?.let { triggerUpdate(context, it) }
        }
    }

    // Optional: Consider onEnabled if you want to force an update immediately when the first widget is placed,
    // though the STATUS_NEEDS_UPDATE logic in ProfileGlanceWidget should handle initial load.
    // override fun onEnabled(context: Context) {
    //     super.onEnabled(context)
    //     // If you need to find all newly enabled glanceIds, it can be complex.
    //     // Relying on onUpdate and the widget's initial state is often simpler.
    // }
}
