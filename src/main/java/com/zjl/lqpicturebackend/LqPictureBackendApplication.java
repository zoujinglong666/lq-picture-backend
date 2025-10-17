package com.zjl.lqpicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.zjl.lqpicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableScheduling
public class LqPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(LqPictureBackendApplication.class, args);
    }

}
