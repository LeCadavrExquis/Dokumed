package pl.fzar.dokumed.ui.security

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import pl.fzar.dokumed.security.PinScreenState
import pl.fzar.dokumed.security.PinViewModel


@Composable
fun PinScreen(
    pinViewModel: PinViewModel,
    activity: FragmentActivity,
    onNavigateToMain: () -> Unit
) {
    val pinState by pinViewModel.pinState.collectAsState()
    val pinInput by pinViewModel.pinInput.collectAsState()
    val showBiometricPrompt by pinViewModel.showBiometricPrompt.collectAsState()
    val navigateToMain by pinViewModel.navigateToMain.collectAsState()
    val toastMessage by pinViewModel.toastMessage.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            pinViewModel.consumeToast()
        }
    }

    LaunchedEffect(navigateToMain) {
        if (navigateToMain) {
            onNavigateToMain()
            pinViewModel.consumeNavigation()
        }
    }

    LaunchedEffect(showBiometricPrompt) {
        if (showBiometricPrompt && pinState is PinScreenState.EnterPin) {
            pinViewModel.attemptBiometricAuthentication(activity)
        }
    }
    
    // Handle hiding biometric prompt if user navigates away or state changes
    DisposableEffect(Unit) {
        onDispose {
            pinViewModel.hideBiometricPrompt()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (val state = pinState) {
                PinScreenState.Loading -> {
                    CircularProgressIndicator()
                }
                PinScreenState.SetupPin -> PinInputView("Set up your PIN", pinInput, pinViewModel)
                PinScreenState.ConfirmPin -> PinInputView("Confirm your PIN", pinInput, pinViewModel)
                PinScreenState.EnterPin -> {
                    PinInputView("Enter your PIN", pinInput, pinViewModel)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { pinViewModel.attemptBiometricAuthentication(activity) }) {
                        Text("Use Biometrics")
                    }
                }
                PinScreenState.AskBiometrics -> {
                    Text("PIN Set Up Successfully!", fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Would you like to enable biometric authentication for faster login?", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row {
                        Button(onClick = { pinViewModel.enableBiometricAuthentication(activity, true) }) {
                            Text("Enable Biometrics")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        OutlinedButton(onClick = { pinViewModel.enableBiometricAuthentication(activity, false) }) {
                            Text("No, thanks")
                        }
                    }
                }
                PinScreenState.PinSetSuccessfully -> { // Should ideally transition quickly
                    Text("PIN configured. Redirecting...", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun PinInputView(
    title: String,
    pinInput: String,
    pinViewModel: PinViewModel
) {
    Text(title, fontSize = 20.sp)
    Spacer(modifier = Modifier.height(24.dp))

    // Display PIN dots (or actual numbers if preferred, but dots are more secure visually)
    Text(
        text = "*".repeat(pinInput.length).padEnd(6, ' '), // Show * for each digit
        fontSize = 24.sp,
        modifier = Modifier.padding(vertical = 16.dp)
    )

    // Simple number pad
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        (1..3).forEach { row ->
            Row {
                (1..3).forEach { col ->
                    val digit = (row - 1) * 3 + col
                    Button(
                        onClick = { pinViewModel.onPinDigitEntered(digit.toString()) },
                        modifier = Modifier
                            .padding(4.dp)
                            .size(70.dp)
                    ) {
                        Text(digit.toString(), fontSize = 18.sp)
                    }
                }
            }
        }
        Row {
            // Placeholder for the first column to align "0" in the center
            Box(modifier = Modifier.padding(4.dp).size(70.dp))

            Button(
                onClick = { pinViewModel.onPinDigitEntered("0") },
                modifier = Modifier
                    .padding(4.dp)
                    .size(70.dp)
            ) {
                Text("0", fontSize = 18.sp)
            }
            Button( // Backspace button
                onClick = { pinViewModel.onBackspaceClicked() },
                modifier = Modifier
                    .padding(4.dp)
                    .size(70.dp)
            ) {
                Text("<-", fontSize = 18.sp) // Simple backspace representation
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = { pinViewModel.onPinConfirmClicked() },
        enabled = pinInput.length >= 4 // Enable when at least 4 digits
    ) {
        Text("Confirm")
    }
}

