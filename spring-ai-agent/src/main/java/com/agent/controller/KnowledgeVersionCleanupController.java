package com.agent.controller;

import com.agent.entity.KnowledgeVersionCleanup;
import com.agent.request.KnowledgeCleanupExecutionRequest;
import com.agent.response.ApiResponse;
import com.agent.service.KnowledgeVersionCleanupService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 旧版本清理教学管理接口。
 * 当前用于本地调试，上线前必须增加管理员鉴权。
 */
@RestController
@RequestMapping("/api/knowledge-cleanup")
public class KnowledgeVersionCleanupController {

    /**
     * 旧版本清理服务。
     */
    private final KnowledgeVersionCleanupService knowledgeVersionCleanupService;

    /**
     * 注入清理服务。
     */
    public KnowledgeVersionCleanupController(KnowledgeVersionCleanupService knowledgeVersionCleanupService) {
        this.knowledgeVersionCleanupService = knowledgeVersionCleanupService;
    }

    /**
     * 查看任务状态。
     */
    @GetMapping("/{taskId}")
    public ApiResponse<KnowledgeVersionCleanup> getTask(@PathVariable("taskId") Long taskId) {
        return ApiResponse.success(knowledgeVersionCleanupService.getTask(taskId));
    }

    /**
     * 领取任务，不执行删除。
     */
    @PostMapping("/{taskId}/claim")
    public ApiResponse<KnowledgeVersionCleanup> claimTask(@PathVariable("taskId") Long taskId) {
        return ApiResponse.success(knowledgeVersionCleanupService.claimTask(taskId));
    }

    /**
     * 使用本次执行编号清理指定旧版本。
     */
    @PostMapping("/{taskId}/execute")
    public ApiResponse<KnowledgeVersionCleanup> executeTask(
            @PathVariable("taskId") Long taskId,
            @Valid @RequestBody KnowledgeCleanupExecutionRequest request) {
        return ApiResponse.success(
                knowledgeVersionCleanupService.executeTask(taskId, request.executionToken()));
    }
}