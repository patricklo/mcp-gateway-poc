package cn.bugstack.ai.domain.session.service.message.handler.impl;


import cn.bugstack.ai.domain.session.model.valobj.McpSchemaVO;
import cn.bugstack.ai.domain.session.service.message.handler.IRequestHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 返回可用资源列表
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/12/20 11:31
 */
@Slf4j
@Service("resourcesListHandler")
public class ResourcesListHandler implements IRequestHandler {

    /**
     * Resource List Request:
     * {
     *   "jsonrpc": "2.0",
     *   "id": 5,
     *   "method": "resources/list"
     * }
     *
     * Resource List Response:
     * {
     *   "jsonrpc": "2.0",
     *   "id": 5,
     *   "result": {
     *     "resources": [
     *       {
     *         "uri": "file:///config.json",
     *         "name": "Configuration",
     *         "description": "Application configuration file",
     *         "mimeType": "application/json"
     *       }
     *     ]
     *   }
     * }
     */
    @Override
    public McpSchemaVO.JSONRPCResponse handle(McpSchemaVO.JSONRPCRequest message) {
        return new McpSchemaVO.JSONRPCResponse("2.0", message.id(), Map.of(
                "resources", Map.of(
                        "resources", new Object[]{}
                )
        ), null);
    }

}
