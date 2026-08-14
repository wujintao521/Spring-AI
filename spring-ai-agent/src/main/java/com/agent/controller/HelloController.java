package com.agent.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用于验证 Spring MVC 是否正常工作的测试接口。
 */
@RestController
@RequestMapping("/api")
public class HelloController {
    /**
     * GET http://localhost:8080/api/hello
     */
    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of("message", "Spring AI Agent 已成功启动");
    }
}
