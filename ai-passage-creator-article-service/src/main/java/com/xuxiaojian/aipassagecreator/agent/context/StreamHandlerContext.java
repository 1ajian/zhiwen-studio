package com.xuxiaojian.aipassagecreator.agent.context;

import java.util.function.Consumer;

/**
 * ClassName: StreamHandlerContext
 * Package: com.xuxiaojian.aipassagecreator.agent.context
 * Description:
 * 流式输出处理器上下文
 * 使用 ThreadLocal 保存 streamHandler，避免将其放入 StateGraph 状态中（无法序列化）
 * @Author 阿健
 * @Create 2026-06-04 18:19
 * @Version 1.0
 */
public class StreamHandlerContext {

    private static final ThreadLocal<Consumer<String>> STREAM_HANDLER = new ThreadLocal<>();

    public static void set(Consumer<String> handler) {
        STREAM_HANDLER.set(handler);
    }

    public static Consumer<String> get() {
        return STREAM_HANDLER.get();
    }

    public static void clear() {
        STREAM_HANDLER.remove();
    }

    public static void send(String message) {
        Consumer<String> consumerHandler = STREAM_HANDLER.get();
        if (consumerHandler != null && message != null) {
            consumerHandler.accept(message);
        }
    }

}
