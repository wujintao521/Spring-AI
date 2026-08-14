package com.agent.service;


import com.agent.tool.DateTimeTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AgentService {

    /**
     * Spring AI 提供的高级聊天客户端。
     */
    private final ChatClient chatClient;

    /**
     * 通过构造方法注入 ChatClient.Builder。
     *
     * Spring AI 自动配置会根据 application.yml，
     * 自动创建 ChatClient.Builder 并放入 Spring 容器。
     */
    public AgentService(ChatClient.Builder chatClientBuilder,
                        DateTimeTool dateTimeTool) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        你是一个严谨、友好的中文 AI 助手。
                        请优先使用中文回答用户问题。
                        如果不知道答案，请明确说明不知道，不要编造信息。
                        """)
                .defaultTools(dateTimeTool)
                .build();
    }

    /**
     * 向大模型发送用户问题。
     *
     * @param message 用户输入的问题
     * @return 大模型生成的文本
     */
    public String chat(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("用户问题不能为空");
        }

        return chatClient
                .prompt()
                .user(message)
                .call()
                .content();
    }
}
