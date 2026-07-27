package com.god.mz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.god.mz.mapper")
public class MozhanApplication {

    public static void main(String[] args) {
        SpringApplication.run(MozhanApplication.class, args);
    }
}
