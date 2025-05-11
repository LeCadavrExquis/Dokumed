package pl.fzar.dokumed.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import pl.fzar.dokumed.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    navController: NavController, // Or your navigation mechanism
    viewModel: ProfileViewModel // Assuming ProfileViewModel is hoisted or koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        // Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") // Example
                        Text("Back") // Placeholder if you don't have Icons.AutoMirrored
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
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    viewModel.saveProfileData()
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
