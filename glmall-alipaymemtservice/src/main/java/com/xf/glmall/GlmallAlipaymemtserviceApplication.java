package com.xf.glmall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan(basePackages = "com.xf.glmall.dao")
public class GlmallAlipaymemtserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GlmallAlipaymemtserviceApplication.class, args);
    }

}

