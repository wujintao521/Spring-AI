package com.agent.service;

import com.agent.enums.OrderStatusEnum;
import com.agent.exception.BusinessException;
import com.agent.response.OrderCancellationWorkflowResponse;
import com.agent.response.TradeOrderQueryResult;
import com.agent.tool.ReadOnlyRetryMcpToolCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OrderCancellationWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(OrderCancellationWorkflowService.class);

    private final McpToolCatalog toolCatalog;

    private final CurrentUserService currentUserService;

    private final JsonMapper jsonMapper = JsonMapper.builder()
            .enable(
                    DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,
                    DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
            .build();

    public OrderCancellationWorkflowService(
            McpToolCatalog toolCatalog,
            CurrentUserService currentUserService) {
        this.toolCatalog = toolCatalog;
        this.currentUserService = currentUserService;
    }

    public OrderCancellationWorkflowResponse check(String orderNo) {
        // 确认当前请求已经通过用户认证。
        // 真正发往MCP的用户身份仍由现有配置自动传递。
        currentUserService.requireUserId();
        if (orderNo == null || orderNo.isBlank()) {
            throw new IllegalArgumentException("订单编号不能为空");
        }

        String normalizedOrderNo = orderNo.strip();

        // 本次流程使用同一份工具清单。
        List<ToolCallback> tools = toolCatalog.currentTools();
        List<String> completedSteps = new ArrayList<>();

        // 第一步：查询真实订单。
        log.info("取消资格工作流开始步骤1：查询订单");

        String orderText = callTextTool(
                tools,
                "queryTradeOrderForWorkflow",
                Map.of("orderNo", normalizedOrderNo));

        TradeOrderQueryResult order = readOrder(orderText, normalizedOrderNo);

        completedSteps.add("QUERY_ORDER");

        if (!order.found()) {
            // Java代码决定提前结束。
            // 不会调用下面的取消资格判断工具。
            log.info("取消资格工作流结束：未查到可访问订单");

            return new OrderCancellationWorkflowResponse(
                    false,
                    normalizedOrderNo,
                    "UNKNOWN",
                    "未查询到可访问的订单，无法判断取消资格。",
                    List.copyOf(completedSteps));
        }

        // 第二步：使用本轮查询得到的真实状态判断取消资格。
        log.info("取消资格工作流开始步骤2：判断取消资格");

        String eligibility = callTextTool(
                tools,
                "checkTradeOrderCancellationEligibility",
                Map.of("orderStatus", order.status()));

        completedSteps.add("CHECK_CANCELLATION_ELIGIBILITY");

        return new OrderCancellationWorkflowResponse(
                true,
                order.orderNo(),
                order.status(),
                eligibility,
                List.copyOf(completedSteps));
    }

    /**
     * 调用已经发现的工具回调，提取文本结果。
     */
    private String callTextTool(List<ToolCallback> tools, String toolName, Map<String, Object> arguments) {
        ToolCallback callback = findTool(tools, toolName);
        // 调用的是现有回调，保留身份传递和已配置的重试。
        // 工具执行异常继续向外抛出，由GlobalExceptionHandler处理。
        String rawResult = callback.call(jsonMapper.writeValueAsString(arguments));
        // 当前SyncMcpToolCallback返回content列表的JSON。
        JsonNode contents;
        try {
            contents = jsonMapper.readTree(rawResult);
        } catch (RuntimeException exception) {
            log.error("工作流工具结果不是有效JSON，toolName={}", toolName, exception);
            throw invalidResult();
        }
        // 本节两个工具都约定返回一个文本内容块。
        if (contents == null || !contents.isArray() || contents.size() != 1) {
            throw invalidResult();
        }
        JsonNode item = contents.get(0);
        if (item == null || !item.isObject()) {
            throw invalidResult();
        }
        JsonNode type = item.get("type");
        JsonNode text = item.get("text");
        if (type == null || !type.isString() || !"text".equals(type.asString()) || text == null || !text.isString() || text.asString().isBlank()) {
            throw invalidResult();
        }
        return text.asString();
    }

    /**
     * 根据MCP服务端的原始工具名，查找对应回调。
     */
    private ToolCallback findTool(List<ToolCallback> tools, String toolName) {
        for (ToolCallback callback : tools) {
            String originalName;
            if (callback instanceof ReadOnlyRetryMcpToolCallback retryCallback) {
                originalName = retryCallback.getOriginalToolName();
            } else if (callback instanceof SyncMcpToolCallback mcpCallback) {
                originalName = mcpCallback.getOriginalToolName();
            } else {
                originalName = callback.getToolDefinition().name();
            }
            if (toolName.equals(originalName)) {
                return callback;
            }
        }

        throw new BusinessException(50303, "工作流所需的远程工具尚未就绪，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE);
    }

    /**
     * 将订单查询工具返回的JSON文本转成Java对象，并检查关键字段。
     */
    private TradeOrderQueryResult readOrder(String text, String expectedOrderNo) {
        TradeOrderQueryResult order;
        try {
            order = jsonMapper.readValue(text, TradeOrderQueryResult.class);
        } catch (RuntimeException exception) {
            log.error("工作流订单结果无法解析", exception);
            throw invalidResult();
        }
        if (order == null || !expectedOrderNo.equals(order.orderNo())) {
            throw invalidResult();
        }
        if (order.found()) {
            if (order.status() == null) {
                throw invalidResult();
            }
            try {
                // 未知状态不能继续传给资格判断工具。
                OrderStatusEnum.valueOf(order.status());
            } catch (IllegalArgumentException exception) {
                throw invalidResult();
            }
        }
        return order;
    }

    private BusinessException invalidResult() {
        return new BusinessException(50203, "远程工具返回的数据格式不符合工作流要求", HttpStatus.BAD_GATEWAY);
    }
}