package com.lowdragmc.mbd2.common.gui.editor;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.data.IProject;
import com.lowdragmc.lowdraglib.gui.editor.data.Resources;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

@LDLRegister(name = "mproj", group = "editor.machine")
public class MachineProject implements IProject {
    @Getter
    protected Resources resources;

    @Override
    public MachineProject newEmptyProject() {
        return null;
    }

    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        tag.put("resources", resources.serializeNBT());
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        this.resources = loadResources(tag.getCompound("resources"));
    }

    @Override
    public void saveProject(File file) {
        try {
            NbtIo.write(serializeNBT(), file);
        } catch (IOException ignored) { }
    }

    @Nullable
    @Override
    public IProject loadProject(File file) {
        try {
            var tag = NbtIo.read(file);
            if (tag != null) {
                var proj = new MachineProject();
                proj.deserializeNBT(tag);
                return proj;
            }
        } catch (IOException ignored) {}
        return null;
    }

}
