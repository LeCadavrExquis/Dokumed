// filepath: /home/filip/AndroidStudioProjects/Dokumed/app/src/main/java/pl/fzar/dokumed/widgets/PanicButtonWidgetProvider.kt
package pl.fzar.dokumed.widgets

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.serialization.json.Json
import pl.fzar.dokumed.ui.profile.ProfileScreenState

import pl.fzar.dokumed.R
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import pl.fzar.dokumed.widgets.glance.PANIC_NUMBER_KEY
import pl.fzar.dokumed.widgets.glance.PanicGlanceWidget

class PanicButtonWidgetConfigureActivity : Activity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var phoneNumberEditText: EditText

    private companion object {
        private const val MAIN_APP_PREFS_NAME = "DokumedPrefs"
        private const val PROFILE_DATA_KEY = "profile_data_json"
        private const val PERMISSION_REQUEST_CODE = 101
    }

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setResult(RESULT_CANCELED)
        setContentView(R.layout.panic_button_widget_configure)

        phoneNumberEditText = findViewById(R.id.appwidget_text_phone_number)
        findViewById<Button>(R.id.add_button_widget_configure).setOnClickListener {
            checkAndRequestPermissions()
        }

        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val profilePrefs = getSharedPreferences(MAIN_APP_PREFS_NAME, Context.MODE_PRIVATE)
        val profileJson = profilePrefs.getString(PROFILE_DATA_KEY, null)
        var globalEmergencyPhone: String? = null

        if (profileJson != null) {
            try {
                val profileState = Json.decodeFromString<ProfileScreenState>(profileJson)
                if (profileState.emergencyContactPhone.isNotBlank()) {
                    globalEmergencyPhone = profileState.emergencyContactPhone
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, getString(R.string.error_reading_profile_settings), Toast.LENGTH_SHORT).show()
            }
        }

        MainScope().launch {
            val glanceManager = GlanceAppWidgetManager(this@PanicButtonWidgetConfigureActivity)
            val glanceId = glanceManager.getGlanceIdBy(appWidgetId)
            var existingGlanceNumber: String? = null
            if (glanceId != null) {
                val currentPrefs = androidx.glance.appwidget.state.getAppWidgetState(this@PanicButtonWidgetConfigureActivity, PreferencesGlanceStateDefinition, glanceId)
                existingGlanceNumber = currentPrefs[PANIC_NUMBER_KEY]
            }

            if (existingGlanceNumber != null) {
                phoneNumberEditText.setText(existingGlanceNumber)
            } else if (globalEmergencyPhone != null) {
                val callPermissionGranted = ContextCompat.checkSelfPermission(this@PanicButtonWidgetConfigureActivity, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                val smsPermissionGranted = ContextCompat.checkSelfPermission(this@PanicButtonWidgetConfigureActivity, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
                if (callPermissionGranted && smsPermissionGranted) {
                    saveConfigurationForGlance(this@PanicButtonWidgetConfigureActivity, appWidgetId, globalEmergencyPhone)
                    return@launch
                } else {
                    phoneNumberEditText.setText(globalEmergencyPhone)
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CALL_PHONE)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.SEND_SMS)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            saveConfigurationForGlance(this, appWidgetId, phoneNumberEditText.text.toString())
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                saveConfigurationForGlance(this, appWidgetId, phoneNumberEditText.text.toString())
            } else {
                Toast.makeText(this, getString(R.string.permissions_required_for_panic_button), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveConfigurationForGlance(context: Context, currentAppWidgetId: Int, phoneNumber: String) {
        if (phoneNumber.isBlank()) {
            Toast.makeText(context, getString(R.string.phone_number_empty_error), Toast.LENGTH_SHORT).show()
            return
        }

        MainScope().launch {
            val glanceManager = GlanceAppWidgetManager(context)
            val glanceId = glanceManager.getGlanceIdBy(currentAppWidgetId)
            if (glanceId == null) {
                Toast.makeText(context, getString(R.string.panic_widget_glance_id_error), Toast.LENGTH_SHORT).show()
                setResult(RESULT_CANCELED)
                finish()
                return@launch
            }

            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { currentPrefs ->
                currentPrefs.toMutablePreferences().apply {
                    this[PANIC_NUMBER_KEY] = phoneNumber
                }
            }

            // Ensure the widget updates its UI after configuration
            PanicGlanceWidget().update(context, glanceId)

            Toast.makeText(context, getString(R.string.panic_widget_config_saved_success), Toast.LENGTH_SHORT).show()

            val resultValue = Intent()
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, currentAppWidgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
}
