package com.example.dto;

/**
 * 사용자 정보 DTO
 */
public class UserDto extends BaseDto {

    /**
     * 사용자 이름
     */
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
