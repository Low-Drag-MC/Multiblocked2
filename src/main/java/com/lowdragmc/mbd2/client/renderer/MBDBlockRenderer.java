package com.lowdragmc.mbd2.client.renderer;

import com.lowdragmc.lowdraglib.client.model.forge.LDLRendererModel;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.AllArgsConstructor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static com.lowdragmc.lowdraglib.client.model.forge.LDLRendererModel.RendererBakedModel.*;

@OnlyIn(Dist.CLIENT)
@AllArgsConstructor
public class MBDBlockRenderer implements IRenderer {

    protected final BooleanSupplier useAO;
    protected final Supplier<IRenderer> defaultRenderer;

    @Override
    public boolean useAO() {
        return useAO.getAsBoolean();
    }

    public Optional<MBDMachine> getMachine(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos) {
        if (level == null || pos == null)
            return Optional.empty();
        return IMachine.ofMachine(level, pos).filter(MBDMachine.class::isInstance).map(MBDMachine.class::cast);
    }

    public Optional<MBDMachine> getMachine(@Nullable BlockEntity blockEntity) {
        if (blockEntity == null)
            return Optional.empty();
        return IMachine.ofMachine(blockEntity).filter(MBDMachine.class::isInstance).map(MBDMachine.class::cast);
    }

    @Override
    public void renderItem(ItemStack stack, ItemDisplayContext transformType, boolean leftHand, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, BakedModel model) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<BakedQuad> renderModel(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getMachine(level, pos)
                .filter(machine -> !machine.isDisableRendering())
                .map(machine -> machine.getMachineState().getRealRenderer().renderModel(level, pos, state, side, rand))
                .orElseGet(Collections::emptyList);
    }

    @NotNull
    @Override
    public TextureAtlasSprite getParticleTexture() {
        var modelData = LDLRendererModel.RendererBakedModel.CURRENT_MODEL_DATA.get();
        if (modelData != null) {
            var world = modelData.get(WORLD);
            var pos = modelData.get(POS);
            return getMachine(world, pos)
                    .map(machine -> machine.getMachineState().getRealRenderer().getParticleTexture())
                    .orElseGet(() -> defaultRenderer.get().getParticleTexture());
        }
        return defaultRenderer.get().getParticleTexture();
    }

    @Override
    public boolean hasTESR(BlockEntity blockEntity) {
        return getMachine(blockEntity).map(machine ->
                machine.getMachineState().getRealRenderer().hasTESR(blockEntity) ||
                        machine.getDefinition().machineSettings().traitDefinitions().stream()
                                .map(definition -> definition.getBESRenderer(machine))
                                .anyMatch(renderer -> renderer.hasTESR(blockEntity))
        ).orElse(false);
    }

    @Override
    public boolean isGlobalRenderer(BlockEntity blockEntity) {
        return getMachine(blockEntity).map(machine ->
                machine.getMachineState().getRealRenderer().isGlobalRenderer(blockEntity) ||
                        machine.getDefinition().machineSettings().traitDefinitions().stream()
                                .map(definition -> definition.getBESRenderer(machine))
                                .anyMatch(renderer -> renderer.isGlobalRenderer(blockEntity))
        ).orElse(false);
    }

    @Override
    public boolean shouldRender(BlockEntity blockEntity, Vec3 cameraPos) {
        return getMachine(blockEntity).filter(machine -> !machine.isDisableRendering())
                .map(machine -> machine.getMachineState().getRealRenderer().shouldRender(blockEntity, cameraPos))
                .orElseGet(() -> defaultRenderer.get().shouldRender(blockEntity, cameraPos));
    }

    @Override
    public void render(BlockEntity blockEntity, float partialTicks, PoseStack stack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        getMachine(blockEntity).ifPresent(machine -> {
            machine.getMachineState().getRealRenderer().render(blockEntity, partialTicks, stack, buffer, combinedLight, combinedOverlay);
            for (var traitDefinition : machine.getDefinition().machineSettings().traitDefinitions()) {
                var renderer = traitDefinition.getBESRenderer(machine);
                if (renderer.hasTESR(blockEntity)) {
                    renderer.render(blockEntity, partialTicks, stack, buffer, combinedLight, combinedOverlay);
                }
            }
        });
    }
}
