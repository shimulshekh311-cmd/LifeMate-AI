package com.example.core.security

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.core.i18n.I18n

enum class BiometricCapabilityStatus {
    READY,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    NONE_ENROLLED,
    SECURITY_UPDATE_REQUIRED,
    UNSUPPORTED
}

sealed class BiometricAuthResult {
    object Success : BiometricAuthResult()
    object Cancelled : BiometricAuthResult()
    data class Error(val errorCode: Int, val message: String) : BiometricAuthResult()
    data class Unavailable(val status: BiometricCapabilityStatus, val message: String) : BiometricAuthResult()
}

interface BiometricAuthenticator {
    fun checkCapability(context: Context): BiometricCapabilityStatus
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onResult: (BiometricAuthResult) -> Unit
    )
}

class SystemBiometricAuthenticator : BiometricAuthenticator {

    private fun getAllowedAuthenticators(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }
    }

    override fun checkCapability(context: Context): BiometricCapabilityStatus {
        val biometricManager = BiometricManager.from(context)
        val authenticators = getAllowedAuthenticators()
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricCapabilityStatus.READY
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricCapabilityStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricCapabilityStatus.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricCapabilityStatus.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricCapabilityStatus.SECURITY_UPDATE_REQUIRED
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricCapabilityStatus.UNSUPPORTED
            else -> BiometricCapabilityStatus.UNSUPPORTED
        }
    }

    override fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onResult: (BiometricAuthResult) -> Unit
    ) {
        val status = checkCapability(activity)
        if (status != BiometricCapabilityStatus.READY) {
            val msg = BiometricAuthManager.getCapabilityMessage(status)
            onResult(BiometricAuthResult.Unavailable(status, msg))
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val promptCallback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(BiometricAuthResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_CANCELED -> {
                        onResult(BiometricAuthResult.Cancelled)
                    }
                    BiometricPrompt.ERROR_LOCKOUT,
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                        onResult(
                            BiometricAuthResult.Error(
                                errorCode = errorCode,
                                message = I18n.t("biometric_auth_lockout")
                            )
                        )
                    }
                    else -> {
                        val message = if (errString.isNotBlank()) errString.toString() else I18n.t("biometric_auth_failed")
                        onResult(BiometricAuthResult.Error(errorCode, message))
                    }
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onResult(BiometricAuthResult.Error(errorCode = -1, message = I18n.t("biometric_auth_failed")))
            }
        }

        try {
            val biometricPrompt = BiometricPrompt(activity, executor, promptCallback)
            val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                promptInfoBuilder.setAllowedAuthenticators(getAllowedAuthenticators())
            } else {
                @Suppress("DEPRECATION")
                promptInfoBuilder.setDeviceCredentialAllowed(true)
            }

            biometricPrompt.authenticate(promptInfoBuilder.build())
        } catch (e: Exception) {
            onResult(
                BiometricAuthResult.Error(
                    errorCode = -2,
                    message = e.localizedMessage ?: I18n.t("biometric_auth_failed")
                )
            )
        }
    }
}

object BiometricAuthManager {
    var authenticator: BiometricAuthenticator = SystemBiometricAuthenticator()

    fun checkCapability(context: Context): BiometricCapabilityStatus {
        return authenticator.checkCapability(context)
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = I18n.t("biometric_prompt_title"),
        subtitle: String = I18n.t("biometric_prompt_subtitle"),
        onResult: (BiometricAuthResult) -> Unit
    ) {
        authenticator.authenticate(activity, title, subtitle, onResult)
    }

    fun getCapabilityMessage(status: BiometricCapabilityStatus): String {
        return when (status) {
            BiometricCapabilityStatus.READY -> I18n.t("biometric_auth_success")
            BiometricCapabilityStatus.NO_HARDWARE -> I18n.t("biometric_no_hardware")
            BiometricCapabilityStatus.HARDWARE_UNAVAILABLE -> I18n.t("biometric_hw_unavailable")
            BiometricCapabilityStatus.NONE_ENROLLED -> I18n.t("biometric_not_enrolled")
            BiometricCapabilityStatus.SECURITY_UPDATE_REQUIRED -> I18n.t("biometric_hw_unavailable")
            BiometricCapabilityStatus.UNSUPPORTED -> I18n.t("biometric_no_hardware")
        }
    }
}
