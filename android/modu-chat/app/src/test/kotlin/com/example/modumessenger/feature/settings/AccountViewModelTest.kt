package com.example.modumessenger.feature.settings

import app.cash.turbine.test
import com.example.modumessenger.R
import com.example.modumessenger.core.model.Member
import com.example.modumessenger.core.session.SessionEvents
import com.example.modumessenger.core.session.SessionLogout
import com.example.modumessenger.core.session.SessionStore
import com.example.modumessenger.data.repository.AccountRepository
import com.example.modumessenger.data.repository.AuthRepository
import com.example.modumessenger.data.repository.PushRepository
import com.example.modumessenger.feature.chat.FakeChatRoomApi
import com.example.modumessenger.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeAccountRepository(private val result: Result<Unit>) : AccountRepository {
        var calls = 0
        override suspend fun withdraw(): Result<Unit> {
            calls++
            return result
        }
    }

    private class FakeAuthRepository : AuthRepository {
        var logoutCalls = 0
        override suspend fun loginWithGoogle(idToken: String, email: String): Result<Member> = error("unused")
        override suspend fun logout(): Result<Unit> {
            logoutCalls++
            return Result.success(Unit)
        }
        override suspend fun issueSsoCode(request: com.example.modumessenger.data.dto.SsoCodeRequestDto) = error("unused")
    }

    private class FakePushRepository : PushRepository {
        val unsubscribed = mutableListOf<String>()
        override suspend fun registerToken(token: String): Result<Unit> = Result.success(Unit)
        override fun subscribeRooms(roomIds: List<String>) = Unit
        override fun unsubscribeRooms(roomIds: List<String>) { unsubscribed += roomIds }
    }

    private fun viewModel(account: AccountRepository, auth: FakeAuthRepository = FakeAuthRepository(), push: FakePushRepository = FakePushRepository()): AccountViewModel {
        val sessionStore: SessionStore = mockk(relaxed = true)
        coEvery { sessionStore.memberNow() } returns Member(id = 11, userId = "me", username = "나", email = "me@x.y")
        return AccountViewModel(SessionLogout(auth, push, FakeChatRoomApi(), sessionStore, SessionEvents()), account)
    }

    @Test
    fun `탈퇴가 되면 서버 탈퇴 후 로그아웃과 같은 정리를 하고 로그인 화면으로 보낸다`() = runTest {
        val account = FakeAccountRepository(Result.success(Unit))
        val auth = FakeAuthRepository()
        val vm = viewModel(account, auth)

        vm.loggedOut.test {
            vm.withdraw()
            awaitItem()
            assertEquals(1, account.calls)
            assertEquals(1, auth.logoutCalls)
            assertFalse(vm.isWorking.value)
        }
    }

    @Test
    fun `탈퇴가 실패하면 세션을 지우지 않고 안내만 띄운다`() = runTest {
        val account = FakeAccountRepository(Result.failure(RuntimeException("500")))
        val auth = FakeAuthRepository()
        val vm = viewModel(account, auth)

        vm.messages.test {
            vm.withdraw()
            assertEquals(R.string.account_withdraw_failed, awaitItem())
            assertEquals(0, auth.logoutCalls)
            assertFalse(vm.isWorking.value)
        }
    }

    @Test
    fun `로그아웃은 예전처럼 동작한다`() = runTest {
        val auth = FakeAuthRepository()
        val vm = viewModel(FakeAccountRepository(Result.success(Unit)), auth)

        vm.loggedOut.test {
            vm.logout()
            awaitItem()
            assertEquals(1, auth.logoutCalls)
            assertTrue(true)
        }
    }
}
