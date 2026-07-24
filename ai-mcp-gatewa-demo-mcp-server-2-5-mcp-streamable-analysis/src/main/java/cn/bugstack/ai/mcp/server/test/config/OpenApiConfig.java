package cn.bugstack.ai.mcp.server.test.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "MCP Server Test API",
                version = "1.0",
                description = "AI MCP Server 测试接口文档"
        )
)
@Configuration
public class OpenApiConfig {
}
