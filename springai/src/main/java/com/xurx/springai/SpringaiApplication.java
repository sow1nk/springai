package com.xurx.springai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(exclude = {
		com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeEmbeddingAutoConfiguration.class
})
@ConfigurationPropertiesScan
public class SpringaiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringaiApplication.class, args);
	}

}
