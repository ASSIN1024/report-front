package com.report.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLogAnnotation {

    String module() default "";

    String operationType() default "";

    String operationDesc() default "";
}
