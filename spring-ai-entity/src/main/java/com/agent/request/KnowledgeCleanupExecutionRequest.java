package com.agent.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 旧版本清理执行请求。
 *
 * @param executionToken 领取任务时返回的执行编号
 */
public record KnowledgeCleanupExecutionRequest(
        @NotBlank(message = "执行编号不能为空") String executionToken) {
}