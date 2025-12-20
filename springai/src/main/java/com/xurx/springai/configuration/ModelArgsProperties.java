package com.xurx.springai.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Data
@ConfigurationProperties(prefix = "model-args")
public class ModelArgsProperties {
    private Map<String, ModelCfg> models;

    @Data
    public static class ModelCfg {
        private String name;
        private String apiKey;
        private String baseUrl;
        private Integer dimensions;
    }

    public ModelCfg getModelCfg(String key) {
        return models.get(key);
    }
}
