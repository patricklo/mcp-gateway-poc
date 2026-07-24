package cn.bugstack.ai.mcp.client.proxy.config;

import cn.bugstack.ai.mcp.client.proxy.api.IOpenAiApiProxy;
import io.reactivex.Single;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

@Configuration
public class Retrofit2Config {


    /**
     * Option1: Retrofit + OkHttpClient
     */
//    @Bean
//    public IOpenAiApiProxy openAiApi(@Value("${spring.ai.agent.base-url}") String baseUrl,
//                                     @Value("${spring.ai.agent.api-key}") String apiKey) {
//        OkHttpClient okHttpClient = new OkHttpClient.Builder()
//                .addInterceptor(chain -> {
//                    Request request = chain.request()
//                            .newBuilder()
//                            .addHeader("Content-Type", "application/json")
//                            .addHeader("Authorization", "Bearer " + apiKey)
//                            .build();
//                    return chain.proceed(request);
//                })
//                .build();
//
//        return new Retrofit.Builder()
//                .baseUrl(baseUrl)
//                .client(okHttpClient)
//                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
//                .addConverterFactory(JacksonConverterFactory.create())
//                .build().create(IOpenAiApiProxy.class);
//    }

    /**
     * Option2: Spring RestClient
     */

    @Bean
    public RestClient openAiApiRestClient(@Value("${spring.ai.agent.base-url}") String baseUrl,
                                          @Value("${spring.ai.agent.api-key}") String apiKey) {
        return RestClient
                .builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Bean
    public IOpenAiApiProxy openAiApi(RestClient openAiApiRestClient){
        return request -> Single.fromCallable(() -> openAiApiRestClient.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(Object.class)
        );
    }



}
