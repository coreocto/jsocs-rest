package org.coreocto.dev.jsocs.rest.filter;

import org.coreocto.dev.jsocs.rest.repo.RequestRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
@Order(1)
public class RequestFilter implements Filter {

    private final Logger logger = LoggerFactory.getLogger(RequestFilter.class);

    @Autowired
    RequestRepo requestRepo;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {
            HttpServletRequest req = (HttpServletRequest) request;

            logger.debug("Starting a transaction for req : {}", req.getRequestURI());

            // TODO: despite it does not throw any errors, the code below does not actually write data to the database
            // TODO: need resolve this problem
//            RequestEntry entry = new RequestEntry();
//            entry.setCcrtdt(Calendar.getInstance().getTime());
//            entry.setCrequesturi(req.getRequestURI());
//            requestRepo.save(entry);

            chain.doFilter(request, response);

//            logger.debug("Committing a transaction for req : {}", req.getRequestURI());
        }

    }

    @Override
    public void destroy() {

    }
}
