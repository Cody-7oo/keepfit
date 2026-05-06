package com.example.demo.filter;

import com.example.demo.WhiteList;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(filterName = "xssFilter", urlPatterns = "/*")
public class XssFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String uri = req.getRequestURI();
        String contentType = request.getContentType();

        // ====================== 放行名单 ======================
        boolean isWhiteUrl = WhiteList.isDocUrl(uri)
                || uri.startsWith("/user/login")
                || uri.startsWith("/user/register");

        // 放行：白名单 + JSON 请求(application/json)
        boolean isJson = contentType != null && contentType.contains("application/json");

        if (isWhiteUrl || isJson) {
            chain.doFilter(request, response);
            return;
        }

        // 非JSON、非白名单才走XSS过滤
        chain.doFilter(new XssHttpServletRequestWrapper(req), response);
    }
}