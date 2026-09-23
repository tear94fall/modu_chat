package com.example.pointservice.point.entity

/** EARN 은 규칙에 따른 적립, SPEND 는 사용(차감), REFUND 는 사용 취소(주문 취소 등으로 되돌림), ADJUST 는 관리자 수동 조정(양수·음수). */
enum class PointTransactionType { EARN, SPEND, REFUND, ADJUST }
