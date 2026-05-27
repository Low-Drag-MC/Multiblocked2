package com.lowdragmc.mbd2.common.trait.entity;

import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderTypes;
import com.lowdragmc.lowdraglib2.client.utils.RenderBufferUtils;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import com.lowdragmc.lowdraglib2.utils.ShapeUtils;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.RecipeCapabilityTraitDefinition;
import com.lowdragmc.mbd2.common.trait.TraitDefinitionType;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

public class EntityHandlerTraitDefinition extends RecipeCapabilityTraitDefinition {
    @LDLRegister(name = "entity_handler", registry = "mbd2:trait_definition_type", group = "trait", priority = -99)
    public static final TraitDefinitionType<EntityHandlerTraitDefinition> TYPE = new TraitDefinitionType<>("entity_handler", "trait") {
        @Override
        public EntityHandlerTraitDefinition createDefinition() {
            return new EntityHandlerTraitDefinition();
        }
    };

    @Getter
    @Configurable(name = "config.definition.trait.entity_handler.area", tips = {
            "config.definition.trait.entity_handler.area.tooltip.0",
            "config.definition.trait.entity_handler.area.tooltip.1"
    })
    @DefaultValue(numberValue = {-1, -1, -1, 2, 2, 2})
    private AABB area = new AABB(-1, -1, -1, 2, 2, 2);

    // runtime
    private final Map<Direction, AABB> areaCache = new EnumMap<>(Direction.class);

    @Override
    public ITrait createTrait(MBDMachine machine) {
        return new EntityHandlerTrait(machine, this);
    }

    @Override
    public TraitDefinitionType<?> type() {
        return TYPE;
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(Items.PIG_SPAWN_EGG);
    }

    @ConfigSetter(field = "area")
    public void setArea(AABB area) {
        this.area = area;
        areaCache.clear();
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        super.deserializeNBT(provider, tag);
        areaCache.clear();
    }

    public AABB getArea(@Nullable Direction direction) {
        return (direction == Direction.NORTH || direction == null) ? area : this.areaCache.computeIfAbsent(direction, dir -> ShapeUtils.rotate(area, dir));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderInEditor(MultiBufferSource bufferSource, float partialTicks) {
        var buffer = bufferSource.getBuffer(LDLibRenderTypes.noDepthLines());
        var color = 0xff11aaee;
        RenderBufferUtils.drawCubeFrame(new PoseStack(), buffer,
                (float) area.minX, (float) area.minY, (float) area.minZ,
                (float) area.maxX, (float) area.maxY, (float) area.maxZ,
                ColorUtils.red(color), ColorUtils.green(color), ColorUtils.blue(color), ColorUtils.alpha(color));
    }
}
