package pl.fzar.dokumed.data.repository


import android.content.SharedPreferences
import kotlinx.serialization.json.Json
import pl.fzar.dokumed.ui.profile.ProfileScreenState

interface ProfileRepository {
    fun getProfileData(): ProfileScreenState
    fun saveProfileData(profileState: ProfileScreenState)
}

class ProfileRepositoryImpl(
    private val sharedPreferences: SharedPreferences
) : ProfileRepository {

    companion object {
        private const val KEY_PROFILE_DATA = "profile_data_json"
    }

    override fun getProfileData(): ProfileScreenState {
        val json = sharedPreferences.getString(KEY_PROFILE_DATA, null)
        return if (json != null) {
            Json.decodeFromString<ProfileScreenState>(json)
        } else {
            ProfileScreenState() // Return default state if nothing is saved
        }
    }

    override fun saveProfileData(profileState: ProfileScreenState) {
        val json = Json.encodeToString(profileState)
        with(sharedPreferences.edit()) {
            putString(KEY_PROFILE_DATA, json)
            apply()
        }
    }
}
