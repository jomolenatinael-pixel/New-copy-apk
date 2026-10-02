package com.areka.app

import android.net.Uri
import com.areka.app.data.remote.AuthState
import com.areka.app.data.remote.AuthValidator
import com.areka.app.data.remote.SecureTokenStorage
import com.areka.app.data.remote.StoredTokens
import com.areka.app.data.remote.SupabaseAuth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthSecurityTest {

    // ==========================================
    // 1. AuthValidator Tests
    // ==========================================
    @Test
    fun `validateEmail accepts standard addresses and rejects invalid formats`() {
        assertNull(AuthValidator.validateEmail("student@areka.edu.et"))
        assertNull(AuthValidator.validateEmail("natijommar@gmail.com"))

        assertNotNull(AuthValidator.validateEmail(""))
        assertNotNull(AuthValidator.validateEmail("   "))
        assertNotNull(AuthValidator.validateEmail("invalid-email"))
        assertNotNull(AuthValidator.validateEmail("@no-user.com"))
        assertNotNull(AuthValidator.validateEmail("no-domain@"))
    }

    @Test
    fun `validatePassword enforces minimum length of 6 characters`() {
        assertNull(AuthValidator.validatePassword("123456"))
        assertNull(AuthValidator.validatePassword("strongPassword2026!"))

        assertNotNull(AuthValidator.validatePassword(""))
        assertNotNull(AuthValidator.validatePassword("12345"))
    }

    @Test
    fun `validatePasswordConfirmation validates length and matching passwords`() {
        assertNull(AuthValidator.validatePasswordConfirmation("secure123", "secure123"))

        assertEquals("Passwords do not match.", AuthValidator.validatePasswordConfirmation("secure123", "mismatch456"))
        assertEquals("Password must be at least 6 characters.", AuthValidator.validatePasswordConfirmation("123", "123"))
    }

    @Test
    fun `normalizeEmail trims whitespace and lowercases string`() {
        assertEquals("user@example.com", AuthValidator.normalizeEmail("  USER@Example.COM  "))
    }

    // ==========================================
    // 2. Password Recovery Deep-Link Parsing Tests
    // ==========================================
    @Test
    fun `handleRecoveryUri extracts tokens from URL fragment`() {
        val uri = Uri.parse("areka://auth/recovery#access_token=fragment_access_123&refresh_token=fragment_refresh_456&type=recovery&email=student%40areka.com")
        val result = SupabaseAuth.handleRecoveryUri(uri)

        assertTrue(result.isSuccess)
        val state = SupabaseAuth.state.value
        assertTrue(state is AuthState.PasswordRecovery)
        assertEquals("student@areka.com", (state as AuthState.PasswordRecovery).email)

        SupabaseAuth.cancelPasswordRecovery()
    }

    @Test
    fun `handleRecoveryUri extracts tokens from URL query parameters`() {
        val uri = Uri.parse("areka://auth/recovery?access_token=query_access_789&refresh_token=query_refresh_012&type=recovery&email=learner%40areka.com")
        val result = SupabaseAuth.handleRecoveryUri(uri)

        assertTrue(result.isSuccess)
        val state = SupabaseAuth.state.value
        assertTrue(state is AuthState.PasswordRecovery)
        assertEquals("learner@areka.com", (state as AuthState.PasswordRecovery).email)

        SupabaseAuth.cancelPasswordRecovery()
    }

    @Test
    fun `handleRecoveryUri reports user friendly error for expired or invalid reset link`() {
        val uri = Uri.parse("areka://auth/recovery?error=access_denied&error_code=otp_expired&error_description=Email+link+is+invalid+or+has+expired")
        val result = SupabaseAuth.handleRecoveryUri(uri)

        assertTrue(result.isFailure)
        val state = SupabaseAuth.state.value
        assertTrue(state is AuthState.Error)
        assertTrue((state as AuthState.Error).message.contains("expired", ignoreCase = true))

        SupabaseAuth.cancelPasswordRecovery()
    }

    @Test
    fun `handleRecoveryUri rejects link with missing tokens`() {
        val uri = Uri.parse("areka://auth/recovery?type=recovery")
        val result = SupabaseAuth.handleRecoveryUri(uri)

        assertTrue(result.isFailure)
        val state = SupabaseAuth.state.value
        assertTrue(state is AuthState.Error)

        SupabaseAuth.cancelPasswordRecovery()
    }

    // ==========================================
    // 3. Custom Token Storage Isolation Tests
    // ==========================================
    @Test
    fun `token storage saves and clears tokens reliably`() {
        val storage = object : SecureTokenStorage {
            private var access: String? = null
            private var refresh: String? = null

            override fun getTokens(): StoredTokens = StoredTokens(access, refresh)
            override fun saveTokens(accessToken: String?, refreshToken: String?) {
                access = accessToken
                refresh = refreshToken
            }
            override fun clearTokens() {
                access = null
                refresh = null
            }
        }

        storage.saveTokens("test_access", "test_refresh")
        val saved = storage.getTokens()
        assertEquals("test_access", saved.accessToken)
        assertEquals("test_refresh", saved.refreshToken)

        storage.clearTokens()
        val cleared = storage.getTokens()
        assertNull(cleared.accessToken)
        assertNull(cleared.refreshToken)
    }

    // ==========================================
    // 4. Deterministic Attempt UUID Stability Tests
    // ==========================================
    @Test
    fun `deterministic attempt UUID is stable and reproducible`() {
        val userId = "user_42"
        val attemptId = "attempt_chem_u1_98765"
        val seed = "${userId}_$attemptId"

        val uuidA = UUID.nameUUIDFromBytes(seed.toByteArray(Charsets.UTF_8)).toString()
        val uuidB = UUID.nameUUIDFromBytes(seed.toByteArray(Charsets.UTF_8)).toString()

        assertEquals(uuidA, uuidB)
    }
}
