package com.example.modumessenger.feature.lock

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * 생체 인식은 PIN 의 지름길일 뿐이라 CryptoObject 바인딩 없이 성공 콜백만 받는다.
 * 취소·실패·하드웨어 오류는 전부 [onDismiss] 로 모아 PIN 입력으로 돌아가게 한다.
 */
object Biometrics {

    private const val AUTHENTICATORS = BIOMETRIC_STRONG or BIOMETRIC_WEAK

    /** 하드웨어가 있고 지문이나 얼굴이 등록돼 있는지. */
    fun isAvailable(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    fun prompt(
        activity: FragmentActivity,
        title: String,
        negativeText: String,
        onSuccess: () -> Unit,
        onDismiss: () -> Unit,
    ) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onDismiss()
            // 한 번 안 맞은 것은 프롬프트가 알아서 다시 받는다. 끝난 게 아니다.
            override fun onAuthenticationFailed() = Unit
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setNegativeButtonText(negativeText)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .setConfirmationRequired(false)
            .build()
        BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback).authenticate(info)
    }
}

/** Compose 의 `LocalContext` 는 보통 액티비티지만 테마 래퍼일 수 있어 벗겨 가며 찾는다. */
fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return null
}
