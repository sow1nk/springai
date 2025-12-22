package com.coffee.springaimcpserver.config;

import com.coffee.springaimcpserver.tool.DateTimeTools;
import com.coffee.springaimcpserver.tool.MathTool;
import com.coffee.springaimcpserver.tool.MySQLTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {

    @Bean
    public ToolCallbackProvider mcpToolCallbackProvider(
            MathTool mathTool,
            MySQLTool mySQLTool,
            DateTimeTools dateTimeTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(mathTool, mySQLTool, dateTimeTools)
                .build();
    }
}
