package com.lowdragmc.mbd2.api.recipe.content;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.BigInteger;

@Getter
@Setter
@KJSBindings
public class ContentModifier implements IConfigurable {
    public static final ContentModifier IDENTITY = ContentModifier.identity();

    @Configurable(name="content_modifier.multiplier", tips="content_modifier.multiplier.tips")
    @ConfigNumber(range = {0, Double.MAX_VALUE}, wheel = 1f)
    private double multiplier;
    @Configurable(name="content_modifier.addition", tips="content_modifier.addition.tips")
    @ConfigNumber(range = {0, Double.MAX_VALUE}, wheel = 1f)
    private double addition;

    public boolean isIdentity() {
        return multiplier == 1 && addition == 0;
    }

    public static ContentModifier of(double multiplier, double addition) {
        return new ContentModifier(multiplier, addition);
    }

    public static ContentModifier multiplier(double multiplier) {
        return new ContentModifier(multiplier, 0);
    }

    public static ContentModifier addition(double addition) {
        return new ContentModifier(1, addition);
    }

    public static ContentModifier identity() {
        return new ContentModifier(1, 0);
    }

    public ContentModifier(double multiplier, double addition) {
        this.multiplier = multiplier;
        this.addition = addition;
    }

    public Number apply(Number number) {
        if (number instanceof BigDecimal decimal) {
            return decimal.multiply(BigDecimal.valueOf(multiplier)).add(BigDecimal.valueOf(addition));
        }
        if (number instanceof BigInteger bigInteger) {
            return bigInteger.multiply(BigInteger.valueOf((long) multiplier)).add(BigInteger.valueOf((long) addition));
        }
        return number.doubleValue() * multiplier + addition;
    }

    public ContentModifier merge(ContentModifier modifier) {
        return new ContentModifier(multiplier * modifier.multiplier, addition + modifier.addition);
    }

}
