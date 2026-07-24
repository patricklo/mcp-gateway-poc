package cn.bugstack.ai.mcp.client.proxy.api;

import io.reactivex.Single;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * <a href="https://dash.cloudflare.com/">可代理api</a>
 */
public interface IOpenAiApiProxy {

    @POST("v1/chat/completions")
    Single<Object> completions(@Body Object request);

}
