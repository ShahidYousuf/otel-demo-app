package com.shahidyousuf.otel_demo.service;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
public class BusinessService {
    private static final Logger logger = LoggerFactory.getLogger(BusinessService.class);
    private final Tracer tracer;

    public BusinessService(Tracer tracer) {
        this.tracer = tracer;
    }

    public String performWork(long ms) throws InterruptedException {
        Span workSpan = tracer.spanBuilder("business.work")
                .setAttribute("work.duration_ms", ms)
                .startSpan();
        
        try (Scope scope = workSpan.makeCurrent()) {
            logger.info("Starting business work", kv("duration_ms", ms));
            
            String requestId = Baggage.current().getEntryValue("request.id");
            if (requestId != null) {
                workSpan.setAttribute("request.id", requestId);
                logger.info("Request ID from baggage", kv("request_id", requestId));
            }
            
            Thread.sleep(ms);
            
            workSpan.setAttribute("work.completed", true);
            logger.info("Business work completed", kv("duration_ms", ms));
            
            return "Work completed successfully";
        } catch (InterruptedException e) {
            workSpan.recordException(e);
            workSpan.setAttribute("work.completed", false);
            throw e;
        } finally {
            workSpan.end();
        }
    }
}
