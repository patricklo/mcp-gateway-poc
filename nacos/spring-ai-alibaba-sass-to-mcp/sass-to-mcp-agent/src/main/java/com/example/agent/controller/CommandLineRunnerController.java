package com.example.agent.controller;

import java.util.Scanner;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Controller;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;

/**
 */
@Controller
public class CommandLineRunnerController implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(CommandLineRunnerController.class);

    ReactAgent agent;

    public CommandLineRunnerController(ReactAgent agent) {
        this.agent = agent;
    }

    @Override
    public void run(String... args) throws Exception {
        // threadId 是给定对话的唯一标识符
        RunnableConfig runnableConfig = RunnableConfig.builder().threadId(UUID.randomUUID().toString()).addMetadata("user_id", "1").build();
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\n>>> QUESTION: ");
            String userInput = scanner.nextLine();
            if (userInput.equalsIgnoreCase("exit")) {
                break;
            }
            if (userInput.isEmpty()) {
                userInput = "你好";
            }
            String res = "";
            try {
                AssistantMessage response = agent.call(userInput, runnableConfig);
                res = response.getText();
            } catch (GraphRunnerException e) {
                logger.error("", e);
                res = "系统异常";
            }
            System.out.println("\n>>> ASSISTANT: " + res);
        }
        scanner.close();
    }

}
