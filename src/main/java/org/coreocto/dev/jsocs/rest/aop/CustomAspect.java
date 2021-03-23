package org.coreocto.dev.jsocs.rest.aop;

import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.coreocto.dev.jsocs.rest.pojo.RequestEntry;
import org.coreocto.dev.jsocs.rest.repo.RequestRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.session.SessionRepository;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Date;

@Aspect
@Component
public class CustomAspect {

    @Autowired
    private RequestRepo requestRepo;

    private final Logger logger = LoggerFactory.getLogger(CustomAspect.class);

    @Around(value = "@annotation(LogRequest)")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {

        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        String requestUri = null;
        String queryStr = null;

        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof HttpServletRequestWrapper) {
                HttpServletRequestWrapper requestWrapper = (HttpServletRequestWrapper) arg;
                requestUri = requestWrapper.getRequestURI();
                queryStr = requestWrapper.getQueryString();
                break;
            }
        }

        if (requestUri != null && queryStr != null && !queryStr.isEmpty()) {
            requestUri += "?" + queryStr;
        }

        RequestEntry req = new RequestEntry();
        req.setCrequesturi(requestUri);
        req.setCcrtdt(new Date());
        req.setChandler(className + "." + methodName);
        requestRepo.save(req);

        Object proceed = joinPoint.proceed(args);

        if (proceed instanceof String) {
            req.setCresponse((String) proceed);
        }

        req.setCupddt(new Date());
//        req.setCresponse(output);
        requestRepo.save(req);

        return proceed;
    }
}
