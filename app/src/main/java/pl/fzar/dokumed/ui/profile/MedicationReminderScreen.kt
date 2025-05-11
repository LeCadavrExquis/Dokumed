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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import pl.fzar.dokumed.R
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationReminderScreen(
    navController: NavController, // Or your navigation mechanism
    viewModel: ProfileViewModel // Assuming ProfileViewModel is hoisted or koinViewModel()
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
                title = { Text("Medication Reminders") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        // Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        Text("Back") // Placeholder
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
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
                    placeholder = { Text("HH:MM") }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    viewModel.saveProfileData() // Assuming this saves reminder settings too
                    // Optionally navigate back or show a toast
                    navController.popBackStack()
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(id = R.string.save))
            }
        }
    }
}
