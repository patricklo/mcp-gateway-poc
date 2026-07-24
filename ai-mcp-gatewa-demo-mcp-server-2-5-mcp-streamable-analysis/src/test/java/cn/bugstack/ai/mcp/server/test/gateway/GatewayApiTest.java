package cn.bugstack.ai.mcp.server.test.gateway;

import cn.bugstack.ai.mcp.server.test.model.XxxResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

import java.time.Duration;

@Slf4j
public class GatewayApiTest {

    public static void main(String[] args) throws Exception {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl("https://apis.itedus.cn")
                .apiKey("sk-MEhn88b6iV7THIHp00724b306cAc41C0A1774eEd4aB99114")
                .completionsPath("v1/chat/completions")
                .embeddingsPath("v1/embeddings")
                .build();

        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("gpt-4o")
                        .toolCallbacks(new SyncMcpToolCallbackProvider(sseMcpClient_gateway()).getToolCallbacks())
                        .build())
                .build();

        log.info("测试结果:{}", chatModel.call("""
                获取公司雇员信息，信息如下；
                城市；北京
                公司；谷歌
                雇员；小傅哥
                """));
    }

    /**
     * http://localhost:8777/api-gateway/gateway_001/mcp/sse
     *
     * http://localhost:8777/api-gateway/gateway_001/mcp/sse?api_key=gw-lf3HFzlJCdnrYl20oHbd5lJQxE7GWz8wjsSgjDZfctJNV8s5
     */
    public static McpSyncClient sseMcpClient_gateway() {
        HttpClientSseClientTransport sseClientTransport = HttpClientSseClientTransport
                .builder("http://localhost:8777")
//                .sseEndpoint("/api-gateway/gateway_001/mcp/sse?api_key=gw-lf3HFzlJCdnrYl20oHbd5lJQxE7GWz8wjsSgjDZfctJNV8s5")
                .sseEndpoint("/api-gateway/gateway_005/mcp/sse")
                .build();

        McpSyncClient mcpSyncClient = McpClient.sync(sseClientTransport).requestTimeout(Duration.ofMinutes(360)).build();
        var init_sse = mcpSyncClient.initialize();
        log.info("Tool SSE MCP Initialized {}", init_sse);

        return mcpSyncClient;
    }

    @Test
    public void testSerialization() throws Exception {
        XxxResponse response = new XxxResponse();
        response.setSalary("16.92");
        response.setDayManHour("14");

        XxxResponse.User user = new XxxResponse.User();
        user.setUserId("111");
        user.setUserName("小傅哥");
        response.setUser(user);

        // Current toString
        System.out.println("Current toString: " + response.toString());

        // Jackson Serialization
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(response);
        System.out.println("Jackson JSON: " + json);

        // Check if it matches expectation
        // Expected: {"salary":"16.92","dayManHour":"14","user":{"userId":"111","userName":"小傅哥"}}
    }

}
