package com.agent.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 为 Agent 提供日期和时间查询能力。
 */
@Component
public class DateTimeTool {

    private static final Logger log =
            LoggerFactory.getLogger(DateTimeTool.class);

    private static final ZoneId BEIJING_ZONE_ID =
            ZoneId.of("Asia/Shanghai");

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern(
                    "yyyy年MM月dd日 HH:mm:ss EEEE",
                    Locale.SIMPLIFIED_CHINESE
            );

    /**
     * 获取当前北京时间。
     *
     * 当模型认为用户在询问当前日期、时间或星期时，
     * 可以主动调用这个 Java 方法。
     */
    @Tool(
            name = "getCurrentBeijingTime",
            description = "获取当前准确的北京时间、日期和星期。当用户询问现在几点、今天日期或者今天星期几时，必须调用这个工具"
    )
    public String getCurrentBeijingTime() {
        log.info("Agent 正在调用工具：getCurrentBeijingTime");
        ZonedDateTime now = ZonedDateTime.now(BEIJING_ZONE_ID);
        return "当前北京时间是：" + now.format(FORMATTER);
    }
}
