package pl.fzar.dokumed.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import org.koin.androidx.compose.koinViewModel
import pl.fzar.dokumed.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController, // Added NavController
    viewModel: ProfileViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

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
            // Display Profile Information (Read-Only)
            ProfileDetailItem(label = stringResource(id = R.string.profile_height), value = uiState.height)
            ProfileDetailItem(label = stringResource(id = R.string.profile_weight), value = uiState.weight)
            ProfileDetailItem(label = stringResource(id = R.string.profile_blood_type), value = uiState.bloodType)
            ProfileDetailItem(label = stringResource(id = R.string.profile_illnesses), value = uiState.illnesses)
            ProfileDetailItem(label = stringResource(id = R.string.profile_medications), value = uiState.medications)
            ProfileDetailItem(label = stringResource(id = R.string.profile_allergies), value = uiState.allergies)

            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(id = R.string.profile_emergency_contact), style = MaterialTheme.typography.titleMedium)
            ProfileDetailItem(label = stringResource(id = R.string.profile_emergency_contact_name), value = uiState.emergencyContactName)
            ProfileDetailItem(label = stringResource(id = R.string.profile_emergency_contact_phone), value = uiState.emergencyContactPhone)

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.profile_organ_donor))
                Spacer(Modifier.weight(1f))
                Text(if (uiState.organDonor) "Yes" else "No", style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Navigation Buttons
            Button(
                onClick = { navController.navigate("profileEdit") /* Replace with your actual route */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Edit Profile Details")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { navController.navigate("medicationReminders") /* Replace with your actual route */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Medication Reminders")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { navController.navigate("cloudSync") /* Replace with your actual route */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cloud Sync Settings")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { navController.navigate("panicButtonSettings") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Panic Button Settings")
            }
            // Removed the general "Save" button as saving is handled in specific screens
        }
    }
}

@Composable
private fun ProfileDetailItem(label: String, value: String) {
    if (value.isNotBlank()) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)) {
            Text("$label:", style = MaterialTheme.typography.titleSmall)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}