import com.agent.exception.BusinessException;
import com.agent.exception.GlobalExceptionHandler;
import com.agent.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Agent工具异常处理测试。
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler =
            new GlobalExceptionHandler();

    private final ToolDefinition toolDefinition = ToolDefinition
            .builder()
            .name("queryTradeOrderByOrderNo")
            .description("根据订单编号查询订单")
            .inputSchema("{}")
            .build();

    /**
     * 验证工具内部的业务异常能够保留业务错误信息。
     */
    @Test
    void shouldReturnBusinessErrorWhenToolThrowsBusinessException() {
        BusinessException businessException =
                new BusinessException("订单状态不能为空");

        ToolExecutionException toolException =
                new ToolExecutionException(
                        toolDefinition,
                        businessException
                );

        ResponseEntity<ApiResponse<Void>> response =
                exceptionHandler.handleToolExecutionException(toolException);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5000, response.getBody().code());
        assertEquals(
                "订单状态不能为空",
                response.getBody().message()
        );
    }

    /**
     * 验证工具内部的系统异常不会向前端暴露详细信息。
     */
    @Test
    void shouldHideInternalMessageWhenToolThrowsSystemException() {
        IllegalStateException systemException =
                new IllegalStateException("数据库连接失败");

        ToolExecutionException toolException =
                new ToolExecutionException(
                        toolDefinition,
                        systemException
                );

        ResponseEntity<ApiResponse<Void>> response =
                exceptionHandler.handleToolExecutionException(toolException);

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );
        assertNotNull(response.getBody());
        assertEquals(50000, response.getBody().code());
        assertEquals(
                "系统内部错误",
                response.getBody().message()
        );
    }
}