package com.example.hello.config;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import java.io.IOException;

/**
 * UTF-8 字符编码过滤器
 * 确保所有请求和响应都使用 UTF-8 编码
 */
@WebFilter(urlPatterns = "/*", filterName = "characterEncodingFilter")
public class CharacterEncodingFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 初始化
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // 设置请求编码
        request.setCharacterEncoding("UTF-8");
        // 设置响应编码
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        
        // 继续执行过滤链
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // 销毁
    }
}
