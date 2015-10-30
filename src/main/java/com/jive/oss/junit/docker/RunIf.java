package com.jive.oss.junit.docker;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.util.function.Predicate;

@java.lang.annotation.Retention(value = RUNTIME)
@java.lang.annotation.Target(value = { METHOD, TYPE })
public @interface RunIf
{

  Class<? extends Predicate<Object>>value();

  String[]arguments() default {};

}
