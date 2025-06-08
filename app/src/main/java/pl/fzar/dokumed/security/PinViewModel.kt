package pl.fzar.dokumed.security

import android.app.Application
import android.content.Context
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
import org.koin.core.component.inject // Ensure inject is imported

// private const val PIN_KEY = "app_pin" // No longer needed for storing actual PIN
private const val BIOMETRIC_ENABLED_KEY = "biometric_enabled"
private const val TEMP_PIN_PREF_KEY = "temp_pin" // For confirming PIN during setup
private const val PIN_LENGTH = 4

class PinViewModel(application: Application) : ViewModel(), KoinComponent {

    private val prefs: SharedPreferences = application.getSharedPreferences("DokumedPrefs", Context.MODE_PRIVATE)
    private val keystoreHelper: KeystoreHelper by inject() // Koin Injected KeystoreHelper

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

    private var isSettingUpPin = false // Tracks if the current flow is for initial PIN setup

    private val _isPinEnabledFlow = MutableStateFlow(false)
    val isPinEnabledFlow: StateFlow<Boolean> = _isPinEnabledFlow.asStateFlow()

    init {
        // User's preference to use a PIN (e.g., from onboarding or settings)
        val userWantsPinEnabled = prefs.getBoolean("is_pin_enabled", false)
        _isPinEnabledFlow.value = userWantsPinEnabled

        if (userWantsPinEnabled) {
            if (keystoreHelper.isPinSet()) {
                _pinState.value = PinScreenState.EnterPin
                isSettingUpPin = false
            } else {
                // User wants PIN, but it's not in Keystore (e.g., first time, data clear)
                _pinState.value = PinScreenState.SetupPin
                isSettingUpPin = true
            }
        } else {
            // User has not enabled PIN feature. If PinScreen is shown (e.g. during onboarding),
            // it should allow setting up a new PIN.
            _pinState.value = PinScreenState.SetupPin // Default to allowing setup
            isSettingUpPin = true // Assume setup context if PIN isn't enabled yet
        }
        // checkPinStatus() // Consider if this is needed or if init logic is sufficient
    }

    // This function might be redundant if init logic is comprehensive.
    // It was originally for checking PIN_KEY in prefs, now KeystoreHelper.isPinSet() is used.
    // It also triggers biometrics. Review its necessity.
    private fun checkPinStatus() {
        viewModelScope.launch {
            val userWantsPin = _isPinEnabledFlow.value
            if (userWantsPin && keystoreHelper.isPinSet()) {
                _pinState.value = PinScreenState.EnterPin
                isSettingUpPin = false
                if (isBiometricAuthEnabled() && canAuthenticateWithBiometrics()) {
                    _showBiometricPrompt.value = true
                }
            } else if (userWantsPin && !keystoreHelper.isPinSet()){
                _pinState.value = PinScreenState.SetupPin
                isSettingUpPin = true
            } else {
                // User doesn't want PIN, or it's part of onboarding to set one up.
                _pinState.value = PinScreenState.SetupPin
                isSettingUpPin = true
            }
        }
    }

    fun onPinDigitEntered(digit: String) {
        if (_pinInput.value.length < PIN_LENGTH) {
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
        if (currentPin.length != PIN_LENGTH) {
            _toastMessage.value = "PIN must be $PIN_LENGTH digits"
            return
        }

        when (val currentState = _pinState.value) {
            is PinScreenState.SetupPin -> {
                prefs.edit().putString(TEMP_PIN_PREF_KEY, currentPin).apply()
                _pinState.value = PinScreenState.ConfirmPin
                _pinInput.value = ""
                _toastMessage.value = "Confirm your PIN"
            }
            is PinScreenState.ConfirmPin -> {
                val tempPin = prefs.getString(TEMP_PIN_PREF_KEY, null)
                if (tempPin == currentPin) {
                    keystoreHelper.setPin(currentPin) // Securely store the PIN
                    prefs.edit().remove(TEMP_PIN_PREF_KEY).apply()

                    _isPinEnabledFlow.value = true // User has now enabled and set up a PIN
                    prefs.edit().putBoolean("is_pin_enabled", true).apply()
                    isSettingUpPin = false

                    _pinState.value = PinScreenState.PinSetSuccessfully
                    _toastMessage.value = "PIN set successfully!"
                    _pinState.value = PinScreenState.AskBiometrics // Proceed to ask for biometrics
                } else {
                    _toastMessage.value = "PINs do not match. Try again."
                    _pinState.value = PinScreenState.SetupPin // Go back to initial setup
                    prefs.edit().remove(TEMP_PIN_PREF_KEY).apply()
                    _pinInput.value = ""
                }
            }
            is PinScreenState.EnterPin -> {
                if (keystoreHelper.checkPin(currentPin)) {
                    _navigateToMain.value = true
                } else {
                    _toastMessage.value = "Incorrect PIN"
                    _pinInput.value = "" // Clear input after failed attempt
                }
            }
            else -> { /* No action for Loading, AskBiometrics, PinSetSuccessfully */ }
        }
    }

    // This method is called when PinViewModel's setPin is directly invoked (e.g. from tests or specific flows).
    // Typically, onPinConfirmClicked handles the full user-driven setup flow.
    fun setPin(pin: String) {
        keystoreHelper.setPin(pin)
        _isPinEnabledFlow.value = true
        prefs.edit().putBoolean("is_pin_enabled", true).apply()
        isSettingUpPin = false
        _pinState.value = PinScreenState.PinSetSuccessfully
        // Consider if navigation or biometric prompt should follow here too.
    }

    // This method is for direct PIN checking, e.g., from settings or other parts of the app.
    // The onPinConfirmClicked handles PIN checking during login flow.
    fun checkPin(pin: String): Boolean {
        val isCorrect = keystoreHelper.checkPin(pin)
        if (isCorrect) {
            // This method is a direct check, navigation/state change should be handled by caller
            // _navigateToMain.value = true // Avoid direct navigation from a simple check method
        } else {
            _toastMessage.value = "Invalid PIN (from checkPin)" // Differentiate if needed
            _pinInput.value = "" // Clear input after failed attempt
        }
        return isCorrect
    }

    fun skipPinSetup() {
        _isPinEnabledFlow.value = false
        prefs.edit().putBoolean("is_pin_enabled", false).apply()
        isSettingUpPin = false
        // This signals that PIN is not active. MainActivity observing isPinEnabledFlow will handle navigation.
        _navigateToMain.value = true // Or a specific callback for onboarding completion
    }

    fun removePin() {
        keystoreHelper.removePin()
        _isPinEnabledFlow.value = false
        prefs.edit().putBoolean("is_pin_enabled", false).apply()
        prefs.edit().putBoolean(BIOMETRIC_ENABLED_KEY, false).apply() // Also disable biometrics if PIN is removed
        _pinState.value = PinScreenState.SetupPin // If PIN screen is shown again, default to setup
        isSettingUpPin = true
        _pinInput.value = ""
        _toastMessage.value = "PIN and biometrics (if enabled) have been removed"
    }

    fun onBiometricAuthSucceeded() {
        _navigateToMain.value = true
        _showBiometricPrompt.value = false
        _toastMessage.value = "Biometric authentication successful"
    }

    fun onBiometricAuthFailed(errorMsg: String? = "Biometric authentication failed") {
        _showBiometricPrompt.value = false
        _toastMessage.value = errorMsg
        if (!isSettingUpPin && _isPinEnabledFlow.value && keystoreHelper.isPinSet()) {
            _pinState.value = PinScreenState.EnterPin // Fallback to PIN entry if PIN is set
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
                        prefs.edit().putBoolean(BIOMETRIC_ENABLED_KEY, true).apply()
                        _toastMessage.value = "Biometric authentication enabled"
                        _navigateToMain.value = true
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        _toastMessage.value = "Biometric setup failed: $errString"
                        _navigateToMain.value = true
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        _toastMessage.value = "Biometric authentication failed"
                        _navigateToMain.value = true
                    }
                })
            biometricPrompt.authenticate(promptInfo)
        } else if (!enable) {
            prefs.edit().putBoolean(BIOMETRIC_ENABLED_KEY, false).apply()
            _toastMessage.value = "Biometric authentication disabled"
            _navigateToMain.value = true
        } else {
            _toastMessage.value = "Biometrics not available or not enrolled."
            _navigateToMain.value = true
        }
        // After this flow, navigateToMain is true, so MainActivity should handle navigation.
        // No need to change _pinState here as this is usually a side-flow from AskBiometrics or settings.
    }

    fun attemptBiometricAuthentication(activity: FragmentActivity) {
        if (!isBiometricAuthEnabled() || !canAuthenticateWithBiometrics(activity)) {
            _toastMessage.value = "Biometric authentication not enabled or not available."
            if (_isPinEnabledFlow.value && keystoreHelper.isPinSet()) {
                 _pinState.value = PinScreenState.EnterPin // Fallback to PIN if it's set up
            }
            return
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Login")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Use PIN")
            .build()

        val biometricPrompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onBiometricAuthSucceeded()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                        onBiometricAuthFailed("Authentication error: $errString")
                    } else {
                        _showBiometricPrompt.value = false
                        if (_isPinEnabledFlow.value && keystoreHelper.isPinSet()) {
                           _pinState.value = PinScreenState.EnterPin // Fallback to PIN
                        }
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
        // Biometrics should only be considered enabled if a PIN is also set in the keystore.
        return prefs.getBoolean(BIOMETRIC_ENABLED_KEY, false) && keystoreHelper.isPinSet()
    }

    private fun canAuthenticateWithBiometrics(activity: FragmentActivity? = null): Boolean {
        val context = activity ?: return false
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
