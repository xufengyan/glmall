package com.xf.glmall.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 创建注解
 */
@Target(ElementType.METHOD) //方法上有效
@Retention(RetentionPolicy.RUNTIME) //虚拟机运行时有效
public @interface LoginRequired {
    //用来判断是否必须登录成功
    //true 是用来判断没有登录不能访问的
    //false 是用来判断即使没有登录也可以访问的
    boolean loginSuccess() default true;
}
