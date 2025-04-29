package com.hua.cloud.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;
@Slf4j
public class MilvusMapperPerformanceAspect {
    @Pointcut("")
    public void milvusMapperMethods() {}

    // 环绕通知记录耗时
    @Around("milvusMapperMethods()")
    public Object logPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed(); // 执行原方法
            long duration = System.currentTimeMillis() - startTime;

            // 记录基础指标
            log.info("[Milvus性能监控] 方法={}, 耗时={}ms", methodName, duration);
            // 可扩展：推送到监控系统（如Prometheus）
            // Metrics.timer("milvus_operation", "method", methodName).record(duration, TimeUnit.MILLISECONDS);

            return result;
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[Milvus性能监控] 方法={} 执行失败, 耗时={}ms, 错误={}",
                    methodName, duration, e.getMessage());
            throw e;
        }
    }
}
