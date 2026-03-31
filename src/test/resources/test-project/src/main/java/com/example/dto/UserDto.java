package com.example.dto;

import java.util.Set;

/**
 * 사용자 정보 DTO
 */
public class UserDto extends BaseDto {

    /**
     * 사용자 이름
     */
    private String name;

    /**
     * 권한 목록
     */
    private Set<String> permissions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }
}
