package com.lowdragmc.mbd2.api.annotations;

import org.jetbrains.annotations.NotNull;

import javax.annotation.meta.TypeQualifierDefault;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@NotNull
@Retention(RetentionPolicy.RUNTIME)
@TypeQualifierDefault({ElementType.METHOD})
public @interface NotNullReturn {
}
