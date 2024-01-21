package com.lowdragmc.mbd2.api.pattern.error;

import net.minecraft.network.chat.Component;

public class PatternStringError extends PatternError{
    public final String translateKey;

    public PatternStringError(String translateKey) {
        this.translateKey = translateKey;
    }

    @Override
    public Component getErrorInfo() {
        return Component.translatable(translateKey);
    }
}
