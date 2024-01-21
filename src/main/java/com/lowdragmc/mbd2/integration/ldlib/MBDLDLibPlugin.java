package com.lowdragmc.mbd2.integration.ldlib;

import com.lowdragmc.lowdraglib.plugin.ILDLibPlugin;
import com.lowdragmc.lowdraglib.plugin.LDLibPlugin;
import com.lowdragmc.mbd2.common.data.MBDSyncedFieldAccessors;

@LDLibPlugin
public class MBDLDLibPlugin implements ILDLibPlugin {
    @Override
    public void onLoad() {
        MBDSyncedFieldAccessors.init();
    }
}
