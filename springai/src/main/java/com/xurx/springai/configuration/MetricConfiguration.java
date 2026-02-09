package com.xurx.springai.configuration;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MetricConfiguration {

    private final MeterRegistry registry;

    // 启动时注册 bean
    @PostConstruct
    public void init() {
        Metrics.addRegistry(registry);
    }
}