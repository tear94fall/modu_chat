package com.example.pointservice.application.domain.entity

/** EARN 은 적립(규칙 또는 금액 지정 — 구매 적립 등), SPEND 는 사용(차감), REFUND 는 사용 취소(주문 취소 등으로 되돌림), ADJUST 는 관리자 수동 조정(양수·음수). */
enum class PointTransactionType { EARN, SPEND, REFUND, ADJUST }
