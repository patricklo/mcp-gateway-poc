package com.example.mcp.util;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.tool.support.ToolDefinitions;
import org.springframework.ai.tool.support.ToolUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

public class ClassMethodToolCallbackProvider implements ToolCallbackProvider, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(MethodToolCallbackProvider.class);

    private List<Class> toolObjectsClass;
    private ApplicationContext applicationContext;

    public ClassMethodToolCallbackProvider(List<Class> toolObjectsClass) {
        this.toolObjectsClass = toolObjectsClass;
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        List<Object> toolObjects = this.toolObjectsClass.stream().map(v -> applicationContext.getBean(v)).toList();
        var toolCallbacks = toolObjects.stream()
                .map(toolObject -> Stream.of(ReflectionUtils.getDeclaredMethods(AopUtils.isAopProxy(toolObject) ? AopUtils.getTargetClass(toolObject) : toolObject.getClass()))
                        .filter(this::isToolAnnotatedMethod).filter(toolMethod -> !isFunctionalType(toolMethod)).filter(ReflectionUtils.USER_DECLARED_METHODS::matches)
                        .map(toolMethod -> MethodToolCallback.builder().toolDefinition(ToolDefinitions.from(toolMethod)).toolMetadata(ToolMetadata.from(toolMethod)).toolMethod(toolMethod)
                                .toolObject(toolObject).toolCallResultConverter(ToolUtils.getToolCallResultConverter(toolMethod)).build())
                        .toArray(ToolCallback[]::new))
                .flatMap(Stream::of).toArray(ToolCallback[]::new);

        validateToolCallbacks(toolCallbacks);

        return toolCallbacks;
    }

    private boolean isFunctionalType(Method toolMethod) {
        var isFunction = ClassUtils.isAssignable(Function.class, toolMethod.getReturnType()) || ClassUtils.isAssignable(Supplier.class, toolMethod.getReturnType())
                || ClassUtils.isAssignable(Consumer.class, toolMethod.getReturnType());

        if (isFunction) {
            logger.warn("Method {} is annotated with @Tool but returns a functional type. " + "This is not supported and the method will be ignored.", toolMethod.getName());
        }

        return isFunction;
    }

    private boolean isToolAnnotatedMethod(Method method) {
        Tool annotation = AnnotationUtils.findAnnotation(method, Tool.class);
        return Objects.nonNull(annotation);
    }

    private void validateToolCallbacks(ToolCallback[] toolCallbacks) {
        List<String> duplicateToolNames = ToolUtils.getDuplicateToolNames(toolCallbacks);
        if (!duplicateToolNames.isEmpty()) {
            throw new IllegalStateException("Multiple tools with the same name (%s) found in sources: %s".formatted(String.join(", ", duplicateToolNames),
                    this.toolObjectsClass.stream().map(o -> o.getName()).collect(Collectors.joining(", "))));
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
