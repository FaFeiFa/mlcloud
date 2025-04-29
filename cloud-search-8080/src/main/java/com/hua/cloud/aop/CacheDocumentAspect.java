package com.hua.cloud.aop;

import com.hua.cloud.aop.annotation.CacheDocument;
import com.hua.cloud.entities.Document;
import com.hua.cloud.service.DocumentCacheService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;
import java.util.List;

@Slf4j
@Aspect
@Component
public class CacheDocumentAspect {

    @Resource
    private DocumentCacheService documentCacheService;

    @Around("@annotation(com.hua.cloud.aop.annotation.CacheDocument)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        CacheDocument cacheDocument = method.getAnnotation(CacheDocument.class);

        Object[] args = joinPoint.getArgs();
        if (args.length == 0 || !(args[0] instanceof String)) {
            throw new IllegalArgumentException("使用@CacheDocument的方法，要求第一个参数是String类型的问题key！");
        }
        String question = (String) args[0];

        List<String> idList = documentCacheService.getIdList(question);
        if (idList != null && !idList.isEmpty()) {
            List<Document> documents = documentCacheService.getDocumentsByIds(idList);
            if (documents != null && !documents.isEmpty()) {
                log.info("Cache hit for question: {}", question);
                return documents;
            }
        }

        Object result = joinPoint.proceed();

        if (result instanceof List<?>) {
            List<?> resultList = (List<?>) result;
            if (!resultList.isEmpty() && resultList.get(0) instanceof Document) {
                @SuppressWarnings("unchecked")
                List<Document> documents = (List<Document>) resultList;
                documentCacheService.cacheDocumentsAndIds(question, documents);
                log.info("Cache populated for question: {}", question);
            }
        }

        return result;
    }



}
