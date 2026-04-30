package com.example.demo.filter;

import com.example.demo.WhiteList;
import org.springframework.stereotype.Component;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.regex.Pattern;

//@Component
public class SqlInjectionFilter implements Filter {

    private static final Pattern SQL_PATTERN = Pattern
            .compile("(\\b(and|or)\\b)|(\\b(union|select|insert|delete|update|drop|truncate|alter)\\b)|[;']",
                    Pattern.CASE_INSENSITIVE);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String uri = req.getRequestURI();

        // ====================== 【关键修复】放行接口文档 ======================
        if (WhiteList.isDocUrl(uri)) {
            chain.doFilter(request, response);
            return;
        }

        // 业务接口才走SQL过滤
        chain.doFilter(new SqlInjectionWrapper(req), response);
    }

    private static class SqlInjectionWrapper extends HttpServletRequestWrapper {
        public SqlInjectionWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) return null;
            for (int i = 0; i < values.length; i++) {
                values[i] = filter(values[i]);
            }
            return values;
        }

        @Override
        public String getParameter(String name) {
            return filter(super.getParameter(name));
        }

        private String filter(String value) {
            if (value == null) return null;
            return SQL_PATTERN.matcher(value).replaceAll("");
        }
    }
}