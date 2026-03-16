package com.example.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 사용자 검색 조건
 */
public class UserSearchRequest {

    /** 사용자 이름 */
    private String name;

    /** 사용자 상태 */
    @NotNull
    private UserStatus status;

    /** 네트워크 ID */
    private Long networkId;

    /** 활성 여부 */
    private boolean active;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    public Long getNetworkId() { return networkId; }
    public void setNetworkId(Long networkId) { this.networkId = networkId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
