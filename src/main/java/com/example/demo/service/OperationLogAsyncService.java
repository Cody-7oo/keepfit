package com.example.demo.service;

import com.example.demo.entity.OperationLog;
import org.springframework.scheduling.annotation.Async;

public interface OperationLogAsyncService {

    /**
     * 异步入库操作日志
     */
    @Async("bizAsyncExecutor")
    void saveOperationLog(OperationLog log);
}