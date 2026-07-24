package cn.bugstack.ai.mcp.server.test.stateless;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

import java.time.Duration;

/**
 * https://cloud.tencent.com/developer/article/2514722
 */
@Slf4j
public class StatelessApiTest {

    public static void main(String[] args) throws Exception {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl("https://apis.itedus.cn")
                .apiKey("sk-plI5Vzs4AQWWJkDT29Af517d624a4d48Ac9fA75589Ee30Fd")
                .completionsPath("v1/chat/completions")
                .embeddingsPath("v1/embeddings")
                .build();

        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("gpt-4o")
                        .toolCallbacks(new SyncMcpToolCallbackProvider(sseMcpClient_stateless()).getToolCallbacks())
                        .build())
                .build();

        log.info("测试结果:{}", chatModel.call("""
                获取公司雇员信息，信息如下；
                城市；北京
                公司；谷歌
                雇员；小傅哥
                """));
    }

    public static McpSyncClient sseMcpClient_stateless() {

        McpClientTransport mcpClientTransport = HttpClientStreamableHttpTransport
                .builder("http://localhost:8701")
                .endpoint("/mcp")
                .build();

        McpSyncClient mcpSyncClient = McpClient.sync(mcpClientTransport).requestTimeout(Duration.ofMinutes(36000)).build();
        var init_sse = mcpSyncClient.initialize();
        log.info("Tool SSE MCP Initialized {}", init_sse);

        return mcpSyncClient;
    }

}
