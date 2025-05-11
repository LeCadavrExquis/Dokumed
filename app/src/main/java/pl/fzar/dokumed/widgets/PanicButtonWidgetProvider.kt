package pl.fzar.dokumed.widgets

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Button
import android.widget.EditText
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.serialization.json.Json
import pl.fzar.dokumed.ui.profile.ProfileScreenState // Assuming ProfileScreenState is @Serializable

import pl.fzar.dokumed.R
import pl.fzar.dokumed.widgets.PanicButtonWidgetProvider.Companion.loadPanicNumber
import pl.fzar.dokumed.widgets.PanicButtonWidgetProvider.Companion.savePanicNumber
import pl.fzar.dokumed.widgets.PanicButtonWidgetProvider.Companion.updateAppWidget

class PanicButtonWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            // First, ensure the widget UI and click listeners are set up
            updateAppWidget(context, appWidgetManager, appWidgetId)

            // Attempt to load number already configured for this widget instance
            var phoneNumber = loadPanicNumber(context, appWidgetId)

            if (phoneNumber == null) {
                // Number not found in widget-specific storage. Try options bundle (e.g., from pinning).
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                val phoneNumberFromOptions = options.getString(EXTRA_PHONE_NUMBER_FROM_SETTINGS)

                if (phoneNumberFromOptions != null && phoneNumberFromOptions.isNotBlank()) {
                    phoneNumber = phoneNumberFromOptions
                    savePanicNumber(context, appWidgetId, phoneNumber) // Save it to widget-specific storage
                } else {
                    // Still no number, try to load from global profile settings
                    val profilePrefs = context.getSharedPreferences(MAIN_APP_PREFS_NAME, Context.MODE_PRIVATE)
                    val profileJson = profilePrefs.getString(PROFILE_DATA_KEY, null)
                    if (profileJson != null) {
                        try {
                            val profileState = Json.decodeFromString<ProfileScreenState>(profileJson)
                            if (profileState.emergencyContactPhone.isNotBlank()) {
                                phoneNumber = profileState.emergencyContactPhone
                                savePanicNumber(context, appWidgetId, phoneNumber) // Save global number to widget-specific storage
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // Log error
                        }
                    }
                }
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            deletePanicNumber(context, appWidgetId)
        }
    }

    companion object {
        private const val PREFS_NAME = "pl.fzar.dokumed.widgets.PanicButtonWidgetProvider"
        private const val PREF_PREFIX_KEY = "panic_number_"
        private const val ACTION_CALL_PANIC = "pl.fzar.dokumed.widgets.ACTION_CALL_PANIC"
        private const val EXTRA_APP_WIDGET_ID = "extra_app_widget_id"
        const val EXTRA_PHONE_NUMBER_FROM_SETTINGS = "EXTRA_PHONE_NUMBER_FROM_SETTINGS" // Added constant

        // Constants for accessing global profile data
        private const val MAIN_APP_PREFS_NAME = "DokumedPrefs" // Matches MainActivity & ProfileRepositoryImpl
        private const val PROFILE_DATA_KEY = "profile_data_json" // Matches ProfileRepositoryImpl


        internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.panic_button_widget_layout)
            val intent = Intent(context, PanicButtonWidgetProvider::class.java).apply {
                action = ACTION_CALL_PANIC
                putExtra(EXTRA_APP_WIDGET_ID, appWidgetId)
                // Ensure a unique PendingIntent for each widget instance
                data = Uri.withAppendedPath(Uri.parse("dokumed://widget/id/"), appWidgetId.toString())
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId, // Use appWidgetId as requestCode to ensure uniqueness
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.panic_button_widget_button, pendingIntent)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        internal fun savePanicNumber(context: Context, appWidgetId: Int, number: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
            prefs.putString(PREF_PREFIX_KEY + appWidgetId, number)
            prefs.apply()
        }

        internal fun loadPanicNumber(context: Context, appWidgetId: Int): String? {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            return prefs.getString(PREF_PREFIX_KEY + appWidgetId, null)
        }

        internal fun deletePanicNumber(context: Context, appWidgetId: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
            prefs.remove(PREF_PREFIX_KEY + appWidgetId)
            prefs.apply()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_CALL_PANIC) {
            val appWidgetId = intent.getIntExtra(EXTRA_APP_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val phoneNumber = loadPanicNumber(context, appWidgetId)
                if (phoneNumber != null) {
                    // Attempt to send SMS
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                        try {
                            val smsManager = SmsManager.getDefault()
                            val message = "Panic button activated! Attempting to call for help."
                            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                            Toast.makeText(context, "Emergency SMS sent.", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to send SMS.", Toast.LENGTH_SHORT).show()
                            e.printStackTrace()
                        }
                    } else {
                        Toast.makeText(context, "SEND_SMS permission needed.", Toast.LENGTH_LONG).show()
                    }

                    // Attempt to make a call
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber"))
                        callIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        try {
                            context.startActivity(callIntent)
                        } catch (e: SecurityException) {
                            Toast.makeText(context, "Failed to initiate call. Check permissions.", Toast.LENGTH_LONG).show()
                            e.printStackTrace()
                        }
                    } else {
                        Toast.makeText(context, "CALL_PHONE permission needed.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "Emergency number not set for this widget.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

class PanicButtonWidgetConfigureActivity : Activity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var phoneNumberEditText: EditText

    // Constants for accessing profile's emergency contact phone from SharedPreferences
    private companion object {
        // Removed PROFILE_PREFS_NAME and PROFILE_KEY_EMERGENCY_CONTACT_PHONE
        private const val MAIN_APP_PREFS_NAME = "DokumedPrefs" // Matches MainActivity
        private const val PROFILE_DATA_KEY = "profile_data_json" // Matches ProfileRepositoryImpl
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

        // Attempt to load emergency contact from global profile settings
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
                // Log error or handle deserialization failure
                e.printStackTrace()
                Toast.makeText(this, "Error reading profile settings.", Toast.LENGTH_SHORT).show()
            }
        }

        if (globalEmergencyPhone != null) {
            // Global contact exists. Check permissions.
            val callPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
            val smsPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

            if (callPermissionGranted && smsPermissionGranted) {
                // Global contact exists AND permissions are granted. Configure silently.
                savePanicNumber(this, appWidgetId, globalEmergencyPhone)
                val appWidgetManager = AppWidgetManager.getInstance(this)
                updateAppWidget(this, appWidgetManager, appWidgetId)

                val resultValue = Intent()
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                setResult(RESULT_OK, resultValue)
                finish()
                return // Successfully auto-configured
            } else {
                // Global contact exists, but permissions are missing. Pre-fill and show config screen.
                phoneNumberEditText.setText(globalEmergencyPhone)
                // The existing click listener for the add button will call checkAndRequestPermissions()
            }
        } else {
            // No global emergency contact found or it's blank.
            // Try to pre-fill with widget-specific number if reconfiguring.
            val existingNumberForWidget = loadPanicNumber(this, appWidgetId)
            if (existingNumberForWidget != null) {
                phoneNumberEditText.setText(existingNumberForWidget)
            }
            // If still no number, EditText will be empty, user needs to input.
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
            // Permissions are already granted, proceed to save
            saveConfiguration()
        }
    }

    private fun saveConfiguration() {
        val context = this@PanicButtonWidgetConfigureActivity
        val phoneNumber = phoneNumberEditText.text.toString()

        if (phoneNumber.isBlank()) {
            Toast.makeText(context, "Phone number cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        PanicButtonWidgetProvider.savePanicNumber(context, appWidgetId, phoneNumber)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        PanicButtonWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId)

        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // We save the configuration regardless of whether permissions were granted or not at this stage.
            // The widget itself will handle missing permissions at runtime with Toasts.
            // However, you could add specific logic here if needed, e.g., show a message if permissions are denied.
            if (grantResults.isNotEmpty()) { // Check if grantResults is not empty
                val callPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                val smsPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

                if (!callPermissionGranted) {
                    Toast.makeText(this, "Call permission denied. The widget might not function fully.", Toast.LENGTH_LONG).show()
                }
                if (!smsPermissionGranted) {
                    Toast.makeText(this, "SMS permission denied. The widget might not function fully.", Toast.LENGTH_LONG).show()
                }
            }
            saveConfiguration() // Proceed to save the configuration
        }
    }
}
