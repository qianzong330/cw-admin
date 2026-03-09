package com.example.hello.config;

import com.example.hello.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/doLogin", "/logout", "/static/**", "/css/**", "/js/**", "/uploads/**", "/api/test/**", "/fix/**", "/debug/**", "/role/list-json");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + System.getProperty("user.dir") + "/src/main/resources/static/uploads/");
    }
    
    /**
     * 配置消息转换器，确保响应使用 UTF-8 编码
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 添加 StringHttpMessageConverter，设置字符集为 UTF-8
        converters.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
    }
}
