package com.example.demo.filter;

import com.example.demo.common.util.XssUtil;
import org.springframework.web.util.HtmlUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    // 过滤普通参数
    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return XssUtil.clean(value);
    }

    // 过滤数组参数
    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) {
            return null;
        }
        String[] newValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            newValues[i] = XssUtil.clean(values[i]);
        }
        return newValues;
    }

    // 过滤请求头
    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        return XssUtil.clean(value);
    }
}