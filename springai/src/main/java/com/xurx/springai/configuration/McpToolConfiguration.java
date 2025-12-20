package com.xurx.springai.configuration;

import com.xurx.springai.tool.DateTimeTools;
import com.xurx.springai.tool.MathTool;
import com.xurx.springai.tool.MySQLTool;
import com.xurx.springai.tool.WeatherTool;
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

    @Bean
    public DateTimeTools dateTimeTools(){
        return new DateTimeTools();
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
                                                     WeatherTool weatherTool,
                                                     DateTimeTools dateTimeTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(
                        mathTool,
                        mySQLTool,
                        weatherTool,
                        dateTimeTools
                )
                .build();
    }
}
