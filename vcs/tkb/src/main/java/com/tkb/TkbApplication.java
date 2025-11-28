package com.tkb;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@MapperScan("com.tkb.mapper")
public class TkbApplication {
    public static void main(String[] args) {
        SpringApplication.run(TkbApplication.class, args);
    }
}
