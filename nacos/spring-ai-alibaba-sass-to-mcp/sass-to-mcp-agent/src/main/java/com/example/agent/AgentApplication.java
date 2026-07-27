package com.example.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI代理应用主类
 */
@SpringBootApplication
public class AgentApplication {

    private static final Logger logger = LoggerFactory.getLogger(AgentApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
        logger.info("AI代理服务启动成功");
    }

}
