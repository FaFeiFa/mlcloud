package com.hua.cloud.log;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ServiceLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(ServiceLoggingAspect.class);

    @Before("execution(* com.hua.cloud.service.*.*(..))")
    public void beforeServiceMethod() {
        // 在请求开始时记录日志
        logger.info("Service method started.");
    }

    @Around("execution(* com.hua.cloud.service.*.*(..))")
    public Object logServiceMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法名和参数
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        // 记录方法调用的参数
        logger.info("Calling method: {} with args: {}", methodName, args);

        // 执行目标方法
        Object result = joinPoint.proceed();

        // 记录方法执行后的结果
        logger.info("Method: {} executed with result: {}", methodName, result);

        return result;
    }

    @After("execution(* com.hua.cloud.service.*.*(..))")
    public void afterServiceMethod() {
        // 在方法结束时记录日志
        logger.info("Service method finished.");
    }
}
