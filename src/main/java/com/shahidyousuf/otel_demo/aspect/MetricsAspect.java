package com.shahidyousuf.otel_demo.aspect;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class MetricsAspect {
    private final MeterRegistry registry;

    public MetricsAspect(MeterRegistry registry) {
        this.registry = registry;
    }

    @Around("@annotation(com.shahidyousuf.otel_demo.annotation.TrackMetrics)")
    public Object trackMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
        String endpoint = extractEndpoint(joinPoint);
        Timer.Sample sample = Timer.start(registry);
        Counter counter = Counter.builder("endpoint.requests")
                .tag("endpoint", endpoint)
                .description("Number of requests per endpoint")
                .register(registry);
        
        Counter errorCounter = Counter.builder("endpoint.errors")
                .tag("endpoint", endpoint)
                .description("Number of errors per endpoint")
                .register(registry);

        try {
            Object result = joinPoint.proceed();
            counter.increment();
            return result;
        } catch (ResponseStatusException e) {
            errorCounter.increment();
            throw e;
        } catch (Exception e) {
            errorCounter.increment();
            throw e;
        } finally {
            sample.stop(Timer.builder("endpoint.latency")
                    .tag("endpoint", endpoint)
                    .description("Latency per endpoint")
                    .register(registry));
        }
    }

    private String extractEndpoint(ProceedingJoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        if (className.equals("HelloController")) {
            return "/hello";
        } else if (className.equals("WorkController")) {
            return "/work";
        }
        return "/" + methodName.toLowerCase();
    }
}
