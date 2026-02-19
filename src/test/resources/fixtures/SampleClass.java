package com.example.fixture;

/**
 * 사용자 정보를 담는 클래스
 */
public class SampleClass {

    /**
     * 사용자 이름
     */
    private String name;

    /**
     * 사용자 나이
     */
    private int age;

    // 라인 코멘트는 무시되어야 함
    private String address;

    private String noComment;

    /**
     * 사용자 정보를 조회한다.<br>
     * 상세한 설명이 포함된 메서드.
     *
     * @param userId 사용자 ID
     * @param includeDetail 상세 포함 여부
     * @return 사용자 정보 객체
     * @response 200 성공
     * @response 404 사용자를 찾을 수 없음
     * @group 사용자 관리
     * @auth true
     * @header X-Custom-Token 커스텀 인증 토큰
     */
    public Object getUser(String userId, boolean includeDetail) {
        return null;
    }

    public void noJavadocMethod() {
    }
}
