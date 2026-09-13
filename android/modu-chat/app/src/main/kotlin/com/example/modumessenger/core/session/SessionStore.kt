package com.example.modumessenger.core.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.modumessenger.core.model.FriendStatus
import com.example.modumessenger.core.model.Member
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** 파일 이름과 키 이름은 기존 자바 앱과 같다. 같은 기기에 이어 깔아도 로그인 상태가 살아 있어야 한다. */
private val Context.moduDataStore: DataStore<Preferences> by preferencesDataStore(name = SessionStore.STORE_NAME)

/**
 * 토큰·회원·FCM 토큰 저장소.
 *
 * 기존 앱은 토큰을 `"Bearer <jwt>"` 로 저장했다. 새 앱은 순수 JWT 로 저장하되, 읽을 때 앞머리가 붙어 있으면
 * 떼어 낸다(기존 데이터 호환).
 */
@Singleton
class SessionStore @Inject constructor(
    @ApplicationContext context: Context,
    private val gson: Gson,
) {

    private val dataStore = context.moduDataStore

    val member: Flow<Member?> = dataStore.data.map { prefs -> decodeMember(prefs[KEY_MEMBER]) }

    val isLoggedIn: Flow<Boolean> = dataStore.data.map { prefs ->
        !prefs[KEY_ACCESS].isNullOrBlank() && decodeMember(prefs[KEY_MEMBER]) != null
    }

    suspend fun accessToken(): String? = readToken(KEY_ACCESS)

    suspend fun refreshToken(): String? = readToken(KEY_REFRESH)

    suspend fun memberNow(): Member? = member.first()

    /** 순수 JWT 로 저장한다. */
    suspend fun saveTokens(access: String, refresh: String) {
        dataStore.edit { prefs ->
            prefs[KEY_ACCESS] = strip(access)
            prefs[KEY_REFRESH] = strip(refresh)
        }
    }

    suspend fun saveMember(member: Member) {
        val json = gson.toJson(member)
        dataStore.edit { prefs -> prefs[KEY_MEMBER] = json }
    }

    suspend fun fcmToken(): String? = dataStore.data.first()[KEY_FCM]

    suspend fun saveFcmToken(token: String) {
        dataStore.edit { prefs -> prefs[KEY_FCM] = token }
    }

    /** 친구 별칭 캐시 원본 JSON. [FriendNames] 만 쓴다. */
    suspend fun friendNamesJson(): String? = dataStore.data.first()[KEY_FRIEND_NAMES]

    suspend fun saveFriendNamesJson(json: String) {
        dataStore.edit { prefs -> prefs[KEY_FRIEND_NAMES] = json }
    }

    /** 차단한 친구 userId 목록 원본 JSON(배열). [BlockedUsers] 만 쓴다. */
    suspend fun blockedIdsJson(): String? = dataStore.data.first()[KEY_BLOCKED_IDS]

    suspend fun saveBlockedIdsJson(json: String) {
        dataStore.edit { prefs -> prefs[KEY_BLOCKED_IDS] = json }
    }

    /** 로그아웃. `fcm-token`, `friend-names`, `blocked-ids` 는 남긴다(기존 앱과 같은 범위). */
    suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_MEMBER)
            prefs.remove(KEY_ACCESS)
            prefs.remove(KEY_REFRESH)
        }
    }

    private suspend fun readToken(key: Preferences.Key<String>): String? {
        val raw = dataStore.data.first()[key] ?: return null
        val value = strip(raw)
        return value.ifBlank { null }
    }

    private fun decodeMember(json: String?): Member? {
        if (json.isNullOrBlank()) return null
        // 기존 자바 앱이 저장한 회원 JSON 은 role 이 "ROLE_USER" 이고 profiles 키가 "profile" 이라
        // 이 모델로는 안전하게 읽을 수 없다. 없는 것으로 보고 무음 로그인으로 다시 받게 한다.
        if (json.contains("\"ROLE_")) return null
        val member = runCatching { gson.fromJson(json, Member::class.java) }.getOrNull() ?: return null
        return member.normalized().takeIf { it.userId.isNotBlank() }
    }

    /**
     * Gson 은 생성자를 거치지 않으므로, 이 필드가 생기기 전에 저장된 JSON 을 읽으면
     * null 이 아닌 자리에 null 이 들어온다. 읽는 쪽이 터지지 않게 여기서 기본값으로 메운다.
     */
    @Suppress("SENSELESS_COMPARISON")
    private fun Member.normalized(): Member = when {
        friendStatus == null -> copy(friendStatus = FriendStatus.NORMAL)
        else -> this
    }

    private fun strip(value: String): String =
        if (value.startsWith(BEARER_PREFIX)) value.substring(BEARER_PREFIX.length) else value

    companion object {
        const val STORE_NAME = "modu-chat"
        private const val BEARER_PREFIX = "Bearer "

        val KEY_ACCESS = stringPreferencesKey("access-token")
        val KEY_REFRESH = stringPreferencesKey("refresh-token")
        val KEY_MEMBER = stringPreferencesKey("member")
        val KEY_FCM = stringPreferencesKey("fcm-token")
        val KEY_FRIEND_NAMES = stringPreferencesKey("friend-names")
        val KEY_BLOCKED_IDS = stringPreferencesKey("blocked-ids")
    }
}
