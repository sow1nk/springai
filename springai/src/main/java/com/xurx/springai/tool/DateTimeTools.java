package com.xurx.springai.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.LocalDateTime;

@Slf4j
public class DateTimeTools {

    @Tool(description = "Get the current date and time in the user's timezone")
    public String getCurrentDateTime() {
        log.info("获取当前日期和时间");
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }

}