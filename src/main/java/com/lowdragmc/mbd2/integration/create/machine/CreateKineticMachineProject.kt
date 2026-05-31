package com.lowdragmc.mbd2.integration.create.machine

import com.lowdragmc.lowdraglib2.editor.project.IProject
import com.lowdragmc.lowdraglib2.editor.project.ProjectType
import com.lowdragmc.lowdraglib2.editor.resource.BuiltinPath
import com.lowdragmc.lowdraglib2.editor.resource.IRendererResource
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture
import com.lowdragmc.mbd2.MBD2
import com.lowdragmc.mbd2.client.MBDRenderers
import com.lowdragmc.mbd2.common.gui.editor.MBDEditor
import com.lowdragmc.mbd2.common.gui.editor.MachineProject
import com.lowdragmc.mbd2.common.gui.editor.machine.MachineConfigView
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition
import net.minecraft.world.phys.shapes.Shapes
import java.io.File

open class CreateKineticMachineProject : MachineProject() {
    companion object {
        @JvmField
        val TYPE: ProjectType = CreateKineticMachineProjectType()
    }

    private class CreateKineticMachineProjectType : ProjectType(
        IGuiTexture.EMPTY,
        "create_machine_project",
        ".cm",
        { CreateKineticMachineProject() }
    ) {
        override fun getRootSavePath(project: IProject, projectRoot: File): File {
            return projectRoot.resolve("mbd2/create_machine")
        }
    }

    override fun createDefinition(): MBDMachineDefinition {
        // use vanilla furnace model as an example
        return CreateKineticMachineDefinition.builder()
            .id(MBD2.id("new_machine"))
            .rootState(CreateMachineState.builder()
                .rotationRenderer(IRendererResource.INSTANCE.resourceInstance.getResource(BuiltinPath("mbd:SHAFT")))
                .renderer({ MBDRenderers.GEAR_BOX })
                .shape(Shapes.block())
                .lightLevel(0)
                .child("working", { it.child("waiting") })
                .child("suspend").build()
            )
            .build()
    }

    override fun getProjectType(): ProjectType {
        return TYPE
    }

    override fun createMachineConfigView(editor: MBDEditor): MachineConfigView {
        return CreateMachineConfigView(editor, this)
    }
}
