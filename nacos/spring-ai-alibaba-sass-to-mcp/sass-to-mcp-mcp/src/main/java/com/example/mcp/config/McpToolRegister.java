package com.example.mcp.config;

import java.util.Arrays;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.stereotype.Component;

import com.example.mcp.mcp.McpTools;
import com.example.mcp.util.ClassMethodToolCallbackProvider;

@Component
public class McpToolRegister implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (beanFactory instanceof BeanDefinitionRegistry) {
            BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

            // 获取所有带有特定注解的 Bean
            String[] beanNames = beanFactory.getBeanNamesForAnnotation(McpTools.class);

            for (String beanName : beanNames) {
                BeanDefinition bd = registry.getBeanDefinition(beanName);

                try {
                    // 获取 Bean 的 Class
                    Class<?> beanClass = Class.forName(bd.getBeanClassName());

                    // 创建新的 MethodToolCallbackProvider Bean 定义
                    registerMethodToolCallbackProvider(registry, beanName, beanClass);

                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void registerMethodToolCallbackProvider(BeanDefinitionRegistry registry, String originalBeanName, Class<?> originalBeanClass) {

        String providerBeanName = "methodToolCallbackProvider_" + originalBeanName;

        // 如果已存在则跳过
        if (registry.containsBeanDefinition(providerBeanName)) {
            return;
        }

        // 使用 GenericBeanDefinition
        GenericBeanDefinition definition = new GenericBeanDefinition();
        definition.setBeanClass(ClassMethodToolCallbackProvider.class);

        // 方案A：通过构造函数参数传递
        definition.getConstructorArgumentValues().addGenericArgumentValue(Arrays.asList(originalBeanClass));
        // 注册 Bean 定义
        registry.registerBeanDefinition(providerBeanName, definition);
    }
}