package com.example.dto;

/**
 * 기본 DTO
 */
public class BaseDto {

    /**
     * 고유 식별자
     */
    private Long id;

    /**
     * 생성일시
     */
    private String createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
