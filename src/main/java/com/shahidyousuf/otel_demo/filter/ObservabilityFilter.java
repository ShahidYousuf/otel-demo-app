package com.shahidyousuf.otel_demo.filter;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Context;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
public class ObservabilityFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(ObservabilityFilter.class);
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String REQUEST_ID_MDC = "request_id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = getOrCreateRequestId(request);
        MDC.put(REQUEST_ID_MDC, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        Baggage baggage = Baggage.current()
                .toBuilder()
                .put("request.id", requestId)
                .build();
        Context contextWithBaggage = baggage.storeInContext(Context.current());

        long startTime = System.currentTimeMillis();
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, 1024 * 1024);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try (var scope = contextWithBaggage.makeCurrent()) {
            logger.info("Incoming request", 
                kv("method", request.getMethod()),
                kv("uri", request.getRequestURI()),
                kv("query_string", request.getQueryString()),
                kv("request_id", requestId));
            
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Outgoing response",
                kv("method", request.getMethod()),
                kv("uri", request.getRequestURI()),
                kv("status", wrappedResponse.getStatus()),
                kv("duration_ms", duration),
                kv("request_id", requestId));
            
            wrappedResponse.copyBodyToResponse();
            MDC.remove(REQUEST_ID_MDC);
        }
    }

    private String getOrCreateRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        return requestId;
    }
}
