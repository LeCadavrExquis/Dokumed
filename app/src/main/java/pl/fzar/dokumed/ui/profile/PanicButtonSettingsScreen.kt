package pl.fzar.dokumed.ui.profile

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle // Required for Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import org.koin.androidx.compose.koinViewModel
import pl.fzar.dokumed.R
import pl.fzar.dokumed.widgets.glance.PanicGlanceWidget // Import for Glance widget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanicButtonSettingsScreen(
    navController: NavController,
    viewModel: ProfileViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var emergencyContactPhone by remember(uiState.emergencyContactPhone) { mutableStateOf(uiState.emergencyContactPhone) }
    // var emergencyContactName by remember(uiState.emergencyContactName) { mutableStateOf(uiState.emergencyContactName) } // Keep for consistency if needed

    val context = LocalContext.current
    val permissionsToRequest = arrayOf(
        Manifest.permission.CALL_PHONE,
        Manifest.permission.SEND_SMS
    )
    var permissionsGrantedState by remember { mutableStateOf(false) } // Renamed to avoid conflict

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGrantedState = permissions.values.all { it }
        if (permissionsGrantedState) {
            viewModel.updateEmergencyContact(
                name = uiState.emergencyContactName, // Or use a TextField for name as well
                phone = emergencyContactPhone
            )
            Toast.makeText(context, context.getString(R.string.toast_emergency_contact_saved), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, context.getString(R.string.toast_permissions_denied_contact_not_saved), Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        permissionsGrantedState = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    val appWidgetManager = AppWidgetManager.getInstance(context)
    // Use the Glance widget's class for the component name
    val componentName = ComponentName(context, PanicGlanceWidget::class.java)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.panic_button_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(R.string.panic_button_settings_description),
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = emergencyContactPhone,
                onValueChange = { emergencyContactPhone = it },
                label = { Text(stringResource(R.string.profile_emergency_contact_phone)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            Button(
                onClick = {
                    if (permissionsToRequest.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
                        permissionsGrantedState = true // All permissions already granted
                        viewModel.updateEmergencyContact(
                            name = uiState.emergencyContactName, // Or use a TextField for name as well
                            phone = emergencyContactPhone
                        )
                        Toast.makeText(context, context.getString(R.string.toast_emergency_contact_saved), Toast.LENGTH_SHORT).show()
                    } else {
                        permissionLauncher.launch(permissionsToRequest)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_save_emergency_contact))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appWidgetManager.isRequestPinAppWidgetSupported) {
                Button(
                    onClick = {
                        // Re-check current permission status before attempting to add widget
                        val currentPermissionsGranted = permissionsToRequest.all {
                            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                        }
                        if (emergencyContactPhone.isNotBlank() && currentPermissionsGranted) {
                            // Pass the current phone number as an extra
                            val extras = Bundle().apply {
                                putString("EXTRA_PHONE_NUMBER_FROM_SETTINGS", emergencyContactPhone)
                            }
                            val success = appWidgetManager.requestPinAppWidget(componentName, extras, null)
                            if (!success) {
                                Toast.makeText(context, context.getString(R.string.toast_widget_pin_failed), Toast.LENGTH_LONG).show()
                            }
                        } else if (emergencyContactPhone.isBlank()){
                             Toast.makeText(context, context.getString(R.string.toast_save_number_first), Toast.LENGTH_LONG).show()
                        } else if (!currentPermissionsGranted) {
                            Toast.makeText(context, context.getString(R.string.toast_grant_permissions_first), Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_add_panic_widget))
                }
            } else {
                Text(
                    stringResource(R.string.panic_widget_add_manually_unsupported),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Show rationale only if permissions were explicitly denied (permissionsGrantedState is false after a launch)
            // or if they are not granted initially.
            val arePermissionsCurrentlyGranted = permissionsToRequest.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
            if (!arePermissionsCurrentlyGranted) {
                Text(
                    stringResource(R.string.panic_button_permission_rationale),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
