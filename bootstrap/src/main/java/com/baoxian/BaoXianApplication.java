package com.baoxian;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.baoxian")
@MapperScan("com.baoxian.infra.mapper")
@EnableScheduling
public class BaoXianApplication {

    public static void main(String[] args) {
        SpringApplication.run(BaoXianApplication.class, args);
    }
}
