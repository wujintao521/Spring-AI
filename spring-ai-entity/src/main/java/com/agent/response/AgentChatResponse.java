package com.agent.response;

/**
 * Agent 返回给调用方的聊天响应。
 * * 响应 JSON 示例：
 * {
 *   "answer": "Spring AI 是一个用于开发 AI 应用的 Spring 项目。"
 * }
 *
 * @param answer 大模型生成的回答
 */
public record AgentChatResponse(String answer) {
}