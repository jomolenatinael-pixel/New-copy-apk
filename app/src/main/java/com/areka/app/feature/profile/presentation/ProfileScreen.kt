package com.areka.app.feature.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.areka.app.core.designsystem.*
import com.areka.app.core.sync.SyncStatus
import com.areka.app.data.remote.AuthState

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onEvent: (ProfileEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var editName by remember(uiState.profile.name) { mutableStateOf(uiState.profile.name) }
    var editGrade by remember(uiState.profile.grade) { mutableStateOf(uiState.profile.grade) }
    var isEditingProfile by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("profile_screen"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "you_header") {
            Column {
                Text(
                    text = "You & Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Manage your learner profile, cloud synchronization, and preferences",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.textMuted
                )
            }
        }

        // 1. Learner Profile Card
        item(key = "profile_identity_card") {
            ArekaV2Card(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (uiState.profile.name.firstOrNull() ?: 'S').uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.profile.name.ifBlank { "Student" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = uiState.profile.grade,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.textMuted
                        )
                    }

                    IconButton(onClick = { isEditingProfile = !isEditingProfile }) {
                        Icon(
                            imageVector = if (isEditingProfile) Icons.Default.Close else Icons.Outlined.Edit,
                            contentDescription = "Edit Profile",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (isEditingProfile) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Display Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editGrade,
                        onValueChange = { editGrade = it },
                        label = { Text("Grade Level") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_grade_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            onEvent(ProfileEvent.UpdateDisplayName(editName))
                            onEvent(ProfileEvent.UpdateGrade(editGrade))
                            isEditingProfile = false
                        },
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("save_profile_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }

        // 2. Study Preferences & Theme
        item(key = "preferences_card") {
            ArekaV2Card(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                SectionHeader(
                    title = "App Preferences",
                    subtitle = "Customize your study experience"
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Dark Theme",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (uiState.isDarkTheme) "High-contrast dark mode enabled" else "Warm light mode enabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.textMuted
                            )
                        }
                    }

                    Switch(
                        checked = uiState.isDarkTheme,
                        onCheckedChange = { onEvent(ProfileEvent.ToggleTheme) },
                        modifier = Modifier.testTag("theme_toggle_switch")
                    )
                }
            }
        }

        // 3. Cloud Sync & Isolation
        item(key = "sync_card") {
            val syncColor = when (uiState.syncStatus) {
                SyncStatus.SYNCED -> MaterialTheme.colorScheme.success
                SyncStatus.SYNCING -> MaterialTheme.colorScheme.primary
                SyncStatus.OFFLINE -> MaterialTheme.colorScheme.textMuted
                SyncStatus.SYNC_FAILED -> MaterialTheme.colorScheme.error
            }

            val syncLabel = when (uiState.syncStatus) {
                SyncStatus.SYNCED -> "Synced"
                SyncStatus.SYNCING -> "Syncing..."
                SyncStatus.OFFLINE -> "Offline Mode"
                SyncStatus.SYNC_FAILED -> "Sync Issue"
            }

            ArekaV2Card(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(syncColor)
                            )
                            Text(
                                text = "Cloud Synchronization: $syncLabel",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.lastSyncMessage ?: "Local Room database holds persistent offline state.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textMuted
                        )
                    }

                    OutlinedButton(
                        onClick = { onEvent(ProfileEvent.ManualSync) },
                        enabled = !uiState.isSyncingNow,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("sync_now_button")
                    ) {
                        if (uiState.isSyncingNow) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Sync Now")
                        }
                    }
                }
            }
        }

        // 4. Account & Security (Supabase Authentication)
        item(key = "account_security_card") {
            ArekaV2Card(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(18.dp)
            ) {
                when (val auth = uiState.authState) {
                    is AuthState.SignedIn -> {
                        SectionHeader(
                            title = "Account & Cloud Identity",
                            subtitle = "Signed in as verified student"
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceSubtle, RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = auth.user.email,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (auth.user.isAdmin) "Role: Admin" else "Role: Grade 10 Student",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.textMuted
                                )
                            }

                            Button(
                                onClick = { onEvent(ProfileEvent.SignOut) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("sign_out_button")
                            ) {
                                Text("Sign Out")
                            }
                        }
                    }

                    is AuthState.PasswordRecovery -> {
                        SectionHeader(
                            title = "Set New Password",
                            subtitle = "Recovery link confirmed. Choose a new secure password."
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = uiState.newPasswordInput,
                            onValueChange = { onEvent(ProfileEvent.NewPasswordInputChanged(it)) },
                            label = { Text("New Password (min 6 characters)") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new_password_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onEvent(ProfileEvent.UpdatePasswordRecovery) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("update_password_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Update Password")
                        }
                    }

                    else -> {
                        // Signed Out state: Sign in / Create Account Form
                        SectionHeader(
                            title = if (uiState.isForgotPasswordMode) "Recover Password"
                            else if (uiState.isSignUpMode) "Create Account" else "Sign In",
                            subtitle = if (uiState.isForgotPasswordMode) "Enter email to receive reset link"
                            else if (uiState.isSignUpMode) "Sync progress and join global leaderboard" else "Access your saved cloud profile"
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = uiState.authEmail,
                            onValueChange = { onEvent(ProfileEvent.EmailChanged(it)) },
                            label = { Text("Email Address") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_email_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        if (!uiState.isForgotPasswordMode) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = uiState.authPassword,
                                onValueChange = { onEvent(ProfileEvent.PasswordChanged(it)) },
                                label = { Text("Password") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_password_input"),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            if (uiState.isSignUpMode) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = uiState.authConfirmPassword,
                                    onValueChange = { onEvent(ProfileEvent.ConfirmPasswordChanged(it)) },
                                    label = { Text("Confirm Password") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_confirm_password_input"),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = uiState.authDisplayName,
                                    onValueChange = { onEvent(ProfileEvent.DisplayNameChanged(it)) },
                                    label = { Text("Display Name") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_display_name_input"),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                            }
                        }

                        if (uiState.authActionError != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = uiState.authActionError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        if (uiState.authActionSuccess != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = uiState.authActionSuccess,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.success
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (uiState.isForgotPasswordMode) {
                            Button(
                                onClick = { onEvent(ProfileEvent.SendRecoveryEmail) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("send_recovery_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Send Recovery Link")
                            }
                            TextButton(
                                onClick = { onEvent(ProfileEvent.SetForgotPasswordMode(false)) },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Back to Sign In")
                            }
                        } else {
                            Button(
                                onClick = { onEvent(ProfileEvent.SubmitAuth) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_submit_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (uiState.isSignUpMode) "Create Account" else "Sign In")
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(
                                    onClick = { onEvent(ProfileEvent.SetSignUpMode(!uiState.isSignUpMode)) },
                                    modifier = Modifier.testTag("toggle_signup_mode_button")
                                ) {
                                    Text(if (uiState.isSignUpMode) "Already have an account? Sign In" else "New here? Create Account")
                                }

                                if (!uiState.isSignUpMode) {
                                    TextButton(onClick = { onEvent(ProfileEvent.SetForgotPasswordMode(true)) }) {
                                        Text("Forgot?")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
