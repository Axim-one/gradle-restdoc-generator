package com.external.entity;

/**
 * 외부 모듈의 Entity 클래스 (basePackage 밖)
 */
public class PartnerEntity {

    /** 파트너 ID */
    private Long id;

    /** 파트너명 */
    private String name;

    /** 파트너 이메일 */
    private String email;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
