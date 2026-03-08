package com.example.hello.interceptor;

import com.example.hello.entity.Employee;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
        Employee employee = (Employee) session.getAttribute("currentUser");
        
        if (employee == null) {
            response.sendRedirect("/login");
            return false;
        }

        // navMenus 现在由 MenuAdvice 全局注入，无需在此处处理
        
        return true;
    }
}
