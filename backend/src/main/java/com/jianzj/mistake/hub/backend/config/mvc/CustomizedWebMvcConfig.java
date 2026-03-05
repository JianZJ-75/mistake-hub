package com.jianzj.mistake.hub.backend.config.mvc;

import com.jianzj.mistake.hub.backend.config.AuthCheckProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * <p>
 * Web mvc 配置类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-21
 */
@Configuration
public class CustomizedWebMvcConfig implements WebMvcConfigurer {

    private final RequestAuthInterceptor requestAuthInterceptor;

    private final AuthCheckProperties authCheckProperties;

    public CustomizedWebMvcConfig(RequestAuthInterceptor requestAuthInterceptor,
                                  AuthCheckProperties authCheckProperties) {

        this.requestAuthInterceptor = requestAuthInterceptor;
        this.authCheckProperties = authCheckProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(requestAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(authCheckProperties.getExcludePathList());
    }

    /**
     * 放开 CORS 跨域限制
     *
     * Web 管理端（React + Vite，开发端口 5173）和微信小程序均通过跨域方式请求后端接口，
     * 浏览器默认会拦截跨域请求，需要服务端显式放行。
     * 配置允许所有来源、所有请求头、所有方法、允许携带凭证（Cookie / Authorization），
     * 对所有接口路径生效。
     */
    @Bean
    public CorsFilter corsFilter() {

        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }

    /**
     * 移除 StringHttpMessageConverter
     *
     * Spring MVC 默认注册了 StringHttpMessageConverter，当 Controller 方法返回 String 类型时，
     * 该转换器会抢先将 String 以 text/plain 格式直接写入响应体，
     * 导致项目中的 GlobalResultWrapper（统一包装为 BaseResult）无法生效。
     * 移除后，String 返回值会走 Jackson 的 MappingJackson2HttpMessageConverter 序列化，
     * GlobalResultWrapper 才能正确将其包装为 {"code":0, "data":"xxx"} 的统一格式。
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {

        converters.removeIf(converter -> converter instanceof StringHttpMessageConverter);
    }
}
