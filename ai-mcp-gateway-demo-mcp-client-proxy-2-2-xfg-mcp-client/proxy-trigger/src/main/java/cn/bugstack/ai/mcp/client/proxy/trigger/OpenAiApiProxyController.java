package cn.bugstack.ai.mcp.client.proxy.trigger;

import cn.bugstack.ai.mcp.client.proxy.api.IOpenAiApiProxy;
import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController()
@CrossOrigin("*")
@RequestMapping("/v1/")
public class OpenAiApiProxyController {

    @Resource
    private IOpenAiApiProxy openAiApiProxy;

    /**
     * curl http://127.0.0.1:8771/v1/chat/completions
     */
    @RequestMapping(value = "chat/completions", method = RequestMethod.POST)
    public Object completions(@RequestBody Object request) {
        log.info("请求入参：{}", JSON.toJSONString(request));
        return openAiApiProxy.completions(request).blockingGet();
    }

}
