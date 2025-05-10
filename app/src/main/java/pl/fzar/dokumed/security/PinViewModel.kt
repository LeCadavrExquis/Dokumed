package pl.fzar.dokumed.security

import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val PIN_KEY = "app_pin"
private const val BIOMETRIC_ENABLED_KEY = "biometric_enabled"

class PinViewModel : ViewModel(), KoinComponent {

    private val encryptedPrefs: SharedPreferences by inject()

    private val _pinState = MutableStateFlow<PinScreenState>(PinScreenState.Loading)
    val pinState: StateFlow<PinScreenState> = _pinState.asStateFlow()

    private val _pinInput = MutableStateFlow("")
    val pinInput: StateFlow<String> = _pinInput.asStateFlow()

    private val _showBiometricPrompt = MutableStateFlow(false)
    val showBiometricPrompt: StateFlow<Boolean> = _showBiometricPrompt.asStateFlow()

    private val _navigateToMain = MutableStateFlow(false)
    val navigateToMain: StateFlow<Boolean> = _navigateToMain.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()
    
    private var isSettingUpPin = false

    init {
        checkPinStatus()
    }

    private fun checkPinStatus() {
        viewModelScope.launch {
            val storedPin = encryptedPrefs.getString(PIN_KEY, null)
            if (storedPin == null) {
                _pinState.value = PinScreenState.SetupPin
                isSettingUpPin = true
            } else {
                _pinState.value = PinScreenState.EnterPin
                isSettingUpPin = false
                if (isBiometricAuthEnabled() && canAuthenticateWithBiometrics()) {
                    _showBiometricPrompt.value = true
                }
            }
        }
    }

    fun onPinDigitEntered(digit: String) {
        if (_pinInput.value.length < 6) {
            _pinInput.value += digit
        }
    }

    fun onBackspaceClicked() {
        if (_pinInput.value.isNotEmpty()) {
            _pinInput.value = _pinInput.value.dropLast(1)
        }
    }

    fun onPinConfirmClicked() {
        val currentPin = _pinInput.value
        if (currentPin.length < 4) {
            _toastMessage.value = "PIN must be at least 4 digits"
            return
        }

        when (_pinState.value) {
            is PinScreenState.SetupPin, is PinScreenState.ConfirmPin -> {
                if (isSettingUpPin) {
                    if (_pinState.value is PinScreenState.SetupPin) {
                        // First time entering PIN during setup
                        encryptedPrefs.edit().putString("temp_pin", currentPin).apply()
                        _pinState.value = PinScreenState.ConfirmPin
                        _pinInput.value = ""
                        _toastMessage.value = "Confirm your PIN"
                    } else {
                        // Confirming PIN
                        val tempPin = encryptedPrefs.getString("temp_pin", null)
                        if (tempPin == currentPin) {
                            setPin(currentPin) // Use the new setPin method
                            _pinState.value = PinScreenState.PinSetSuccessfully
                            _toastMessage.value = "PIN set successfully!"
                            // Optionally ask to enable biometrics here
                             _pinState.value = PinScreenState.AskBiometrics // Transition to ask for biometrics
                        } else {
                            _toastMessage.value = "PINs do not match. Try again."
                            _pinState.value = PinScreenState.SetupPin
                            encryptedPrefs.edit().remove("temp_pin").apply()
                        }
                        _pinInput.value = ""
                    }
                }
            }
            is PinScreenState.EnterPin -> {
                val storedPin = encryptedPrefs.getString(PIN_KEY, null)
                if (storedPin == currentPin) {
                    _navigateToMain.value = true
                } else {
                    _toastMessage.value = "Incorrect PIN"
                    _pinInput.value = ""
                }
            }
            else -> { /* No action needed for other states */ }
        }
    }
    
    fun setPin(pin: String) {
        viewModelScope.launch {
            encryptedPrefs.edit()
                .putString(PIN_KEY, pin)
                .remove("temp_pin") // Clean up temporary PIN if it exists
                .apply()
            // Update internal state if needed, e.g., to reflect PIN is now set
            // For onboarding, this might mean transitioning to the next step or enabling features.
            // If called from a settings screen, it might just confirm success.
            // For now, we assume the primary action is saving the PIN.
            // The existing logic in onPinConfirmClicked already handles state transitions.
        }
    }

    fun onBiometricAuthSucceeded() {
        _navigateToMain.value = true
        _showBiometricPrompt.value = false
        _toastMessage.value = "Biometric authentication successful"
    }

    fun onBiometricAuthFailed(errorMsg: String? = "Biometric authentication failed") {
        _showBiometricPrompt.value = false
        _toastMessage.value = errorMsg
        // Fallback to PIN entry if not setting up
        if (!isSettingUpPin) {
             _pinState.value = PinScreenState.EnterPin
        }
    }
    
    fun enableBiometricAuthentication(activity: FragmentActivity, enable: Boolean) {
        if (enable && canAuthenticateWithBiometrics(activity)) {
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Enable Biometric Authentication")
                .setSubtitle("Confirm biometric to enable this feature")
                .setNegativeButtonText("Cancel")
                .build()

            val biometricPrompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        encryptedPrefs.edit().putBoolean(BIOMETRIC_ENABLED_KEY, true).apply()
                        _toastMessage.value = "Biometric authentication enabled"
                        _navigateToMain.value = true // Proceed to main app
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        _toastMessage.value = "Biometric setup failed: $errString"
                         _navigateToMain.value = true // Proceed to main app even if biometrics fail to enable
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        _toastMessage.value = "Biometric authentication failed"
                         _navigateToMain.value = true // Proceed to main app
                    }
                })
            biometricPrompt.authenticate(promptInfo)
        } else if (!enable) {
            encryptedPrefs.edit().putBoolean(BIOMETRIC_ENABLED_KEY, false).apply()
            _toastMessage.value = "Biometric authentication disabled"
            _navigateToMain.value = true // Proceed to main app
        } else {
             _toastMessage.value = "Biometrics not available or not enrolled."
             _navigateToMain.value = true // Proceed to main app
        }
         _pinState.value = PinScreenState.Loading // Reset state or navigate as needed
         checkPinStatus() // Re-check status, which might lead to EnterPin or navigate
    }


    fun attemptBiometricAuthentication(activity: FragmentActivity) {
        if (!canAuthenticateWithBiometrics(activity)) {
            _toastMessage.value = "Biometric authentication not available or not set up."
            _pinState.value = PinScreenState.EnterPin // Fallback to PIN
            return
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Login")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Use PIN") // Allows user to cancel and use PIN
            .build()

        val biometricPrompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onBiometricAuthSucceeded()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Don't show error if user cancelled to use PIN
                    if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                        onBiometricAuthFailed("Authentication error: $errString")
                    } else {
                         _showBiometricPrompt.value = false // Hide prompt if cancelled
                         _pinState.value = PinScreenState.EnterPin // Ensure PIN entry is shown
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onBiometricAuthFailed()
                }
            })
        biometricPrompt.authenticate(promptInfo)
    }


    private fun isBiometricAuthEnabled(): Boolean {
        return encryptedPrefs.getBoolean(BIOMETRIC_ENABLED_KEY, false)
    }

    private fun canAuthenticateWithBiometrics(activity: FragmentActivity? = null): Boolean {
        val context = activity ?: return false // Requires activity context for BiometricManager
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }
    
    fun consumeToast() {
        _toastMessage.value = null
    }

    fun consumeNavigation() {
        _navigateToMain.value = false
    }
    
    fun hideBiometricPrompt() {
        _showBiometricPrompt.value = false
    }
}

sealed class PinScreenState {
    object Loading : PinScreenState()
    object SetupPin : PinScreenState() // Initial PIN setup
    object ConfirmPin : PinScreenState() // Confirm PIN during setup
    object AskBiometrics : PinScreenState() // Ask user if they want to enable biometrics
    object PinSetSuccessfully : PinScreenState() // Intermediate state, might not be directly observed by UI for long
    object EnterPin : PinScreenState() // Enter existing PIN
}
