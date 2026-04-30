package com.example.demo.service.impl;

import com.example.demo.entity.OperationLog;
import com.example.demo.mapper.OperationLogMapper;
import com.example.demo.service.OperationLogAsyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class OperationLogAsyncServiceImpl implements OperationLogAsyncService {

    private static final Logger logger = LoggerFactory.getLogger(OperationLogAsyncServiceImpl.class);

    @Resource
    private OperationLogMapper operationLogMapper;

    @Override
    public void saveOperationLog(OperationLog log) {
        try {
            operationLogMapper.insert(log);
        } catch (Exception e) {
            logger.error("[异步操作日志] 入库失败: {}", e.getMessage(), e);
        }
    }
}