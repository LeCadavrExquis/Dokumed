package pl.fzar.dokumed.widgets.glance

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import pl.fzar.dokumed.R

class PanicHandlerActivity : Activity() {

    private var phoneNumber: String? = null
    private val PERMISSION_REQUEST_CODE = 102 // Differentiate from configure activity's code

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This activity is transparent, so no layout is set.

        phoneNumber = intent.getStringExtra("PHONE_NUMBER")

        if (phoneNumber.isNullOrBlank()) {
            Toast.makeText(this, "Error: Phone number not provided.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        checkAndRequestPermissions()
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
            // Permissions are already granted, proceed to action
            performPanicActions()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                performPanicActions()
            } else {
                Toast.makeText(this, getString(R.string.panic_button_permissions_not_granted_widget), Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun performPanicActions() {
        phoneNumber?.let { number ->
            // Attempt to send SMS
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                try {
                    val smsManager = SmsManager.getDefault()
                    // Consider making the SMS message configurable or adding more context
                    val message = getString(R.string.panic_button_sms_message_widget) 
                    smsManager.sendTextMessage(number, null, message, null, null)
                    Toast.makeText(this, getString(R.string.panic_button_sms_sent_widget), Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, getString(R.string.panic_button_sms_failed_widget), Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            } else {
                // This case should ideally be caught by permission check, but as a fallback:
                Toast.makeText(this, "SEND_SMS permission needed.", Toast.LENGTH_LONG).show()
            }

            // Attempt to make a call
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
                callIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK // Though not strictly necessary from an Activity
                try {
                    startActivity(callIntent)
                } catch (e: SecurityException) {
                    Toast.makeText(this, getString(R.string.panic_button_call_failed_widget), Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            } else {
                 // This case should ideally be caught by permission check, but as a fallback:
                Toast.makeText(this, "CALL_PHONE permission needed.", Toast.LENGTH_LONG).show()
            }
        }
        finish() // Close the transparent activity after actions
    }
}
