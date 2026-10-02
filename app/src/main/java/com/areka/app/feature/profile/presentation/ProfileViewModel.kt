package com.areka.app.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.areka.app.core.repository.IProfileRepository
import com.areka.app.core.sync.SyncRepository
import com.areka.app.core.sync.SyncStatus
import com.areka.app.data.remote.AuthState
import com.areka.app.data.remote.SupabaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileRepository: IProfileRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        observeRepositories()
    }

    private fun observeRepositories() {
        viewModelScope.launch {
            profileRepository.userProfile.collect { profile ->
                _uiState.update { it.copy(profile = profile) }
            }
        }
        viewModelScope.launch {
            profileRepository.isDarkTheme.collect { isDark ->
                _uiState.update { it.copy(isDarkTheme = isDark) }
            }
        }
        viewModelScope.launch {
            SupabaseAuth.state.collect { auth ->
                _uiState.update { it.copy(authState = auth) }
                if (auth is AuthState.SignedIn) {
                    syncRepository.syncAll(profileRepository.userProfile.value)
                }
            }
        }
        viewModelScope.launch {
            syncRepository.syncStatus.collect { status ->
                _uiState.update { it.copy(syncStatus = status) }
            }
        }
        viewModelScope.launch {
            syncRepository.lastSyncMessage.collect { msg ->
                _uiState.update { it.copy(lastSyncMessage = msg) }
            }
        }
    }

    fun updateProfile(name: String, grade: String) {
        profileRepository.updateProfile(name, grade)
    }

    fun toggleTheme() {
        profileRepository.toggleTheme()
    }

    fun triggerManualSync() {
        _uiState.update { it.copy(isSyncingNow = true) }
        viewModelScope.launch {
            syncRepository.syncAll(profileRepository.userProfile.value)
            _uiState.update { it.copy(isSyncingNow = false) }
        }
    }

    fun setEmail(email: String) {
        _uiState.update { it.copy(authEmail = email, authActionError = null) }
    }

    fun setPassword(password: String) {
        _uiState.update { it.copy(authPassword = password, authActionError = null) }
    }

    fun setConfirmPassword(confirm: String) {
        _uiState.update { it.copy(authConfirmPassword = confirm, authActionError = null) }
    }

    fun setDisplayName(name: String) {
        _uiState.update { it.copy(authDisplayName = name, authActionError = null) }
    }

    fun setNewPasswordInput(pass: String) {
        _uiState.update { it.copy(newPasswordInput = pass, authActionError = null) }
    }

    fun setSignUpMode(isSignUp: Boolean) {
        _uiState.update { it.copy(isSignUpMode = isSignUp, isForgotPasswordMode = false, authActionError = null, authActionSuccess = null) }
    }

    fun setForgotPasswordMode(isForgot: Boolean) {
        _uiState.update { it.copy(isForgotPasswordMode = isForgot, authActionError = null, authActionSuccess = null) }
    }

    fun submitAuth() {
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true, authActionError = null, authActionSuccess = null) }

        viewModelScope.launch {
            if (state.isSignUpMode) {
                val res = SupabaseAuth.signUp(
                    email = state.authEmail,
                    password = state.authPassword,
                    confirmation = state.authConfirmPassword,
                    displayName = state.authDisplayName.ifBlank { state.profile.name }
                )
                if (res.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            authActionSuccess = "Account created! If confirmation is enabled, check your email.",
                            authPassword = "",
                            authConfirmPassword = ""
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            authActionError = res.exceptionOrNull()?.message ?: "Sign up failed"
                        )
                    }
                }
            } else {
                val res = SupabaseAuth.signIn(state.authEmail, state.authPassword)
                if (res.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            authPassword = "",
                            authActionSuccess = "Signed in successfully"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            authActionError = res.exceptionOrNull()?.message ?: "Sign in failed"
                        )
                    }
                }
            }
        }
    }

    fun sendRecoveryEmail() {
        val email = _uiState.value.authEmail.trim()
        if (email.isBlank()) {
            _uiState.update { it.copy(authActionError = "Please enter your email address") }
            return
        }
        _uiState.update { it.copy(isLoading = true, authActionError = null) }
        viewModelScope.launch {
            val res = SupabaseAuth.sendPasswordReset(email)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    authActionSuccess = if (res.isSuccess) "Recovery link sent to $email" else null,
                    authActionError = res.exceptionOrNull()?.message
                )
            }
        }
    }

    fun updatePasswordRecovery() {
        val newPass = _uiState.value.newPasswordInput.trim()
        if (newPass.length < 6) {
            _uiState.update { it.copy(authActionError = "Password must be at least 6 characters") }
            return
        }
        _uiState.update { it.copy(isLoading = true, authActionError = null) }
        viewModelScope.launch {
            val res = SupabaseAuth.updatePassword(newPass, newPass)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    newPasswordInput = "",
                    authActionSuccess = if (res.isSuccess) "Password successfully updated!" else null,
                    authActionError = res.exceptionOrNull()?.message
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            SupabaseAuth.signOut()
        }
    }

    companion object {
        fun provideFactory(
            profileRepository: IProfileRepository,
            syncRepository: SyncRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(profileRepository, syncRepository) as T
            }
        }
    }
}
