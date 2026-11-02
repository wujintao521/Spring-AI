package com.agent.mcp.tool;

import io.modelcontextprotocol.common.McpTransportContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class TimeMcpTool {

    private static final Logger log = LoggerFactory.getLogger(TimeMcpTool.class);

    @McpTool(
            name = "getCurrentTime",
            description = "查询当前北京时间，返回日期、时间和时区。"
    )
    public String getCurrentTime(McpTransportContext context) {
        // context由框架注入，不由模型填写。
        Object userId = context.get("userId");
        log.info("MCP时间查询工具被调用，userId={}", userId);
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
        return now.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
    }

    @McpTool(
            name = "getTimeByZone",
            description = "查询指定时区的当前日期和时间。当用户询问其他城市或时区的当前时间时使用。"
    )
    public String getTimeByZone(
            @McpToolParam(
                    description = "IANA时区标识，例如东京Asia/Tokyo、纽约America/New_York、北京Asia/Shanghai。",
                    required = true
            ) String zoneId) {

        if (zoneId == null || zoneId.isBlank()) {
            throw new IllegalArgumentException("时区不能为空");
        }
        ZoneId targetZone = ZoneId.of(zoneId.strip());
        log.info("MCP指定时区查询工具被调用，zoneId={}", targetZone.getId());
        ZonedDateTime now = ZonedDateTime.now(targetZone);
        return now.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
    }
}