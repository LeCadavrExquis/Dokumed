package pl.fzar.dokumed.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pl.fzar.dokumed.security.PinViewModel
import pl.fzar.dokumed.ui.profile.ProfileViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    pinViewModel: PinViewModel,
    profileViewModel: ProfileViewModel,
    onFinishOnboarding: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 4 }) // Assuming 4 pages for now: Welcome, Features, PIN, Profile
    val coroutineScope = rememberCoroutineScope()

    // State for PinSetupPage
    var pinValue by remember { mutableStateOf("") }
    var confirmPinValue by remember { mutableStateOf("") }
    var pinErrorMessage by remember { mutableStateOf<String?>(null) }

    // State for ProfileSetupPage
    var profileNameValue by remember { mutableStateOf("") }
    var profileBloodTypeValue by remember { mutableStateOf("") }
    // Add more profile fields here if needed, e.g.:
    // var profileAllergiesValue by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> FeaturesPage()
                2 -> PinSetupPage(
                    pinViewModel = pinViewModel,
                    pin = pinValue,
                    onPinChange = { pinValue = it; pinErrorMessage = null },
                    confirmPin = confirmPinValue,
                    onConfirmPinChange = { confirmPinValue = it; pinErrorMessage = null },
                    errorMessage = pinErrorMessage
                )
                3 -> ProfileSetupPage(
                    // profileViewModel = profileViewModel, // Pass if ProfileSetupPage needs to call it directly
                    name = profileNameValue,
                    onNameChange = { profileNameValue = it },
                    bloodType = profileBloodTypeValue,
                    onBloodTypeChange = { profileBloodTypeValue = it }
                    // Pass other profile states and their setters here
                )
                else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Page $page (Placeholder)")
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pin state needed for validation before proceeding from PinSetupPage
            // This is a simplified way; ideally, PinSetupPage would expose its state or a validation function.
            // val pinPageState = remember { mutableStateMapOf<String, String>() } // Removed, using hoisted state now

            if (pagerState.currentPage > 0) {
                Button(onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                }) {
                    Text("Back")
                }
            } else {
                Spacer(modifier = Modifier.weight(1f)) // Keep alignment
            }

            Button(onClick = {
                coroutineScope.launch {
                    if (pagerState.currentPage == 2) { // Pin Setup Page
                        if (pinValue.length != 6) {
                            pinErrorMessage = "PIN must be 6 digits."
                            return@launch
                        }
                        if (pinValue != confirmPinValue) {
                            pinErrorMessage = "PINs do not match."
                            return@launch
                        }
                        pinErrorMessage = null // Clear error
                        pinViewModel.setPin(pinValue) // Save the PIN
                    }

                    if (pagerState.currentPage < pagerState.pageCount - 1) {
                        // Only advance if PIN validation passed (it would have returned early otherwise)
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    } else {
                        // This is the last page (Profile Setup) and "Finish" is clicked
                        if (pagerState.currentPage == 3) { // Current page is Profile Setup
                            // Save profile data using ProfileViewModel
                            // Assuming ProfileViewModel has methods to update and save data.
                            // Adjust these calls based on your ProfileViewModel's actual API.
                            profileViewModel.updateName(profileNameValue) // Example method
                            profileViewModel.updateBloodType(profileBloodTypeValue) // Example method
                            // If your ProfileViewModel requires an explicit save call:
                            // profileViewModel.saveProfileData()
                        }
                        onFinishOnboarding()
                    }
                }
            }) {
                Text(if (pagerState.currentPage < pagerState.pageCount -1) "Next" else "Finish")
            }
        }
    }
}

@Composable
fun WelcomePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp), // Increased padding for better spacing
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Welcome to Dokumed!",
            style = MaterialTheme.typography.headlineLarge, // Made title larger
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp)) // Increased spacer
        Text(
            "Your personal health companion for managing medical records securely and efficiently. Keep track of your consultations, lab results, medications, and more, all in one place.",
            style = MaterialTheme.typography.bodyLarge, // Used bodyLarge for better readability
            textAlign = TextAlign.Center
        )
        // TODO: Add an illustrative icon or image if desired
    }
}

@Composable
fun FeaturesPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Discover Dokumed Features",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        FeatureListItem(text = "Securely store and manage all your medical records.")
        FeatureListItem(text = "Categorize records with customizable tags.")
        FeatureListItem(text = "Track various record types: consultations, lab tests, medications, etc.")
        FeatureListItem(text = "Visualize health trends with insightful statistics.")
        FeatureListItem(text = "Easily export your data for backups or sharing with doctors.")
        FeatureListItem(text = "Protect your sensitive information with a secure PIN.")
        FeatureListItem(text = "Maintain a personal health profile.")
        // Consider adding icons next to each feature for better visual appeal
    }
}

@Composable
fun FeatureListItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp), // Add some vertical padding for each item
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Placeholder for an icon, e.g., a checkmark
        Text(
            text = "✓", // Simple checkmark, consider using Material Icons
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary, // Use primary color for the checkmark
            modifier = Modifier.padding(end = 12.dp) // Increased padding
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium // Slightly smaller text for feature description
        )
    }
}

@Composable
fun PinSetupPage(
    pinViewModel: PinViewModel, // Kept if direct interaction is needed, though not used in this revision
    pin: String,
    onPinChange: (String) -> Unit,
    confirmPin: String,
    onConfirmPinChange: (String) -> Unit,
    errorMessage: String?
) {
    // Local states removed, using hoisted states passed as parameters

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Set Up Your Secure PIN",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Create a PIN to protect your medical records. Choose a PIN that is easy for you to remember but hard for others to guess.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = { newValue ->
                onPinChange(newValue.filter { it.isDigit() }.take(6))
            },
            label = { Text("Enter PIN (6 digits)") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            isError = errorMessage != null,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = confirmPin,
            onValueChange = { newValue ->
                onConfirmPinChange(newValue.filter { it.isDigit() }.take(6))
            },
            label = { Text("Confirm PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            isError = errorMessage != null,
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        // Validation and PIN setting are now handled by the "Next" button in OnboardingScreen
    }
}


@Composable
fun ProfileSetupPage(
    // profileViewModel: ProfileViewModel, // Keep if needed for direct VM interactions within this page
    name: String,
    onNameChange: (String) -> Unit,
    bloodType: String,
    onBloodTypeChange: (String) -> Unit
    // Add other profile fields and their respective onValueChange lambdas here
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()), // Make content scrollable if it overflows
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Set Up Your Profile (Optional)",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Providing some basic profile information can help personalize your experience. You can skip this step and update your profile later.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Full Name (Optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = bloodType,
            onValueChange = onBloodTypeChange,
            label = { Text("Blood Type (e.g., A+, O-)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        // TODO: Add more OutlinedTextFields for other profile information
        // e.g., Allergies, Chronic Conditions, Emergency Contact

        Spacer(modifier = Modifier.height(32.dp))

        // The "Finish" button is handled by the main OnboardingScreen's navigation logic.
        // This page just collects the data. Saving will be triggered by the main "Finish" button.
        // We can add a specific "Save Profile" action here if needed before onFinish,
        // or rely on the main button to call profileViewModel.saveProfile(...)

        // The Skip button is implicitly handled by the "Finish" button if fields are empty,
        // or we can add an explicit "Skip" button that directly calls onFinish().
        // For simplicity, the main "Finish" button will complete onboarding.
        // If the user doesn't fill anything, nothing is saved for the profile.
    }
}
