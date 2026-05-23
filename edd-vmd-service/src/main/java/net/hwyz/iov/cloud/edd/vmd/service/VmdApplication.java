package net.hwyz.iov.cloud.edd.vmd.service;


import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.security.annotation.EnableCustomConfig;
import net.hwyz.iov.cloud.framework.security.annotation.EnableCustomFeignClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 启动类
 *
 * @author hwyz_leo
 */
@Slf4j
@EnableAsync
@EnableCustomConfig
@EnableDiscoveryClient
@EnableCustomFeignClients
@SpringBootApplication
public class VmdApplication {

    public static void main(String[] args) {
        SpringApplication.run(VmdApplication.class, args);
        log.info("应用启动完成");
    }

}
