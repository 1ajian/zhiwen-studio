package com.xuxiaojian.aipassagecreator.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AgentExecution {

    /**
     * 智能体名称 : "agent1_generate_titles", "agent2_generate_outline"
     * @return
     */
    String value();

    /**
     * 智能体描述
     */
    String description() default "";
}
