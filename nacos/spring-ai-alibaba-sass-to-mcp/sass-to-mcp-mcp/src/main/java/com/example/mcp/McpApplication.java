package com.example.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MCP服务端应用主类
 */
@SpringBootApplication
public class McpApplication {

    private static final Logger logger = LoggerFactory.getLogger(McpApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(McpApplication.class, args);
        logger.info("MCP服务端启动成功");
    }

}
