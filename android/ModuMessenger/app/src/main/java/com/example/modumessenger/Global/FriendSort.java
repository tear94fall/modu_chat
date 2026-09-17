package com.example.modumessenger.Global;

/** 친구 목록 API 의 sort 파라미터 값. 서버(member-service FriendSort)가 허용하는 문자열과 같아야 한다. */
public final class FriendSort {
    public static final String NAME_ASC = "name,asc";
    public static final String NAME_DESC = "name,desc";
    public static final String EMAIL_ASC = "email,asc";
    public static final String EMAIL_DESC = "email,desc";

    /** 화면들이 쓰는 기본값. 정렬을 고르는 UI 는 아직 없다. */
    public static final String DEFAULT = NAME_ASC;

    private FriendSort() {}
}
