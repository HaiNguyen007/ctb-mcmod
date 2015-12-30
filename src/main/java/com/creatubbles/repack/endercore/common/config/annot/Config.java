package com.creatubbles.repack.endercore.common.config.annot;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.minecraftforge.common.config.Configuration;

import com.creatubbles.repack.endercore.common.config.ConfigProcessor;

/**
 * Used to mark a {@code static} field as a config option. Has no effect if the class is not processed with a
 * {@link ConfigProcessor}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface Config {

    /**
     * The section of the config option.
     *
     * (AKA Category)
     *
     * @return A string section name.
     */
    String value() default Configuration.CATEGORY_GENERAL;
}
