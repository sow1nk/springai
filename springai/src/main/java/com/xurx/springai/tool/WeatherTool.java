package com.xurx.springai.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

@Slf4j
public class WeatherTool {

    @Tool(description = "获取指定城市的实时天气信息，当用户询问任何城市的天气时，必须调用此工具。")
    public String getCurrentWeather(@ToolParam(description = "城市名称，例如：北京、上海、广州") String city) {
        log.info("获取城市 {} 的当前天气信息", city);
        return "晴朗";
    }
}
