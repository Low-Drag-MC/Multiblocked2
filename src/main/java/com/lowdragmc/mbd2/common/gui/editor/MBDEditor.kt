package com.lowdragmc.mbd2.common.gui.editor

import com.lowdragmc.lowdraglib2.LDLib2
import com.lowdragmc.lowdraglib2.editor.ui.Editor
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture
import com.lowdragmc.mbd2.common.gui.MBDSprites
import net.minecraft.resources.ResourceLocation


open class MBDEditor : Editor() {
    companion object {
        @JvmField
        val WINDOW_ID: ResourceLocation = LDLib2.id("mbd2_editor")
        val ICON: SpriteTexture? = MBDSprites.ICON
    }

    init {
        this.icon.style.backgroundTexture(ICON)
        this.leftWindow.setDisplay(false)
        this.leftWindow.getParentWindow()?.removeSplitWindow(this.leftWindow)
    }

    override fun initMenus() {
        super.initMenus()
        fileMenu.addProjectProvider(MachineProject.TYPE)
        fileMenu.addProjectProvider(MultiblockMachineProject.TYPE)
        fileMenu.addProjectProvider(RecipeTypeProject.TYPE)
    }

    override fun createNewEditorInstance(): Editor {
        return MBDEditor()
    }
}
