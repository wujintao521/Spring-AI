package com.agent.service;


import com.agent.enums.AgentActionEnum;
import com.agent.response.AgentChatResponse;
import com.agent.response.AgentModelResponse;
import com.agent.tool.DateTimeTool;
import com.agent.tool.TradeOrderCancellationTool;
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

    private final AgentPendingActionService agentPendingActionService;

    /**
     * 通过构造方法注入 ChatClient.Builder。
     *
     * Spring AI 自动配置会根据 application.yml，
     * 自动创建 ChatClient.Builder 并放入 Spring 容器。
     */
    public AgentService(ChatClient.Builder chatClientBuilder,
                        ChatMemory chatMemory,
                        DateTimeTool dateTimeTool,
                        TradeOrderTool tradeOrderTool,
                        TradeOrderCancellationTool tradeOrderCancellationTool,
                        AgentPendingActionService agentPendingActionService) {
        this.agentPendingActionService = agentPendingActionService;
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        你是一个严谨、友好的中文 AI 助手。
                        请优先使用中文回答用户问题。
                        如果不知道答案，请明确说明不知道，不要编造信息。
                        
                        当用户询问订单详情、订单状态或物流信息时，必须调用订单查询工具，不得根据经验猜测。
                        如果用户提供订单编号，使用订单编号查询工具。
                        如果用户没有提供订单编号，但提供了客户姓名，使用客户姓名查询工具。
                        如果订单查询工具没有查询到数据，请如实告知用户，不得编造订单信息。
                        
                        当用户询问某个具体订单是否可以取消时，必须先调用订单编号查询工具获得真实订单状态，再调用订单取消资格判断工具。
                        不得根据用户描述或历史经验猜测订单状态。
                        订单取消资格判断工具只提供判断结果，不代表已经取消订单。
                        当前阶段禁止告诉用户订单已经取消。
                        
                        生成最终回答时，必须正确设置本轮业务意图：
                        GENERAL_CHAT表示普通对话；
                        ORDER_QUERY表示查询订单详情、状态或物流；
                        ORDER_CANCELLATION表示咨询或者申请取消订单。
                        
                        relatedOrderNos必须包含本轮实际涉及的全部订单编号；
                        如果本轮没有涉及订单，relatedOrderNos返回空数组。
                        不得编造没有出现在用户消息或工具查询结果中的订单编号。
                        
                        pendingAction用于保存等待用户确认的业务动作。
                        confirmationRequired表示执行pendingAction前是否需要用户确认。
                        
                        必须按照以下规则设置pendingAction和confirmationRequired：
                        
                        1. 普通聊天、订单查询，以及询问“可以取消吗”“能否取消”“能不能取消”“是否可以取消”等取消资格问题时，pendingAction必须为NONE，confirmationRequired必须为false。对于取消资格问题，只回答是否允许取消和原因，不能主动请求用户确认取消。
                        2. 只有用户明确使用“帮我取消”“我要取消”“请取消”“立即取消”等表达，明确要求执行取消操作时，才可以考虑生成CANCEL_ORDER。询问是否可以取消不属于执行请求。
                        3. 用户明确要求取消订单，但没有提供一个明确订单编号时，pendingAction必须为NONE，confirmationRequired必须为false，并在answer中询问订单编号。
                        4. 用户明确要求取消一个订单，并且取消资格判断工具返回允许取消时，pendingAction必须为CANCEL_ORDER，confirmationRequired必须为true，并在answer中请求用户确认。
                        5. 用户明确要求取消订单，但取消资格判断工具返回不允许取消时，pendingAction必须为NONE，confirmationRequired必须为false，并在answer中说明不能取消的原因。
                        6. pendingAction为NONE时，confirmationRequired只能为false。
                        7. pendingAction不为NONE时，confirmationRequired只能为true。
                        8. 即使用户表示确认，当前阶段也不能声称订单已经取消。
                        """)

                .defaultAdvisors(
                        //增加记忆功能
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .defaultTools(dateTimeTool, tradeOrderTool,tradeOrderCancellationTool)
                .build();
    }

    /**
     * 向大模型发送用户问题。
     *
     * @param conversationId 会话id
     * @param message 用户输入的问题
     * @return 大模型生成的结构化聊天结果
     */
    public AgentChatResponse chat(String conversationId, String message) {
        if (conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("会话ID不能为空");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("用户问题不能为空");
        }
        AgentModelResponse modelResponse = chatClient
                .prompt()
                .user(message)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .entity(AgentModelResponse.class);

        String actionId = null;
        if (modelResponse.pendingAction() != AgentActionEnum.NONE) {
            String orderNo = modelResponse.relatedOrderNos().get(0);
            actionId = agentPendingActionService
                    .createPendingAction(conversationId, modelResponse.pendingAction(), orderNo)
                    .getActionId();
        }
        return AgentChatResponse.from(modelResponse, actionId);
    }

}
