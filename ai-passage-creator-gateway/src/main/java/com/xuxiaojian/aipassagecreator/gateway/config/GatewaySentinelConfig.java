package com.xuxiaojian.aipassagecreator.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;

import java.util.*;

/**
 * Sentinel Gateway 限流配置。
 * 首期固定保护文章创建与支付接口，避免高成本接口被瞬时流量打穿。
 */
@Configuration
public class GatewaySentinelConfig {

    @PostConstruct
    public void initGatewayRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();
        rules.add(new GatewayFlowRule("article-route").setCount(20).setIntervalSec(1));
        rules.add(new GatewayFlowRule("payment-route").setCount(5).setIntervalSec(1));
        GatewayRuleManager.loadRules(rules);

        Set<ApiDefinition> definitions = new HashSet<>();
        definitions.add(new ApiDefinition("article-api")
                .setPredicateItems(Set.of(new ApiPathPredicateItem().setPattern("/api/article/**")
                        .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX))));
        definitions.add(new ApiDefinition("payment-api")
                .setPredicateItems(Set.of(new ApiPathPredicateItem().setPattern("/api/payment/**")
                        .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX))));
        GatewayApiDefinitionManager.loadApiDefinitions(definitions);

        GatewayCallbackManager.setBlockHandler((exchange, throwable) -> {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("code", 42900);
            body.put("message", "请求过于频繁，请稍后再试");
            body.put("data", null);
            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body);
        });
    }

    /**
     * 注册 Sentinel 网关限流异常处理器，统一输出 JSON 响应。
     *
     * @param viewResolversProvider 视图解析器提供者
     * @param serverCodecConfigurer 编解码配置
     * @return Sentinel 网关限流异常处理器
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SentinelGatewayBlockExceptionHandler sentinelGatewayBlockExceptionHandler(
            ObjectProvider<List<ViewResolver>> viewResolversProvider,
            ServerCodecConfigurer serverCodecConfigurer) {
        return new SentinelGatewayBlockExceptionHandler(
                viewResolversProvider.getIfAvailable(Collections::emptyList),
                serverCodecConfigurer
        );
    }
}
