package com.xurx.springai.Configuration;

import com.xurx.springai.Tool.MathTool;
import com.xurx.springai.Tool.MySQLTool;
import com.xurx.springai.Tool.WeatherTool;
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
    public WeatherTool weatherTool() {
        return new WeatherTool();
    }

    /**
     * Tool 回调提供者，注册所有的 Tool 实例
     * @param mathTool 数学工具
     * @param mySQLTool 数据库工具
     * @return
     */
    @Bean
    public ToolCallbackProvider toolCallbackProvider(MathTool mathTool,
                                                     MySQLTool mySQLTool,
                                                     WeatherTool weatherTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(mathTool, mySQLTool, weatherTool)
                .build();
    }
}
