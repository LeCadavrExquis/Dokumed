package pl.fzar.dokumed.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncScreen(
    navController: NavController, // Or your navigation mechanism
    viewModel: ProfileViewModel // Assuming ProfileViewModel is hoisted or koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cloud Sync Settings") },
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
                onClick = { 
                    // It's good practice to save settings before attempting to sync
                    viewModel.saveProfileData() // Assuming this saves WebDAV credentials
                    viewModel.syncProfileToWebDAV()
                    // Optionally provide feedback to the user about the sync attempt
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings & Sync to Cloud")
            }
            Spacer(modifier = Modifier.height(16.dp))
            // The main save button for just credentials might be redundant if sync also saves.
            // If only saving credentials without immediate sync is needed, add a separate save button.
            Button(
                onClick = {
                    viewModel.saveProfileData()
                    navController.popBackStack() // Navigate back after saving
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Save Settings")
            }
        }
    }
}
