package com.xurx.springai.Configuration;

import com.xurx.springai.Tool.MathTool;
import com.xurx.springai.Tool.MySQLTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class McpToolConfiguration {

    @Bean
    public MathTool mathTool() {
        return new MathTool();
    }

    @Bean
    public MySQLTool mySQLTool(JdbcTemplate jdbcTemplate) {
        return new MySQLTool(jdbcTemplate);
    }

    @Bean
    public ToolCallbackProvider toolCallbackProvider(MathTool mathTool, MySQLTool mySQLTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(mathTool, mySQLTool)
                .build();
    }
}
