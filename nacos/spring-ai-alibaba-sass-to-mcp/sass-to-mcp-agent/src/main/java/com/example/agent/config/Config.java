package com.example.agent.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.example.agent.hook.LoggingInterceptor;

@Configuration(proxyBeanMethods = false)
public class Config {

    @Bean
    public ReactAgent buildReactAgent(@Qualifier("distributedSyncToolCallback") ToolCallbackProvider tools, ChatModel chatModel) {
        ToolCallback[] toolCallbacks = tools.getToolCallbacks();
        System.out.println(">>> Available tools: ");
        for (int i = 0; i < toolCallbacks.length; i++) {
            System.out.println("[" + i + "] " + toolCallbacks[i].getToolDefinition().name());
        }
        ReactAgent agent = ReactAgent.builder().tools(tools.getToolCallbacks()).interceptors(new LoggingInterceptor()).model(chatModel).name("test_agent").saver(new MemorySaver()).build();
        return agent;

    }
}
