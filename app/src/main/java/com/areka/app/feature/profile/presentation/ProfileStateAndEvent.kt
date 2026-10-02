package com.areka.app.feature.profile.presentation

import com.areka.app.core.sync.SyncStatus
import com.areka.app.data.local.MistakeEntity
import com.areka.app.data.model.UserProfile
import com.areka.app.data.remote.AuthState

data class ProfileUiState(
    val isLoading: Boolean = false,
    val profile: UserProfile = UserProfile(),
    val isDarkTheme: Boolean = false,
    val authState: AuthState = AuthState.SignedOut,
    val syncStatus: SyncStatus = SyncStatus.OFFLINE,
    val lastSyncMessage: String? = null,
    val isSyncingNow: Boolean = false,
    // Auth Form State
    val authEmail: String = "",
    val authPassword: String = "",
    val authConfirmPassword: String = "",
    val authDisplayName: String = "",
    val isSignUpMode: Boolean = false,
    val isForgotPasswordMode: Boolean = false,
    val newPasswordInput: String = "",
    val authActionError: String? = null,
    val authActionSuccess: String? = null
)

sealed interface ProfileEvent {
    data class UpdateDisplayName(val name: String) : ProfileEvent
    data class UpdateGrade(val grade: String) : ProfileEvent
    data object ToggleTheme : ProfileEvent
    data object ManualSync : ProfileEvent
    data class EmailChanged(val email: String) : ProfileEvent
    data class PasswordChanged(val pass: String) : ProfileEvent
    data class ConfirmPasswordChanged(val pass: String) : ProfileEvent
    data class DisplayNameChanged(val name: String) : ProfileEvent
    data class NewPasswordInputChanged(val pass: String) : ProfileEvent
    data class SetSignUpMode(val isSignUp: Boolean) : ProfileEvent
    data class SetForgotPasswordMode(val isForgot: Boolean) : ProfileEvent
    data object SubmitAuth : ProfileEvent
    data object SendRecoveryEmail : ProfileEvent
    data object UpdatePasswordRecovery : ProfileEvent
    data object SignOut : ProfileEvent
}
