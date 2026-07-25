package com.example.personalmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 个人管理系统启动类，Spring Boot 应用入口。
 */
@SpringBootApplication
public class PersonalManagerApplication {

    /**
     * 应用主入口，启动 Spring Boot 容器。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(PersonalManagerApplication.class, args);
    }
}
