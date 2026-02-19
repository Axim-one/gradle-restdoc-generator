package com.example;

import com.example.dto.ApiResult;
import com.example.dto.OrderDto;
import com.example.dto.UserDto;
import com.example.dto.UserStatus;
import com.example.exception.AuthException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * @error UserNotFoundException
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
    public UserStatus getUserStatus(@PathVariable("id") Long id) throws AuthException {
        return null;
    }

    /**
     * 사용자 페이징 조회
     *
     * @param pageable 페이지 정보
     * @return 사용자 페이징 목록
     * @group 사용자
     */
    @GetMapping(name = "사용자 페이징 조회", value = "/paged")
    public Page<UserDto> getUsersPaged(Pageable pageable) {
        return null;
    }

    /**
     * 주문 상태별 조회
     *
     * @param status 주문 상태
     * @return 주문 목록
     * @group 주문
     */
    @GetMapping(name = "주문 상태별 조회", value = "/orders")
    public List<OrderDto> getOrdersByStatus(@RequestParam("status") OrderDto.OrderStatus status) {
        return null;
    }

    /**
     * 사용자 단건 조회 (ApiResult 래핑)
     *
     * @param id 사용자 ID
     * @return 사용자 정보
     * @group 사용자
     */
    @GetMapping(name = "사용자 단건 조회 래핑", value = "/wrapped/{id}")
    public ApiResult<UserDto> getUserWrapped(@PathVariable("id") Long id) {
        return null;
    }

    /**
     * 사용자 목록 조회 (ApiResult + List 래핑)
     *
     * @return 사용자 목록
     * @group 사용자
     */
    @GetMapping(name = "사용자 목록 조회 래핑", value = "/wrapped")
    public ApiResult<List<UserDto>> getUsersWrapped() {
        return null;
    }

    /**
     * 사용자 페이징 조회 (ApiResult + Page 래핑)
     *
     * @param pageable 페이지 정보
     * @return 사용자 페이징 목록
     * @group 사용자
     */
    @GetMapping(name = "사용자 페이징 조회 래핑", value = "/wrapped/paged")
    public ApiResult<Page<UserDto>> getUsersPagedWrapped(Pageable pageable) {
        return null;
    }
}
