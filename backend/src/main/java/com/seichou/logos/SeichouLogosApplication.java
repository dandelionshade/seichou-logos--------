package com.seichou.logos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SeichouLogosApplication
 * 项目的启动类 (Entry Point)
 */
@SpringBootApplication // 这是一个组合注解，包含了 @Configuration, @EnableAutoConfiguration 和 @ComponentScan。它告诉 Spring Boot 自动配置项目并扫描当前包及其子包下的所有组件。
public class SeichouLogosApplication {

    /**
     * Java 程序的标准主方法
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 启动 Spring Boot 应用程序，初始化 Spring 容器并启动内嵌的 Tomcat 服务器
        SpringApplication.run(SeichouLogosApplication.class, args);
    }

}
