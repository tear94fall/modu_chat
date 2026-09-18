package com.example.modumessenger.core.lock

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** 저장되는 전부. 실패 횟수와 대기 시각까지 저장해 앱을 껐다 켜도 대기가 유지된다. */
data class LockData(
    val enabled: Boolean = false,
    val graceMillis: Long = 0L,
    val biometricEnabled: Boolean = false,
    val pinHash: String? = null,
    val pinSalt: String? = null,
    val failedCount: Int = 0,
    val lockedUntil: Long = 0L,
)

/** [AppLock] 이 쓰는 저장소. 테스트는 메모리 구현으로 바꾼다. */
interface LockStorage {
    val data: Flow<LockData>
    suspend fun update(transform: (LockData) -> LockData)
}

private val Context.lockDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_lock")

@Singleton
class DataStoreLockStorage @Inject constructor(
    @ApplicationContext context: Context,
) : LockStorage {

    private val dataStore = context.lockDataStore

    override val data: Flow<LockData> = dataStore.data.map { prefs ->
        LockData(
            enabled = prefs[KEY_ENABLED] ?: false,
            graceMillis = prefs[KEY_GRACE] ?: 0L,
            biometricEnabled = prefs[KEY_BIOMETRIC] ?: false,
            pinHash = prefs[KEY_PIN_HASH],
            pinSalt = prefs[KEY_PIN_SALT],
            failedCount = prefs[KEY_FAILED] ?: 0,
            lockedUntil = prefs[KEY_LOCKED_UNTIL] ?: 0L,
        )
    }

    override suspend fun update(transform: (LockData) -> LockData) {
        dataStore.edit { prefs ->
            val current = LockData(
                enabled = prefs[KEY_ENABLED] ?: false,
                graceMillis = prefs[KEY_GRACE] ?: 0L,
                biometricEnabled = prefs[KEY_BIOMETRIC] ?: false,
                pinHash = prefs[KEY_PIN_HASH],
                pinSalt = prefs[KEY_PIN_SALT],
                failedCount = prefs[KEY_FAILED] ?: 0,
                lockedUntil = prefs[KEY_LOCKED_UNTIL] ?: 0L,
            )
            val next = transform(current)
            prefs[KEY_ENABLED] = next.enabled
            prefs[KEY_GRACE] = next.graceMillis
            prefs[KEY_BIOMETRIC] = next.biometricEnabled
            if (next.pinHash == null) prefs.remove(KEY_PIN_HASH) else prefs[KEY_PIN_HASH] = next.pinHash
            if (next.pinSalt == null) prefs.remove(KEY_PIN_SALT) else prefs[KEY_PIN_SALT] = next.pinSalt
            prefs[KEY_FAILED] = next.failedCount
            prefs[KEY_LOCKED_UNTIL] = next.lockedUntil
        }
    }

    private companion object {
        val KEY_ENABLED = booleanPreferencesKey("enabled")
        val KEY_GRACE = longPreferencesKey("grace_ms")
        val KEY_BIOMETRIC = booleanPreferencesKey("biometric")
        val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        val KEY_PIN_SALT = stringPreferencesKey("pin_salt")
        val KEY_FAILED = intPreferencesKey("failed_count")
        val KEY_LOCKED_UNTIL = longPreferencesKey("locked_until")
    }
}
