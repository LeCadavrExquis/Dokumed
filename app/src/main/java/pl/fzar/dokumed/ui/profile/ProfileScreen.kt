package pl.fzar.dokumed.ui.profile

import android.app.TimePickerDialog
import android.widget.TimePicker
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import pl.fzar.dokumed.R
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var showTimePicker by remember { mutableStateOf(false) }
    val calendar = Calendar.getInstance()

    if (showTimePicker) {
        TimePickerDialog(
            context,
            { _: TimePicker, hourOfDay: Int, minute: Int ->
                val formattedTime = String.format("%02d:%02d", hourOfDay, minute)
                viewModel.onMedicationReminderTimeChange(formattedTime)
                showTimePicker = false
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true // 24 hour format
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.profile_title)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            OutlinedTextField(
                value = uiState.height,
                onValueChange = viewModel::onHeightChange,
                label = { Text(stringResource(id = R.string.profile_height)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.weight,
                onValueChange = viewModel::onWeightChange,
                label = { Text(stringResource(id = R.string.profile_weight)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.bloodType,
                onValueChange = viewModel::onBloodTypeChange,
                label = { Text(stringResource(id = R.string.profile_blood_type)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.illnesses,
                onValueChange = viewModel::onIllnessesChange,
                label = { Text(stringResource(id = R.string.profile_illnesses)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.medications,
                onValueChange = viewModel::onMedicationsChange,
                label = { Text(stringResource(id = R.string.profile_medications)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.allergies,
                onValueChange = viewModel::onAllergiesChange,
                label = { Text(stringResource(id = R.string.profile_allergies)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(id = R.string.profile_emergency_contact), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.emergencyContactName,
                onValueChange = viewModel::onEmergencyContactNameChange,
                label = { Text(stringResource(id = R.string.profile_emergency_contact_name)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.emergencyContactPhone,
                onValueChange = viewModel::onEmergencyContactPhoneChange,
                label = { Text(stringResource(id = R.string.profile_emergency_contact_phone)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.profile_organ_donor))
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = uiState.organDonor,
                    onCheckedChange = viewModel::onOrganDonorChange
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(id = R.string.profile_medication_reminder_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.profile_medication_reminder_enable))
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = uiState.medicationReminderEnabled,
                    onCheckedChange = viewModel::onMedicationReminderEnabledChange
                )
            }
            if (uiState.medicationReminderEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.medicationName,
                    onValueChange = viewModel::onMedicationNameChange,
                    label = { Text(stringResource(id = R.string.profile_medication_reminder_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.medicationDosage,
                    onValueChange = viewModel::onMedicationDosageChange,
                    label = { Text(stringResource(id = R.string.profile_medication_reminder_dosage)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text) // Changed from Number
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.medicationReminderTime,
                    onValueChange = { }, // Handled by clickable modifier
                    label = { Text(stringResource(id = R.string.profile_medication_reminder_time)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePicker = true },
                    readOnly = true,
                    placeholder = { Text("HH:MM") } // Placeholder for time format
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // WebDAV Synchronization Section
            Text("WebDAV Cloud Sync", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.webdavUrl,
                onValueChange = viewModel::onWebdavUrlChange,
                label = { Text("WebDAV Server URL") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.webdavUsername,
                onValueChange = viewModel::onWebdavUsernameChange,
                label = { Text("WebDAV Username") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.webdavPassword,
                onValueChange = viewModel::onWebdavPasswordChange,
                label = { Text("WebDAV Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.syncProfileToWebDAV() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sync to Cloud")
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.saveProfileData() },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(id = R.string.save))
            }
        }
    }
}