package com.embabel.demo.agents;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.ai.tool.annotation.Tool;

import com.embabel.agent.api.annotation.*;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.domain.io.UserInput;

import lombok.RequiredArgsConstructor;

@Agent(name = "CustomerSupportAgent", description = "Handles customer support related tasks")
@RequiredArgsConstructor
public class CustomerSupportAgent {
    private static final String TASK_ID = "taskId";

    private final RefundAgent refundAgent;
    private final ShippingAgent shippingAgent;

    public static final String CANCELLED = "CANCELLED";
    public static final String SHIPPED = "SHIPPED";
    public static final String PENDING = "PENDING";

    // enum OrderStatus {
    // CANCELLED,
    // SHIPPED,
    // PENDING
    // }

    record OrderInfo(String orderId) {
        @Tool(description = "Retrieve order status for the given order")
        public String getOrderStatus() {
            var statuses = new String[] { CANCELLED, SHIPPED, PENDING };
            return statuses[ThreadLocalRandom.current().nextInt(statuses.length)];
        }
    }

    record OrderStatus(String status) {
    }
    record TaskOutput(String taskId, String orderId, String gatheredInfo) {
    }

    @Action(description = "Extracts order information from user input")
    public OrderInfo extract(UserInput userInput, OperationContext context) {
        return context.ai().withDefaultLlm().createObject(
                String.format(
                        "Extract the order number from input : %s", userInput),
                OrderInfo.class);
    }

    @Action(description = "Retrieves the status of an order")
    public OrderStatus getOrderStatus(OrderInfo orderInfo, OperationContext context) {
        return context.ai().withDefaultLlm()
                .withToolObject(orderInfo)
                .createObject(
                        String.format(
                                "Retrieve the status of order: %s", orderInfo),
                        OrderStatus.class);
    }

    @Action(description = "Orchestrates order processing tasks")
    public TaskOutput handleSupport(OrderInfo orderInfo, OrderStatus orderStatus, OperationContext context) {
        var taskId = UUID.randomUUID().toString();

        context.bind(TASK_ID, taskId);

        return switch (orderStatus.status()) {
            case PENDING -> RunSubagent.fromAnnotatedInstance(shippingAgent, TaskOutput.class);
            case CANCELLED -> RunSubagent.fromAnnotatedInstance(refundAgent, TaskOutput.class);
            case SHIPPED -> new TaskOutput(
                    taskId,
                    orderInfo.orderId(),
                    "Order is shipped");
            default -> throw new IllegalStateException("Unexpected value: " + orderStatus.status());
        };
    }

    @Action(description = "Marks the support task as done")
    @AchievesGoal(description = "Marks the support task as completed")
    public TaskOutput done(TaskOutput taskOutput) {
        return taskOutput;
    }

    @Agent(name = "RefundAgent", description = "Handles refund related tasks")
    static class RefundAgent {
        @Action(description = "Processes a refund for the given order")
        @AchievesGoal(description = "Handles the refund process for an order")
        public TaskOutput processRefund(OrderInfo orderInfo, OperationContext context) {
            return new TaskOutput(
                    context.get(TASK_ID).toString(),
                    orderInfo.orderId(),
                    "Refund processed");
        }
    }

    @Agent(name = "ShippingAgent", description = "Handles shipping related tasks")
    static class ShippingAgent {
        @Action(description = "Processes shipping for the given order")
        @AchievesGoal(description = "Handles the shipping process for an order")
        public TaskOutput processShipping(OrderInfo orderInfo, OperationContext context) {
            return new TaskOutput(
                    context.get(TASK_ID).toString(),
                    orderInfo.orderId(),
                    "Shipping processed");
        }
    }
}
