package com.agent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动类
 */
@MapperScan("com.agent.dao")
@SpringBootApplication
public class SpringAiAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringAiAgentApplication.class, args);
    }
}
