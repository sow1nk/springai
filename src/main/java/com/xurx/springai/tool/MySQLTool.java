package com.xurx.springai.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;


@Slf4j
@RequiredArgsConstructor
public class MySQLTool {

    private final JdbcTemplate jdbcTemplate;

    @Tool(description = "获取当前数据库中的所有表名。")
    public List<String> listTables() {
        String sql = "SHOW TABLES";
        log.info("正在执行 SQL 语句: {}", sql);
        try {
            return jdbcTemplate.queryForList(sql, String.class);
        } catch (DataAccessException ex) {
            log.error("获取表名失败", ex);
            throw new IllegalStateException("获取表名失败: " + ex.getMessage(), ex);
        }
    }

    @Tool(description = "执行只读 SQL 查询并返回结果列表，仅支持以 SELECT 开头的语句。")
    public List<Map<String, Object>> executeSelect(String sql) {
        // TODO 查询条件字段的完善
        log.info("SQL 查询语句: {}", sql);
        if (!StringUtils.hasText(sql)) {
            throw new IllegalArgumentException("SQL 语句不能为空。");
        }

        String sanitizedSql = sql.trim();
        if (!sanitizedSql.toLowerCase(Locale.ROOT).startsWith("select")) {
            throw new IllegalArgumentException("只允许执行 SELECT 语句。");
        }

        log.info("正在执行 SQL 查询: {}", sanitizedSql);
        try {
            return jdbcTemplate.queryForList(sanitizedSql);
        } catch (DataAccessException ex) {
            log.error("SQL 查询执行失败", ex);
            throw new IllegalStateException("SQL 查询执行失败: " + ex.getMessage(), ex);
        }
    }

    @Tool(description = "更新数据库中的数据，仅支持以 UPDATE、INSERT 或 DELETE 开头的语句。")
    public int executeUpdate(String sql) {
        log.info("SQL 更新语句: {}", sql);
        if (!StringUtils.hasText(sql)) {
            throw new IllegalArgumentException("SQL 语句不能为空。");
        }

        String sanitizedSql = sql.trim();
        String lowerSql = sanitizedSql.toLowerCase(Locale.ROOT);
        if (!(lowerSql.startsWith("update") || lowerSql.startsWith("insert") || lowerSql.startsWith("delete"))) {
            throw new IllegalArgumentException("只允许执行 UPDATE、INSERT 或 DELETE 语句。");
        }

        log.info("正在执行 SQL 更新: {}", sanitizedSql);
        try {
            return jdbcTemplate.update(sanitizedSql);
        } catch (DataAccessException ex) {
            log.error("SQL 更新执行失败", ex);
            throw new IllegalStateException("SQL 更新执行失败: " + ex.getMessage(), ex);
        }
    }
}
