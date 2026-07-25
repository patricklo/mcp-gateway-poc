package cn.bugstack.ai.domain.session.service.message.handler.impl;

import cn.bugstack.ai.domain.session.model.valobj.McpSchemaVO;
import cn.bugstack.ai.domain.session.service.message.handler.IRequestHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 返回服务器支持的工具列表
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/12/20 11:29
 */
@Slf4j
@Service("toolsListHandler")
public class ToolsListHandler implements IRequestHandler {
    /**
     * Tool List Request:
     * {
     *   "jsonrpc": "2.0",
     *   "id": 3,
     *   "method": "tools/list"
     * }
     *
     * Tool List Response:
     * {
     *   "jsonrpc": "2.0",
     *   "id": 3,
     *   "result": {
     *     "tools": [
     *       {
     *         "name": "calculate",
     *         "description": "Perform basic math operations",
     *         "inputSchema": {
     *           "type": "object",
     *           "properties": {
     *             "operation": {
     *               "type": "string",
     *               "enum": ["add", "subtract", "multiply", "divide"]
     *             },
     *             "a": {"type": "number"},
     *             "b": {"type": "number"}
     *           },
     *           "required": ["operation", "a", "b"]
     *         }
     *       }
     *     ]
     *   }
     * }
     */

    @Override
    public McpSchemaVO.JSONRPCResponse handle(McpSchemaVO.JSONRPCRequest message) {
        return new McpSchemaVO.JSONRPCResponse("2.0", message.id(), Map.of(
                "tools", new Object[]{
                        Map.of(
                                "name", "toUpperCase",
                                "description", "小写转大写",
                                "inputSchema", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "word", Map.of(
                                                        "type", "string",
                                                        "description", "单词，字符串"
                                                )
                                        ),
                                        "required", new String[]{"word"}
                                )
                        )
                }
        ), null);

    }

}
