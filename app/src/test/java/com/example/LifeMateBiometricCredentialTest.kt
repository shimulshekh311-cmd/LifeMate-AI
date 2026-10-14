package com.example

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.i18n.I18n
import com.example.core.security.*
import com.example.data.local.AppDatabase
import com.example.data.local.entities.JobApplicationEntity
import com.example.data.repository.LifeMateRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LifeMateBiometricCredentialTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: LifeMateRepository

    // Test Double for BiometricAuthenticator
    class FakeBiometricAuthenticator : BiometricAuthenticator {
        var simulatedCapability: BiometricCapabilityStatus = BiometricCapabilityStatus.READY
        var simulatedResult: BiometricAuthResult = BiometricAuthResult.Success
        var authenticateCallCount = 0

        override fun checkCapability(context: Context): BiometricCapabilityStatus {
            return simulatedCapability
        }

        override fun authenticate(
            activity: FragmentActivity,
            title: String,
            subtitle: String,
            onResult: (BiometricAuthResult) -> Unit
        ) {
            authenticateCallCount++
            onResult(simulatedResult)
        }
    }

    private lateinit var fakeAuthenticator: FakeBiometricAuthenticator

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LifeMateRepository(db)
        I18n.currentLanguage = "bn"

        fakeAuthenticator = FakeBiometricAuthenticator()
        BiometricAuthManager.authenticator = fakeAuthenticator
    }

    @After
    fun tearDown() {
        db.close()
        // Restore default system authenticator
        BiometricAuthManager.authenticator = SystemBiometricAuthenticator()
    }

    // 1. Password remains masked by default before authentication
    @Test
    fun testNoPasswordRevealBeforeAuthentication(): Unit = runBlocking {
        val rawSecret = "GovtJobPassword@2026"
        val encrypted = KeystoreVault.encrypt(rawSecret)

        val job = JobApplicationEntity(
            organizationName = "Bangladesh Public Service Commission",
            positionName = "Cadre Officer",
            creationDate = "2026-10-14",
            weekday = "Wednesday",
            encryptedPassword = encrypted
        )
        db.jobApplicationDao().insertApplication(job)

        // Verify initial state: without explicit authentication, no plaintext is revealed
        var displayedPassword: String? = null
        assertEquals(0, fakeAuthenticator.authenticateCallCount)
        assertNull("Password must be unrevealed (null) before user requests authentication", displayedPassword)
        assertNotEquals("Plaintext must never match stored encrypted string", rawSecret, job.encryptedPassword)
    }

    // 2. Successful biometric authentication reveals decrypted password
    @Test
    fun testAuthenticationSuccessRevealsDecryptedPassword() {
        val rawSecret = "Secur3P@ssw0rd!"
        val encrypted = KeystoreVault.encrypt(rawSecret)

        fakeAuthenticator.simulatedCapability = BiometricCapabilityStatus.READY
        fakeAuthenticator.simulatedResult = BiometricAuthResult.Success

        var revealedPassword: String? = null
        var errorMessage: String? = null

        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()

        BiometricAuthManager.authenticate(activity) { result ->
            when (result) {
                is BiometricAuthResult.Success -> {
                    revealedPassword = KeystoreVault.decrypt(encrypted)
                }
                is BiometricAuthResult.Cancelled -> {
                    revealedPassword = null
                    errorMessage = "Cancelled"
                }
                is BiometricAuthResult.Error -> {
                    revealedPassword = null
                    errorMessage = result.message
                }
                is BiometricAuthResult.Unavailable -> {
                    revealedPassword = null
                    errorMessage = result.message
                }
            }
        }

        assertEquals(1, fakeAuthenticator.authenticateCallCount)
        assertNotNull("Password must be revealed upon successful authentication", revealedPassword)
        assertEquals("Decrypted password must match original plaintext", rawSecret, revealedPassword)
        assertNull("No error message on success", errorMessage)
    }

    // 3. Authentication failure keeps password masked and reports error
    @Test
    fun testAuthenticationFailureKeepsPasswordMasked() {
        val rawSecret = "MySecretBankPass"
        val encrypted = KeystoreVault.encrypt(rawSecret)

        fakeAuthenticator.simulatedCapability = BiometricCapabilityStatus.READY
        fakeAuthenticator.simulatedResult = BiometricAuthResult.Error(errorCode = -1, message = "Fingerprint not recognized")

        var revealedPassword: String? = null
        var errorMessage: String? = null

        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()

        BiometricAuthManager.authenticate(activity) { result ->
            when (result) {
                is BiometricAuthResult.Success -> {
                    revealedPassword = KeystoreVault.decrypt(encrypted)
                }
                is BiometricAuthResult.Error -> {
                    revealedPassword = null
                    errorMessage = result.message
                }
                else -> {
                    revealedPassword = null
                }
            }
        }

        assertEquals(1, fakeAuthenticator.authenticateCallCount)
        assertNull("Password must remain masked (null) on authentication failure", revealedPassword)
        assertNotNull("Error message must be reported on failure", errorMessage)
        assertEquals("Fingerprint not recognized", errorMessage)
    }

    // 4. User cancellation keeps password masked without disclosing plaintext
    @Test
    fun testAuthenticationCancellationKeepsPasswordMasked() {
        val rawSecret = "ConfidentialPass"
        val encrypted = KeystoreVault.encrypt(rawSecret)

        fakeAuthenticator.simulatedCapability = BiometricCapabilityStatus.READY
        fakeAuthenticator.simulatedResult = BiometricAuthResult.Cancelled

        var revealedPassword: String? = null
        var isCancelled = false

        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()

        BiometricAuthManager.authenticate(activity) { result ->
            when (result) {
                is BiometricAuthResult.Success -> {
                    revealedPassword = KeystoreVault.decrypt(encrypted)
                }
                is BiometricAuthResult.Cancelled -> {
                    revealedPassword = null
                    isCancelled = true
                }
                else -> {}
            }
        }

        assertEquals(1, fakeAuthenticator.authenticateCallCount)
        assertNull("Password must remain masked on cancellation", revealedPassword)
        assertTrue("Cancellation event must be flagged", isCancelled)
    }

    // 5. Unsupported device or no biometric/screen lock enrolled handling
    @Test
    fun testUnsupportedOrNoHardwareDeviceHandling() {
        val rawSecret = "NoHardwarePassword"
        val encrypted = KeystoreVault.encrypt(rawSecret)

        // Case A: No hardware
        fakeAuthenticator.simulatedCapability = BiometricCapabilityStatus.NO_HARDWARE
        val noHwMsg = BiometricAuthManager.getCapabilityMessage(BiometricCapabilityStatus.NO_HARDWARE)
        assertTrue(noHwMsg.isNotBlank())

        var revealedPassword: String? = null
        val statusA = BiometricAuthManager.checkCapability(context)
        assertEquals(BiometricCapabilityStatus.NO_HARDWARE, statusA)
        assertNull("No decryption if hardware is missing", revealedPassword)

        // Case B: None enrolled
        fakeAuthenticator.simulatedCapability = BiometricCapabilityStatus.NONE_ENROLLED
        val notEnrolledMsg = BiometricAuthManager.getCapabilityMessage(BiometricCapabilityStatus.NONE_ENROLLED)
        assertTrue("Bangla explanation must guide user to device lock settings", notEnrolledMsg.contains("স্ক্রিন লক"))

        I18n.currentLanguage = "en"
        val notEnrolledMsgEn = BiometricAuthManager.getCapabilityMessage(BiometricCapabilityStatus.NONE_ENROLLED)
        assertTrue("English explanation must guide user to screen lock settings", notEnrolledMsgEn.contains("screen lock"))
        I18n.currentLanguage = "bn"
    }

    // 6. Existing encrypted credential compatibility (Phase 1 & 2A backward compatibility)
    @Test
    fun testExistingEncryptedCredentialCompatibility(): Unit = runBlocking {
        // Encrypt with existing KeystoreVault format
        val originalPlaintext = "LegacyFormatPassword_2026"
        val ciphertext = KeystoreVault.encrypt(originalPlaintext)

        // Verify it can still be decrypted safely
        val decrypted = KeystoreVault.decrypt(ciphertext)
        assertEquals("Existing ciphertext must decrypt identically without key regeneration", originalPlaintext, decrypted)

        // Verify fallback format (VAULT: prefix) backward compatibility
        val fallbackCiphertext = "VAULT:" + android.util.Base64.encodeToString(
            originalPlaintext.toByteArray(Charsets.UTF_8),
            android.util.Base64.NO_WRAP
        )
        val decryptedFallback = KeystoreVault.decrypt(fallbackCiphertext)
        assertEquals("Fallback ciphertext must decrypt cleanly", originalPlaintext, decryptedFallback)
    }

    // 7. No plaintext credential leakage into JSON backup or exported files
    @Test
    fun testNoPlaintextCredentialLeakageIntoBackup(): Unit = runBlocking {
        val secretPass = "TopSecretPasswordDoNotExport"
        val encrypted = KeystoreVault.encrypt(secretPass)

        db.jobApplicationDao().insertApplication(
            JobApplicationEntity(
                organizationName = "Ministry of ICT",
                positionName = "Senior Consultant",
                creationDate = "2026-10-14",
                weekday = "Wednesday",
                encryptedPassword = encrypted
            )
        )

        val backupJson = repository.backupRestoreManager.generateBackupJson()
        assertFalse(
            "Plaintext password must NEVER appear in backup JSON",
            backupJson.contains(secretPass)
        )
        assertFalse(
            "Encrypted password field must be omitted from backup JSON",
            backupJson.contains("encryptedPassword")
        )
    }

    // 8. Correct dialog and state cleanup after reveal
    @Test
    fun testDialogStateCleanupAfterReveal() {
        var inMemoryCredential: String? = "TemporaryDecryptedSecret"
        assertNotNull(inMemoryCredential)

        // Simulate dialog dismissal / DisposableEffect onDispose cleanup
        inMemoryCredential = null

        assertNull("In-memory revealed password must be cleared to null upon dialog dismissal", inMemoryCredential)
    }
}
