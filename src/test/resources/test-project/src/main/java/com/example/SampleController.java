package com.example;

import com.example.dto.UserDto;
import com.example.dto.UserStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 사용자 API 컨트롤러
 */
@RestController
@RequestMapping("/api/users")
public class SampleController {

    /**
     * 사용자 목록 조회
     *
     * @return 사용자 목록
     * @group 사용자
     */
    @GetMapping(name = "사용자 목록 조회", value = "")
    public List<UserDto> getUsers() {
        return null;
    }

    /**
     * 사용자 상세 조회
     *
     * @param id 사용자 ID
     * @return 사용자 정보
     * @response 200 성공
     * @response 404 사용자 없음
     * @group 사용자
     * @auth true
     */
    @GetMapping(name = "사용자 상세 조회", value = "/{id}")
    public UserDto getUser(@PathVariable("id") Long id) {
        return null;
    }

    /**
     * 사용자 상태 조회
     *
     * @param id 사용자 ID
     * @return 사용자 상태
     * @group 사용자
     */
    @GetMapping(name = "사용자 상태 조회", value = "/{id}/status")
    public UserStatus getUserStatus(@PathVariable("id") Long id) {
        return null;
    }
}
