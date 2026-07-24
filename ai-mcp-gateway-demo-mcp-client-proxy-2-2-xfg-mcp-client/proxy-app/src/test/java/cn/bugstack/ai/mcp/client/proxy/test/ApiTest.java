package cn.bugstack.ai.mcp.client.proxy.test;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Duration;
import java.util.Arrays;

/**
 * 协议对象；JSONRPCMessage
 * <p>
 * 测试说明；
 * 1. debug 启动服务端，8771 端口
 * 2. 在 OpenAiApiProxyController 执行接口方法的日志上打断点
 * 3. debug 调试 test 方法，观察 mcp 协议
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class ApiTest {

    @Resource
    private ChatClient.Builder chatClientBuilder;

    @Test
    public void test() {
//        ChatClient chatClient = chatClientBuilder.defaultOptions(
//                        OpenAiChatOptions.builder()
//                                .model("gpt-4.1-mini")
//                                .toolCallbacks(new SyncMcpToolCallbackProvider(sseMcpClient()).getToolCallbacks())
//                                .build())
//                .build();
//
//        log.info("测试结果:{}", chatClient.prompt("有哪些工具可以使用").call().content());

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl("http://127.0.0.1:8771")
                .apiKey("OPENAI_API_KEY")
                .completionsPath("v1/chat/completions")
                .embeddingsPath("v1/embeddings")
                .build();

        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("gpt-4o")
                        .toolCallbacks(new SyncMcpToolCallbackProvider(sseMcpClient()).getToolCallbacks())
                        .build())
                .build();

        log.info("测试结果:{}", chatModel.call("""
                获取公司雇员信息，信息如下；
                城市；北京
                公司；谷歌
                雇员；小傅哥
                """));
    }

    public McpSyncClient sseMcpClient() {
        HttpClientSseClientTransport sseClientTransport = HttpClientSseClientTransport
                .builder("http://localhost:8701/sse")
                .build();

        McpSyncClient mcpSyncClient = McpClient.sync(sseClientTransport).requestTimeout(Duration.ofMinutes(360)).build();
        var init_sse = mcpSyncClient.initialize();
        log.info("Tool SSE MCP Initialized {}", init_sse);

        return mcpSyncClient;
    }

}
