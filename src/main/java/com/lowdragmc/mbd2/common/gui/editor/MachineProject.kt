package com.lowdragmc.mbd2.common.gui.editor

import com.lowdragmc.lowdraglib2.editor.project.IProject
import com.lowdragmc.lowdraglib2.editor.project.ProjectType
import com.lowdragmc.lowdraglib2.editor.resource.ColorsResource
import com.lowdragmc.lowdraglib2.editor.resource.IRendererResource
import com.lowdragmc.lowdraglib2.editor.resource.Resources
import com.lowdragmc.lowdraglib2.editor.resource.TexturesResource
import com.lowdragmc.lowdraglib2.editor.resource.UIResource
import com.lowdragmc.lowdraglib2.editor.ui.Editor
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture
import com.lowdragmc.lowdraglib2.utils.TagBuilder
import com.lowdragmc.mbd2.MBD2
import com.lowdragmc.mbd2.client.MBDRenderers
import com.lowdragmc.mbd2.common.gui.editor.machine.MachineConfigView
import com.lowdragmc.mbd2.common.gui.editor.machine.MachineTraitView
import com.lowdragmc.mbd2.common.gui.editor.machine.MachineUIView
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState
import com.lowdragmc.mbd2.common.machine.definition.config.StateMachine
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import java.io.File

open class MachineProject : IProject {
    companion object {
        var VERSION: Int = 1
        val TYPE: ProjectType = MachineProjectType()
    }

    private class MachineProjectType: ProjectType(IGuiTexture.EMPTY, "single_machine_project", ".sm", { MachineProject() }) {
        override fun getRootSavePath(project: IProject, projectRoot: File): File {
            return projectRoot.resolve("mbd2/machine")
        }
    }

    val definition: MBDMachineDefinition = createDefinition()

    private val resources = createResources()
    override fun getResources(): Resources = resources

    // runtime
    var machineConfigView: MachineConfigView? = null
        protected set
    var machineTraitView: MachineTraitView? = null
        protected set
    var machineUIView: MachineUIView? = null
        protected set

    init {
        definition.loadFactory()
    }

    protected open fun createResources(): Resources {
        return Resources.of(
            IRendererResource.INSTANCE,
            ColorsResource.INSTANCE,
            TexturesResource.INSTANCE,
            UIResource.INSTANCE
        )
    }

    protected open fun createDefinition(): MBDMachineDefinition {
        // use vanilla furnace model as an example
        return MBDMachineDefinition.builder()
            .id(MBD2.id("new_machine"))
            .rootState(StateMachine.createSingleDefault( { MachineState.baseBuilder() },
                { MBDRenderers.MACHINE },
                { MBDRenderers.MACHINE_WORKING },
                { MBDRenderers.MACHINE_WAITING },
                { MBDRenderers.MACHINE_WAITING }
            ))
            .build()
    }

    override fun getVersion(): String {
        return "%d.0".format(VERSION)
    }

    override fun getProjectType(): ProjectType {
        return TYPE
    }

    override fun serializeProject(provider: HolderLookup.Provider): CompoundTag {
        return TagBuilder.compound().add("definition", definition.serializeNBT(provider)).build()
    }

    override fun deserializeProject(provider: HolderLookup.Provider, tag: CompoundTag) {
        this.definition.deserializeNBT(provider, tag.getCompound("definition"))
    }

    override fun onLoad(editor: Editor) {
        if (editor is MBDEditor) {
            super.onLoad(editor)
            editor.placeView(createMachineConfigView(editor).also { machineConfigView = it }, { editor.centerWindow.leftTop })
            editor.placeView(createMachineTraitView(editor).also { machineTraitView = it }, { editor.centerWindow.leftTop })
            editor.placeView(createMachineUIView(editor).also { machineUIView = it }, { editor.centerWindow.leftTop })
        }
    }

    protected open fun createMachineConfigView(editor: MBDEditor): MachineConfigView {
        return MachineConfigView(editor, this)
    }

    protected fun createMachineTraitView(editor: MBDEditor): MachineTraitView {
        return MachineTraitView(editor, this)
    }

    protected fun createMachineUIView(editor: MBDEditor): MachineUIView {
        return MachineUIView(editor, this)
    }

    override fun onClosed(editor: Editor) {
        this.machineConfigView?.removeSelf()
        this.machineConfigView = null

        this.machineTraitView?.removeSelf()
        this.machineTraitView = null

        this.machineUIView?.removeSelf()
        this.machineUIView = null
    }
}
