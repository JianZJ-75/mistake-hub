package com.jianzj.mistake.hub.common.base;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * <p>
 * 自定义上下文
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-20
 */
public class ApplicationContextHolder implements ApplicationContextAware {

    public static ApplicationContext CONTEXT;

    /**
     * 创建该实例时自动调用
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        CONTEXT = applicationContext;
    }

    /**
     * 根据类型获取
     */
    public static <T> T getBean(Class<T> clazz) {
        return CONTEXT.getBean(clazz);
    }

    /**
     * 根据类型和名称获取
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        return CONTEXT.getBean(name, clazz);
    }

    /**
     * 根据类型获取多个
     */
    public static <T> Map<String, T> getBeansOfType(Class<T> clazz) {
        return CONTEXT.getBeansOfType(clazz);
    }

    /**
     * 根据名称和注解获取
     */
    public static <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType) {
        return CONTEXT.getBean(beanName, annotationType);
    }
}
