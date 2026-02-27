package com.jianzj.mistake.hub.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>
 * Swagger 配置类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-06
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI springShopOpenAPI() {

        return new OpenAPI()
                .info(new Info().title("错题复习调度系统后端接口")
                        .contact(new Contact().name("钟健").email("2279814184@qq.com"))
                        .version("1.0")
                        .description("基于艾宾浩斯记忆曲线的复习调度系统。前端同学（AI）请根据此文档生成界面。"));
    }
}
