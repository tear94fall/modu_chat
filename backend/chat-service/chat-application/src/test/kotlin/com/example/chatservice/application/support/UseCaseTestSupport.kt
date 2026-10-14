package com.example.chatservice.application.support

import com.example.chatservice.application.common.exception.CustomException
import com.example.chatservice.application.common.exception.ErrorCode
import com.example.chatservice.application.member.MemberGateway
import com.example.chatservice.application.member.MemberInfo
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

/** 회원 n 의 userId 는 "user-n" 이다. */
fun member(id: Long) = MemberInfo(id = id, userId = "user-$id", username = "이름$id")

fun userId(id: Long) = "user-$id"

/** member-service 흉내: user-1 … user-9 가 있고, 초대·나가기는 받은 회원을 그대로 돌려준다. */
fun MemberGateway.stubMembers(range: LongRange = 1L..9L) {
    val all = range.associateWith { member(it) }
    whenever(byUserId(any())).thenAnswer { inv ->
        all.values.firstOrNull { it.userId == inv.getArgument<String>(0) } ?: throw NoSuchElementException("no member")
    }
    whenever(byUserIds(any())).thenAnswer { inv ->
        inv.getArgument<List<String>>(0).mapNotNull { u -> all.values.firstOrNull { it.userId == u } }
    }
    whenever(byIds(any())).thenAnswer { inv -> inv.getArgument<List<Long>>(0).distinct().mapNotNull { all[it] } }
    whenever(invite(any(), any())).thenAnswer { inv -> inv.getArgument<List<MemberInfo>>(1) }
    whenever(exit(any(), any())).thenAnswer { inv -> inv.getArgument<List<MemberInfo>>(1) }
}

fun assertErrorCode(expected: ErrorCode, block: () -> Unit) {
    assertThatThrownBy { block() }
        .isInstanceOfSatisfying(CustomException::class.java) { assertThat(it.errorCode).isEqualTo(expected) }
}
