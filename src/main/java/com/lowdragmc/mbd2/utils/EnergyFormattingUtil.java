package com.lowdragmc.mbd2.utils;

import java.text.DecimalFormat;

public class EnergyFormattingUtil {
    private static final DecimalFormat COMPACT_FORMAT = new DecimalFormat("#.##");
    private static final String[] SUFFIXES = {"", "k", "M", "G"};

    public static String formatExtended(long number) {
        return String.format("%,d", number).replace(".", ",");
    }

    public static String formatCompact(long number) {
        if (number < 1000) {
            return String.valueOf(number);
        }

        var exp = (int) (Math.log10(number) / 3);
        if (exp >= SUFFIXES.length) {
            exp = SUFFIXES.length - 1;
        }

        var value = number / Math.pow(1000, exp);

        return COMPACT_FORMAT.format(value).replace(".", ",") + SUFFIXES[exp];
    }
}
