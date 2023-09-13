package org.chk.jdbc.rsm;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(value = RetentionPolicy.RUNTIME)
public @interface Mapping {

	String propertyType() default "";
}
