package com.shahidyousuf.otel_demo;

import com.shahidyousuf.otel_demo.annotation.TrackMetrics;
import com.shahidyousuf.otel_demo.dto.HelloResponse;
import io.opentelemetry.api.baggage.Baggage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static net.logstash.logback.argument.StructuredArguments.kv;

@RestController
public class HelloController {

    private static final Logger logger = LoggerFactory.getLogger(HelloController.class);

    @GetMapping(value = "/hello", produces = MediaType.APPLICATION_JSON_VALUE)
    @TrackMetrics
    public HelloResponse hello() {
        long timestamp = System.currentTimeMillis();
        String requestId = MDC.get("request_id");
        if (requestId == null) {
            requestId = Baggage.current().getEntryValue("request.id");
        }

        logger.info("hello endpoint invoked", kv("endpoint", "/hello"), kv("timestamp", timestamp), kv("request_id", requestId));
        return new HelloResponse("Hello", timestamp, requestId);
    }
}
