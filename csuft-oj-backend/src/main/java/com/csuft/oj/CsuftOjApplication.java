package com.csuft.oj;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@MapperScan("com.csuft.oj.mapper")
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class CsuftOjApplication {

    public static void main(String[] args) {
        SpringApplication.run(CsuftOjApplication.class, args);
    }
}
