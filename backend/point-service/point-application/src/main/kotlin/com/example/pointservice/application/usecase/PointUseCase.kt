package com.example.pointservice.application.usecase

import com.example.pointservice.application.service.PointCommandService
import com.example.pointservice.application.usecase.command.AdjustCommand
import com.example.pointservice.application.usecase.command.EarnAmountCommand
import com.example.pointservice.application.usecase.command.EarnCommand
import com.example.pointservice.application.usecase.command.SpendCommand
import com.example.pointservice.application.usecase.result.EarnResult
import com.example.pointservice.application.usecase.result.PointBalanceResult
import com.example.pointservice.application.usecase.result.PointTransactionResult
import com.example.pointservice.application.usecase.result.SpendResult
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

/**
 * 포인트 원장 유스케이스: 적립·출석·사용·환불·조정과, 그 직후에 읽는 잔액·원장.
 * 앱(공개)·다른 서비스(내부)·백오피스(조정)가 같이 쓴다. 트랜잭션은 서비스가 연다.
 */
@Component
class PointUseCase(private val pointCommandService: PointCommandService) {

    /** 적립·사용 직후에 읽히므로 master 에서 읽는다. */
    fun balance(userId: String): PointBalanceResult = pointCommandService.balance(userId)

    /** 적립·사용 직후에 읽히므로 master 에서 읽는다. */
    fun history(userId: String, pageable: Pageable): Page<PointTransactionResult> = pointCommandService.history(userId, pageable)

    fun earn(command: EarnCommand): EarnResult = pointCommandService.earn(command)

    fun earnAmount(command: EarnAmountCommand): EarnResult = pointCommandService.earnAmount(command)

    fun checkIn(userId: String): EarnResult = pointCommandService.checkIn(userId)

    fun spend(command: SpendCommand): SpendResult = pointCommandService.spend(command)

    fun refund(command: SpendCommand): SpendResult = pointCommandService.refund(command)

    fun adjust(command: AdjustCommand): PointBalanceResult = pointCommandService.adjust(command)
}
