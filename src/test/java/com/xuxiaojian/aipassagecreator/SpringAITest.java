package com.xuxiaojian.aipassagecreator;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

/**
 * ClassName: SpringAITest
 * Package: com.xuxiaojian.aipassagecreator
 * Description:
 *
 * @Author 阿健
 * @Create 2026-05-27 17:26
 * @Version 1.0
 */
@SpringBootTest
public class SpringAITest {

    @Resource
    private DashScopeChatModel chatModel;

    @Test
    void testChat() {
        //同步调用
//        String response = chatModel.call("你好,请介绍下自己");
//        System.out.println(response);

        //流式调用
        Flux<ChatResponse> stream = chatModel.stream(new Prompt("用一句话介绍 Spring AI"));
        stream.subscribe(chunk -> System.out.println(chunk.getResult().getOutput().getText()));
    }
}
