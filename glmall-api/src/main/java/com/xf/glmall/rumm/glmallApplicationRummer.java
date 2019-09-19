package com.xf.glmall.rumm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
@Slf4j
@Component
public class glmallApplicationRummer implements ApplicationRunner {

        @Value("${server.port}")
        private int port;

        @Override
        public void run(ApplicationArguments applicationArguments) {
            log.info("商城部署完成，访问地址：http://localhost:" + port);
        }

}
