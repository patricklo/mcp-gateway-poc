package com.example.agent.hook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.fastjson.JSON;

public class LoggingInterceptor extends ModelInterceptor {
    Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        // 请求前记录
        logger.debug("发送请求到模型: " + JSON.toJSONString(request));

        // 执行实际调用
        ModelResponse response = handler.call(request);

        // 响应后记录
        logger.debug("模型响应: " + JSON.toJSONString(response));

        return response;
    }

    @Override
    public String getName() {
        return "LoggingInterceptor";
    }
}