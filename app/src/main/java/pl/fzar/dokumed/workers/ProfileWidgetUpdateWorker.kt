package pl.fzar.dokumed.workers

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import pl.fzar.dokumed.widgets.glance.ProfileGlanceWidget
import pl.fzar.dokumed.widgets.glance.ProfileGlanceWidget.Companion.DATA_STATUS_KEY
import pl.fzar.dokumed.widgets.glance.ProfileGlanceWidget.Companion.STATUS_NEEDS_UPDATE

class ProfileWidgetUpdateWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val manager = GlanceAppWidgetManager(appContext)
        val glanceIds = manager.getGlanceIds(ProfileGlanceWidget::class.java)

        return try {
            glanceIds.forEach { glanceId ->
                updateAppWidgetState(appContext, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[DATA_STATUS_KEY] = STATUS_NEEDS_UPDATE
                    }
                }
                ProfileGlanceWidget().update(appContext, glanceId)
            }
            Result.success()
        } catch (e: Exception) {
            // Log.e("ProfileWidgetUpdateWorker", "Error updating widget", e)
            Result.failure()
        }
    }
}
