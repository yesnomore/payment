package com.flyhtml.payment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author xiaowei
 * @time 17-4-5 上午10:28
 * @describe
 */
@EnableAutoConfiguration
@SpringBootApplication
@ComponentScan
@MapperScan("com.flyhtml.payment.db.mapper")
public class Application {
}