package com.zjl.lqpicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.zjl.lqpicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class LqPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(LqPictureBackendApplication.class, args);
    }

}
