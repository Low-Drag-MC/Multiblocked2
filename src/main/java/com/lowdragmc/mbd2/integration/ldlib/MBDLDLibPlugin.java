package com.lowdragmc.mbd2.integration.ldlib;

import com.lowdragmc.lowdraglib2.plugin.ILDLibPlugin;
import com.lowdragmc.lowdraglib2.plugin.LDLibPlugin;


@LDLibPlugin
public class MBDLDLibPlugin implements ILDLibPlugin {

    @Override
    public void onLoad() {
        MBDSyncedFieldAccessors.init();
    }
}
