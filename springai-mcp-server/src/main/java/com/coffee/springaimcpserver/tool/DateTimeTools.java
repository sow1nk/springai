package com.coffee.springaimcpserver.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
public class DateTimeTools {

    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Tool(description = "获取当前日期和时间，返回格式: yyyy-MM-dd HH:mm:ss")
    public String getCurrentDateTime() {
        log.info("获取当前日期和时间");
        LocalDateTime now = LocalDateTime.now();
        return now.format(DEFAULT_FORMATTER);
    }

    @Tool(description = "获取当前日期，返回格式: yyyy-MM-dd")
    public String getCurrentDate() {
        log.info("获取当前日期");
        LocalDate today = LocalDate.now();
        return today.format(DATE_FORMATTER);
    }

    @Tool(description = "获取当前时间，返回格式: HH:mm:ss")
    public String getCurrentTime() {
        log.info("获取当前时间");
        LocalTime now = LocalTime.now();
        return now.format(TIME_FORMATTER);
    }

    @Tool(description = "获取当前时间戳（毫秒）")
    public long getCurrentTimestamp() {
        log.info("获取当前时间戳");
        return System.currentTimeMillis();
    }

    @Tool(description = "获取指定时区的当前日期和时间")
    public String getDateTimeInTimezone(
            @ToolParam(description = "时区ID，例如: Asia/Shanghai, America/New_York, Europe/London") String zoneId) {
        log.info("获取时区 {} 的当前时间", zoneId);
        try {
            ZoneId zone = ZoneId.of(zoneId);
            ZonedDateTime zonedDateTime = ZonedDateTime.now(zone);
            return zonedDateTime.format(DEFAULT_FORMATTER) + " (" + zoneId + ")";
        } catch (DateTimeException e) {
            log.error("无效的时区ID: {}", zoneId);
            throw new IllegalArgumentException("无效的时区ID: " + zoneId);
        }
    }

    @Tool(description = "计算两个日期之间的天数差")
    public long daysBetween(
            @ToolParam(description = "起始日期，格式: yyyy-MM-dd") String startDate,
            @ToolParam(description = "结束日期，格式: yyyy-MM-dd") String endDate) {
        log.info("计算日期差: {} 到 {}", startDate, endDate);
        try {
            LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
            LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);
            return ChronoUnit.DAYS.between(start, end);
        } catch (DateTimeParseException e) {
            log.error("日期格式错误: {} 或 {}", startDate, endDate);
            throw new IllegalArgumentException("日期格式必须为 yyyy-MM-dd");
        }
    }

    @Tool(description = "在指定日期上增加或减少天数")
    public String addDays(
            @ToolParam(description = "基准日期，格式: yyyy-MM-dd") String date,
            @ToolParam(description = "要增加的天数，负数表示减少") int days) {
        log.info("日期 {} 增加 {} 天", date, days);
        try {
            LocalDate baseDate = LocalDate.parse(date, DATE_FORMATTER);
            LocalDate resultDate = baseDate.plusDays(days);
            return resultDate.format(DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            log.error("日期格式错误: {}", date);
            throw new IllegalArgumentException("日期格式必须为 yyyy-MM-dd");
        }
    }

    @Tool(description = "在指定日期上增加或减少月数")
    public String addMonths(
            @ToolParam(description = "基准日期，格式: yyyy-MM-dd") String date,
            @ToolParam(description = "要增加的月数，负数表示减少") int months) {
        log.info("日期 {} 增加 {} 个月", date, months);
        try {
            LocalDate baseDate = LocalDate.parse(date, DATE_FORMATTER);
            LocalDate resultDate = baseDate.plusMonths(months);
            return resultDate.format(DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            log.error("日期格式错误: {}", date);
            throw new IllegalArgumentException("日期格式必须为 yyyy-MM-dd");
        }
    }

    @Tool(description = "将时间戳转换为日期时间字符串")
    public String timestampToDateTime(
            @ToolParam(description = "时间戳（毫秒）") long timestamp) {
        log.info("转换时间戳: {}", timestamp);
        try {
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(timestamp),
                    ZoneId.systemDefault()
            );
            return dateTime.format(DEFAULT_FORMATTER);
        } catch (DateTimeException e) {
            log.error("无效的时间戳: {}", timestamp);
            throw new IllegalArgumentException("无效的时间戳: " + timestamp);
        }
    }

    @Tool(description = "将日期时间字符串转换为时间戳")
    public long dateTimeToTimestamp(
            @ToolParam(description = "日期时间字符串，格式: yyyy-MM-dd HH:mm:ss") String dateTime) {
        log.info("转换日期时间到时间戳: {}", dateTime);
        try {
            LocalDateTime ldt = LocalDateTime.parse(dateTime, DEFAULT_FORMATTER);
            return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            log.error("日期时间格式错误: {}", dateTime);
            throw new IllegalArgumentException("日期时间格式必须为 yyyy-MM-dd HH:mm:ss");
        }
    }

    @Tool(description = "判断指定年份是否为闰年")
    public boolean isLeapYear(
            @ToolParam(description = "年份，例如: 2024") int year) {
        log.info("判断 {} 年是否为闰年", year);
        return Year.of(year).isLeap();
    }

    @Tool(description = "获取指定日期是星期几")
    public String getDayOfWeek(
            @ToolParam(description = "日期，格式: yyyy-MM-dd") String date) {
        log.info("获取日期 {} 是星期几", date);
        try {
            LocalDate localDate = LocalDate.parse(date, DATE_FORMATTER);
            DayOfWeek dayOfWeek = localDate.getDayOfWeek();
            String[] weekDays = {"星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
            return weekDays[dayOfWeek.getValue() - 1];
        } catch (DateTimeParseException e) {
            log.error("日期格式错误: {}", date);
            throw new IllegalArgumentException("日期格式必须为 yyyy-MM-dd");
        }
    }

    @Tool(description = "格式化日期时间字符串")
    public String formatDateTime(
            @ToolParam(description = "日期时间字符串，格式: yyyy-MM-dd HH:mm:ss") String dateTime,
            @ToolParam(description = "目标格式，例如: yyyy年MM月dd日 HH时mm分ss秒") String pattern) {
        log.info("格式化日期时间: {} 为格式 {}", dateTime, pattern);
        try {
            LocalDateTime ldt = LocalDateTime.parse(dateTime, DEFAULT_FORMATTER);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return ldt.format(formatter);
        } catch (DateTimeParseException e) {
            log.error("日期时间格式错误: {}", dateTime);
            throw new IllegalArgumentException("日期时间格式必须为 yyyy-MM-dd HH:mm:ss");
        } catch (IllegalArgumentException e) {
            log.error("无效的格式模式: {}", pattern);
            throw new IllegalArgumentException("无效的格式模式: " + pattern);
        }
    }
}