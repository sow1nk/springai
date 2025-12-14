package com.xurx.springai.Tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;

@Slf4j
public class MathTool {

    @Tool(description = "计算两个整数的和，返回 a + b 的结果")
    public Integer add(Integer a, Integer b) {
        log.info("加法计算 {} and {}", a, b);
        return a + b;
    }

    @Tool(description = "计算两个整数的差，返回 a - b 的结果")
    public Integer subtract(Integer a, Integer b) {
        log.info("减法计算 {} - {}", a, b);
        return a - b;
    }

    @Tool(description = "计算两个整数的乘积，返回 a × b 的结果")
    public Integer multiply(Integer a, Integer b) {
        log.info("乘法计算 {} * {}", a, b);
        return a * b;
    }

    @Tool(description = "计算两个整数的商，返回 a ÷ b 的结果（除数不能为0）")
    public Integer divide(Integer a, Integer b) {
        if (b == 0) {
            log.error("除数不能为零");
            throw new IllegalArgumentException("Division by zero is not allowed.");
        }
        log.info("除法计算 {} / {}", a, b);
        return a / b;
    }
}
