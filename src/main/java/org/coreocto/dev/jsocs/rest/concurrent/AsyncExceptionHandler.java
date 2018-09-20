package org.coreocto.dev.jsocs.rest.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;

public class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(AsyncExceptionHandler.class);

    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... obj) {

        logger.debug("Exception Cause - " + throwable.getMessage());
        logger.debug("Method name - " + method.getName());
        for (Object param : obj) {
            logger.debug("Parameter value - " + param);
        }
    }
}
