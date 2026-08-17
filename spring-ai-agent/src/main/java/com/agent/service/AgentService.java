package com.agent.service;


import com.agent.tool.DateTimeTool;
import com.agent.tool.TradeOrderTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
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
                        ChatMemory chatMemory,
                        DateTimeTool dateTimeTool,
                        TradeOrderTool tradeOrderTool) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        你是一个严谨、友好的中文 AI 助手。
                        请优先使用中文回答用户问题。
                        如果不知道答案，请明确说明不知道，不要编造信息。
                        当用户询问订单详情、订单状态或物流信息时，必须调用订单查询工具，不得根据经验猜测。
                        如果用户提供订单编号，使用订单编号查询工具。
                        如果用户没有提供订单编号，但提供了客户姓名，使用客户姓名查询工具。
                        如果订单查询工具没有查询到数据，请如实告知用户，不得编造订单信息。
                        """)
                //增加记忆功能
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(dateTimeTool, tradeOrderTool)
                .build();
    }

    /**
     * 向大模型发送用户问题。
     *
     * @param conversationId 会话id
     * @param message 用户输入的问题
     * @return 大模型生成的文本
     */
    public String chat(String conversationId, String message) {
        if (conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("会话ID不能为空");
        }
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
