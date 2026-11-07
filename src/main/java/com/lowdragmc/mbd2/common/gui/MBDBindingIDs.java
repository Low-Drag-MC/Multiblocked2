package com.lowdragmc.mbd2.common.gui;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class MBDBindingIDs {
    public static final String MACHINE_NAME = "ui:machine_name";
    public static final String PROGRESS_BAR = "ui:progress_bar";
    public static final String FUEL_BAR = "ui:fuel_bar";
    public static final String XEI_LOOKUP = "ui:xei_lookup";

    /**
     * {@code binding} for one recipe logic: {@code ui:progress_bar@b}, {@code ui:fuel_bar@main#1}... The plain id
     * stays bound to the primary logic.
     */
    public static String forRecipeLogic(String binding, String logicAddress) {
        return binding + "@" + logicAddress;
    }
}