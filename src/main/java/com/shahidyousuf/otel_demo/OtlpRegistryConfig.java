package com.shahidyousuf.otel_demo;

import io.micrometer.core.instrument.Clock;
import io.micrometer.registry.otlp.OtlpConfig;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Configuration
public class OtlpRegistryConfig {
    private static final Logger log = LoggerFactory.getLogger(OtlpRegistryConfig.class);

    @Bean
    @ConditionalOnMissingBean(OtlpMeterRegistry.class)
    OtlpMeterRegistry otlpMeterRegistry(Environment env, Clock clock) {
        // Resolve URL from either property namespace, fallback to Grafana Cloud gateway if present in env
        String url = env.getProperty("management.otlp.metrics.export.url",
                env.getProperty("management.metrics.export.otlp.url", ""));

        // Collect Authorization header from either dotted or bracket syntax
        String authHeader = Optional.ofNullable(env.getProperty("management.otlp.metrics.export.headers.Authorization"))
                .orElse(env.getProperty("management.otlp.metrics.export.headers[Authorization]"));
        if (authHeader == null) {
            authHeader = Optional.ofNullable(env.getProperty("management.metrics.export.otlp.headers.Authorization"))
                    .orElse(env.getProperty("management.metrics.export.otlp.headers[Authorization]"));
        }

        Map<String, String> headers = new HashMap<>();
        if (authHeader != null && !authHeader.isBlank()) {
            headers.put("Authorization", authHeader);
        }

        String stepProp = Optional.ofNullable(env.getProperty("management.otlp.metrics.export.step"))
                .orElse(env.getProperty("management.metrics.export.otlp.step"));
        Duration computedStep = null;
        if (stepProp != null) {
            // support Spring-style "10s" by simple parsing
            try {
                if (stepProp.endsWith("ms")) {
                    computedStep = Duration.ofMillis(Long.parseLong(stepProp.replace("ms", "").trim()));
                } else if (stepProp.endsWith("s")) {
                    computedStep = Duration.ofSeconds(Long.parseLong(stepProp.replace("s", "").trim()));
                } else if (stepProp.endsWith("m")) {
                    computedStep = Duration.ofMinutes(Long.parseLong(stepProp.replace("m", "").trim()));
                } else {
                    computedStep = Duration.parse(stepProp); // attempt ISO-8601
                }
            } catch (Exception ignored) {
                // leave null to use Micrometer default
            }
        }

        final Duration stepFinal = computedStep;
        OtlpConfig config = new OtlpConfig() {
            @Override
            public String get(String k) { return null; }

            @Override
            public String url() { return url; }

            @Override
            public Duration step() { return stepFinal != null ? stepFinal : OtlpConfig.super.step(); }

            @Override
            public Map<String, String> headers() { return headers; }
        };

        log.info("Creating OtlpMeterRegistry bean (url={})", url);
        return new OtlpMeterRegistry(config, clock);
    }
}
