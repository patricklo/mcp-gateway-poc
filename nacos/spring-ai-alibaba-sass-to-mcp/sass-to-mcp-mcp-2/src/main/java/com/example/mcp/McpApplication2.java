package com.example.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MCP服务端应用主类
 */
@SpringBootApplication
public class McpApplication2 {

    private static final Logger logger = LoggerFactory.getLogger(McpApplication2.class);

    public static void main(String[] args) {
        SpringApplication.run(McpApplication2.class, args);
        logger.info("MCP服务端启动成功");
    }

}
