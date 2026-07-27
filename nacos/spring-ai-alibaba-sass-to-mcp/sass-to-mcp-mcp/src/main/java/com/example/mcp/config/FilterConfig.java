package com.example.mcp.config;

import com.example.mcp.filter.TraceIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 过滤器配置
 */
@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistrationBean(TraceIdFilter traceIdFilter) {
        FilterRegistrationBean<TraceIdFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(traceIdFilter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setName("traceIdFilter");
        registrationBean.setOrder(1);
        return registrationBean;
    }

}
