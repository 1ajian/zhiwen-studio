package com.xuxiaojian.aipassagecreator.agent.agents;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.google.gson.reflect.TypeToken;
import com.xuxiaojian.aipassagecreator.constant.PromptConstant;
import com.xuxiaojian.aipassagecreator.exception.BusinessException;
import com.xuxiaojian.aipassagecreator.exception.ErrorCode;
import com.xuxiaojian.aipassagecreator.model.dto.article.ArticleState;
import com.xuxiaojian.aipassagecreator.model.enums.ArticleStyleEnum;
import com.xuxiaojian.aipassagecreator.utils.GsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * ClassName: TitleGeneratorAgent
 * Package: com.xuxiaojian.aipassagecreator.agent.agents
 * Description:
 *
 * @Author 阿健
 * @Create 2026-06-04 18:31
 * @Version 1.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TitleGeneratorAgent implements NodeAction {

    private final DashScopeChatModel chatModel;

    public static final String INPUT_TOPIC = "topic";
    public static final String INPUT_STYLE = "style";
    public static final String OUTPUT_TITLE_OPTIONS = "titleOptions";

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String topic = state.value(INPUT_TOPIC)
                .map(Object::toString)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR, "缺少选题参数"));

        String style = state.value(INPUT_STYLE).map(Object::toString).orElse(null);

        log.info("TitleGeneratorAgent 开始执行: topic={}, style={}", topic, style);

        // 构建 prompt
        String prompt = PromptConstant.AGENT1_TITLE_PROMPT
                .replace("{topic}", topic)
                + getStylePrompt(style);

        ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
        String content = response.getResult().getOutput().getText();

        List<ArticleState.TitleOption> titleOptions = GsonUtils.fromJson(
                content,
                new TypeToken<List<ArticleState.TitleOption>>(){}
        );

        log.info("TitleGeneratorAgent 执行完成: 生成了 {} 个标题方案", titleOptions.size());
        return Map.of(OUTPUT_TITLE_OPTIONS,titleOptions);
    }

    private String getStylePrompt(String style) {
        if (style == null || style.isEmpty()) {
            return "";
        }

        ArticleStyleEnum styleEnum = ArticleStyleEnum.getEnumByValue(style);
        if (styleEnum == null) {
            return "";
        }

        return switch (styleEnum) {
            case TECH -> PromptConstant.STYLE_TECH_PROMPT;
            case EMOTIONAL -> PromptConstant.STYLE_EMOTIONAL_PROMPT;
            case EDUCATIONAL -> PromptConstant.STYLE_EDUCATIONAL_PROMPT;
            case HUMOROUS -> PromptConstant.STYLE_HUMOROUS_PROMPT;
        };
    }
}
