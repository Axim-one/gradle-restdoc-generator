package com.example;

import one.axim.gradle.annotation.XApiIgnore;
import org.springframework.web.bind.annotation.*;

/**
 * 내부 전용 컨트롤러 (문서 생성에서 제외)
 */
@XApiIgnore
@RestController
@RequestMapping("/internal")
public class InternalController {

    /**
     * 헬스체크
     *
     * @return 상태
     * @group 내부
     */
    @GetMapping(name = "헬스체크", value = "/health")
    public String health() {
        return "ok";
    }
}
