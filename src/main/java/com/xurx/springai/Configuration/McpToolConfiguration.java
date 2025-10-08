package com.xurx.springai.Configuration;


import com.xurx.springai.Tool.MathTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolConfiguration {

    @Bean
    public ToolCallbackProvider mathTool() {
        return MethodToolCallbackProvider.builder().toolObjects(new MathTool()).build();
    }
}
