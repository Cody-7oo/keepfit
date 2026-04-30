package com.example.demo.filter;

import com.example.demo.WhiteList;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

//@WebFilter(filterName = "xssFilter", urlPatterns = "/*")
public class XssFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String uri = req.getRequestURI();

        // ====================== 🔥 放行接口文档！关键！======================
        if (WhiteList.isDocUrl(uri)) {
            chain.doFilter(request, response);
            return;
        }

        // 业务接口才走 XSS 过滤
        chain.doFilter(new XssHttpServletRequestWrapper((HttpServletRequest) request), response);
    }
}