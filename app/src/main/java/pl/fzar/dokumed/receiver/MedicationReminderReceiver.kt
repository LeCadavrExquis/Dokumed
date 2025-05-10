package pl.fzar.dokumed.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import pl.fzar.dokumed.util.createMedicationReminderNotificationChannel
import pl.fzar.dokumed.util.showMedicationReminderNotification

class MedicationReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_MEDICATION_NAME = "pl.fzar.dokumed.EXTRA_MEDICATION_NAME"
        const val EXTRA_MEDICATION_DOSAGE = "pl.fzar.dokumed.EXTRA_MEDICATION_DOSAGE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        createMedicationReminderNotificationChannel(context) // Ensure channel is created

        val medicationName = intent.getStringExtra(EXTRA_MEDICATION_NAME) ?: "Medication"
        val medicationDosage = intent.getStringExtra(EXTRA_MEDICATION_DOSAGE) ?: "As prescribed"

        showMedicationReminderNotification(context, medicationName, medicationDosage)
    }
}
