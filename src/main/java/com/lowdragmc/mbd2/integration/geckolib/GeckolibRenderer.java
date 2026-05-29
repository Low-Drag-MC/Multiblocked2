package com.lowdragmc.mbd2.integration.geckolib;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.model.ModelFactory;
import com.lowdragmc.lowdraglib2.client.renderer.IItemRendererProvider;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.configurator.ConfiguratorParser;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ArrayConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.ResourceHelper;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.core.mixins.LevelRendererAccessor;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.texture.AnimatableTexture;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayersContainer;
import software.bernie.geckolib.util.RenderUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.function.Consumer;

@LDLRegisterClient(name = "geckolib_model", registry = "ldlib2:renderer", group = "renderer", modID = "geckolib")
@Getter
@OnlyIn(Dist.CLIENT)
public class GeckolibRenderer implements IRenderer, GeoRenderer<GeoAnimatable> {
    public static final ResourceLocation DEFAULT_MODEL_PATH = MBD2.id("geo/fire_pedestal.geo.json");
    public static final ResourceLocation DEFAULT_TEXTURE_PATH = MBD2.id("textures/block/fire_pedestal.png");
    public static final ResourceLocation DEFAULT_ANIMATION_PATH = MBD2.id("animations/fire_pedestal.animation.json");
    public static final ResourceLocation DEFAULT_ITEM_TRANSFORM_MODEL = MBD2.id("item/model");

    @Setter
    @Persisted
    @Configurable(name = "geckolib_renderer.model_path")
    protected ResourceLocation modelPath;
    @Setter
    @Persisted
    @Configurable(name = "geckolib_renderer.texture_path")
    protected ResourceLocation texturePath;
    @Setter
    @Persisted
    @Configurable(name = "geckolib_renderer.animation_path")
    protected ResourceLocation animationPath;
    @Persisted
    @Configurable(name = "geckolib_renderer.item_transform_model", tips = "geckolib_renderer.item_transform_model.tips")
    protected ResourceLocation itemTransformModel = DEFAULT_ITEM_TRANSFORM_MODEL;
    @Persisted
    @Configurable(name = "geckolib_renderer.scale_width")
    @ConfigNumber(range = {0, 10000}, wheel = 0.1f)
    protected float scaleWidth = 1;
    @Persisted
    @Configurable(name = "geckolib_renderer.scale_height")
    @ConfigNumber(range = {0, 10000}, wheel = 0.1f)
    protected float scaleHeight = 1;
    @Persisted
    @Configurable(name = "geckolib_renderer.use_glowing_layer", tips = {
            "geckolib_renderer.use_glowing_layer.tips.0",
            "geckolib_renderer.use_glowing_layer.tips.1",
    })
    protected boolean useGlowingLayer = false;
    @Persisted
    @Configurable(name = "geckolib_renderer.use_translucent", tips = "geckolib_renderer.use_translucent.tips")
    protected boolean useTranslucent = false;
    @Persisted
    @Configurable(name = "geckolib_renderer.use_entity_gui_lighting", tips = "geckolib_renderer.use_entity_gui_lighting.tips")
    protected boolean useEntityGuiLighting = false;
    @Persisted
    @Configurable(name = "geckolib_renderer.schedule_state_anim", tips = {
            "geckolib_renderer.schedule_state_anim.tips.0",
            "geckolib_renderer.schedule_state_anim.tips.1",
            "geckolib_renderer.schedule_state_anim.tips.2",
    })
    protected boolean scheduleStateAnimation = false;
    protected final List<Animation> animations = new ArrayList<>();

    protected final GeoRenderLayersContainer<GeoAnimatable> renderLayers = new GeoRenderLayersContainer<>(this);
    @Nullable
    private ResourceLocation particleTexture;
    private final StaticAnimatable staticAnimatable = new StaticAnimatable();
    private final GeckolibRendererModel model = new GeckolibRendererModel(this);
    protected ItemStack currentItemStack;
    protected ItemDisplayContext renderPerspective;
    protected GeoAnimatable animatable;
    protected Matrix4f blockRenderTranslations = new Matrix4f();
    protected Matrix4f modelRenderTranslations = new Matrix4f();
    protected BakedModel itemModel;
    protected final Map<String, RawAnimation> animationCache = new HashMap<>();

    public GeckolibRenderer() {
        this(DEFAULT_MODEL_PATH, DEFAULT_TEXTURE_PATH, DEFAULT_ANIMATION_PATH);
    }

    public GeckolibRenderer(ResourceLocation modelPath, ResourceLocation texturePath, ResourceLocation animationPath) {
        this.renderLayers.addLayer(new AutoGlowingGeoLayer<>(this));
        this.modelPath = modelPath;
        this.texturePath = texturePath;
        this.animationPath = animationPath;
        if (LDLib2.isClient()) {
            registerEvent();
        }
    }

    public void setItemTransformModel(ResourceLocation itemTransformModel) {
        this.itemTransformModel = itemTransformModel;
        this.itemModel = null;
    }

    @Override
    public boolean hasBlockEntityRenderer(BlockEntity blockEntity) {
        return true;
    }

    @Override
    public void onPrepareTextureAtlas(ResourceLocation atlasName, Consumer<ResourceLocation> register) {
        if (atlasName.equals(TextureAtlas.LOCATION_BLOCKS)) {
            particleTexture = ResourceLocation.fromNamespaceAndPath(texturePath.getNamespace(),
                    texturePath.getPath().replace("textures/", "").replace(".png", ""));
            register.accept(particleTexture);
        }
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleTexture(@Nullable BlockAndTintGetter level, @Nullable net.minecraft.core.BlockPos pos, ModelData modelData) {
        return particleTexture == null ?
                IRenderer.super.getParticleTexture(level, pos, modelData) :
                Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(particleTexture);
    }

    protected BakedModel getItemTransformModel() {
        if (itemModel == null) {
            itemModel = ModelFactory.getUnBakedModel(itemTransformModel).bake(
                    ModelFactory.getModelBaker(),
                    Material::sprite,
                    BlockModelRotation.X0_Y0);
        }
        return itemModel;
    }

    @Override
    public void renderItem(ItemStack stack, ItemDisplayContext transformType, boolean leftHand, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, BakedModel model) {
        if (!checkModelAvailable() || !checkTextureAvailable() || !checkAnimationAvailable()) {
            return;
        }
        IItemRendererProvider.disabled.set(true);
        this.animatable = staticAnimatable;
        this.currentItemStack = stack;
        this.renderPerspective = transformType;
        poseStack.pushPose();
        poseStack.translate(0, -0.25, 0);
        ClientHooks.handleCameraTransforms(poseStack, getItemTransformModel(), transformType, leftHand);
        if (transformType == ItemDisplayContext.GUI) {
            renderInGui(poseStack, bufferSource, packedLight, packedOverlay);
        } else {
            var renderType = getRenderType(this.animatable, getTextureLocation(this.animatable), bufferSource, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
            var buffer = ItemRenderer.getFoilBufferDirect(bufferSource, renderType, false, this.currentItemStack != null && this.currentItemStack.hasFoil());
            defaultRender(poseStack, this.animatable, bufferSource, renderType, buffer,
                    0, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true), packedLight);
        }
        poseStack.popPose();
        IItemRendererProvider.disabled.set(false);
        this.currentItemStack = null;
    }

    public GeoAnimatable getAnimatableFromMachine(MBDMachine machine) {
        var cached = machine.getAnimatableMachine().get(this);
        if (cached instanceof GeoAnimatable geoAnimatable) {
            return geoAnimatable;
        }
        var animatableMachine = new AnimatableMachine(machine, this);
        machine.getAnimatableMachine().put(this, animatableMachine);
        return animatableMachine;
    }

    @Override
    public void render(BlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int combinedOverlay) {
        if (!checkModelAvailable() || !checkTextureAvailable() || !checkAnimationAvailable()) {
            return;
        }
        if (IMachine.ofMachine(blockEntity).orElse(null) instanceof MBDMachine machine) {
            this.animatable = getAnimatableFromMachine(machine);
        } else {
            this.animatable = staticAnimatable;
        }
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        defaultRender(poseStack, this.animatable, bufferSource, null, null, 0, partialTicks, packedLight);
    }

    @Override
    public Tag serializeAdditionalNBT(HolderLookup.@NotNull Provider provider) {
        var list = new ListTag();
        this.animations.forEach(animation -> list.add(animation.serializeNBT()));
        return list;
    }

    @Override
    public void deserializeAdditionalNBT(Tag tag, HolderLookup.@NotNull Provider provider) {
        this.animations.clear();
        if (tag instanceof ListTag list) {
            list.forEach(nbt -> {
                var animation = new Animation();
                animation.deserializeNBT((CompoundTag) nbt);
                this.animations.add(animation);
            });
        }
        attachAnimationInfoSources();
        this.animationCache.clear();
    }

    protected void attachAnimationInfoSources() {
        animations.forEach(animation -> animation.attachAnimationInfoSource(this::getAvailableAnimations));
    }

    public List<AnimationInfo> getAvailableAnimations() {
        BakedAnimations bakedAnimations = GeckoLibCache.getBakedAnimations().get(getAnimationPath());
        if (bakedAnimations == null) {
            return List.of();
        }
        return bakedAnimations.animations().values().stream()
                .map(AnimationInfo::of)
                .sorted(Comparator.comparing(AnimationInfo::name))
                .toList();
    }

    public boolean hasAnimation(String name) {
        return getRawAnimation(name) != null;
    }

    @Nullable
    public RawAnimation getRawAnimation(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        if (!animationCache.containsKey(name)) {
            animations.stream()
                    .filter(animation -> animation.getName().equals(name))
                    .findFirst()
                    .ifPresent(animation -> animationCache.put(name, animation.toRawAnimation()));
            if (!animationCache.containsKey(name)) {
                animationCache.put(name, null);
            }
        }
        return animationCache.get(name);
    }

    public boolean checkModelAvailable() {
        return GeckoLibCache.getBakedModels().containsKey(getModelPath());
    }

    public boolean checkTextureAvailable() {
        return ResourceHelper.isResourceExist(getTexturePath()) || ResourceHelper.isResourceExistRaw(getTexturePath());
    }

    public boolean checkAnimationAvailable() {
        return GeckoLibCache.getBakedAnimations().containsKey(getAnimationPath());
    }

    @Override
    public GeoModel<GeoAnimatable> getGeoModel() {
        return this.model;
    }

    @Override
    public GeoAnimatable getAnimatable() {
        return this.animatable;
    }

    @Override
    public long getInstanceId(GeoAnimatable animatable) {
        if (animatable instanceof AnimatableMachine machine) {
            return ((long) machine.getRenderer().hashCode()) << 32 | machine.getMachine().getPos().hashCode();
        }
        if (currentItemStack != null) {
            return GeoItem.getId(currentItemStack);
        }
        return GeoRenderer.super.getInstanceId(animatable);
    }

    @Override
    public void preRender(PoseStack poseStack, GeoAnimatable animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        this.blockRenderTranslations = new Matrix4f(poseStack.last().pose());
        scaleModelForRender(this.scaleWidth, this.scaleHeight, poseStack, animatable, model, isReRender, partialTick, packedLight, packedOverlay);
    }

    @Override
    public void actuallyRender(PoseStack poseStack, GeoAnimatable animatable, BakedGeoModel model, @Nullable RenderType renderType,
                               MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight,
                               int packedOverlay, int colour) {
        if (!isReRender) {
            var animationState = new AnimationState<>(animatable, 0, 0, partialTick, false);
            var instanceId = getInstanceId(animatable);
            var currentModel = getGeoModel();

            animationState.setData(DataTickets.TICK, animatable.getTick(animatable));
            if (currentItemStack == null) {
                poseStack.translate(0.5, 0, 0.5);
                rotateBlock(getFacing(animatable), poseStack);
            } else {
                poseStack.translate(0, 0.01f, 0);
                animationState.setData(DataTickets.ITEMSTACK, currentItemStack);
                animationState.setData(DataTickets.ITEM_RENDER_PERSPECTIVE, renderPerspective);
            }
            currentModel.addAdditionalStateData(animatable, instanceId, animationState::setData);
            currentModel.handleAnimations(animatable, instanceId, animationState, partialTick);
        }

        this.modelRenderTranslations = new Matrix4f(poseStack.last().pose());
        GeoRenderer.super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour);
    }

    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        switch (facing) {
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(0));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
            case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
            case DOWN -> poseStack.mulPose(Axis.XN.rotationDegrees(90));
        }
    }

    protected Direction getFacing(GeoAnimatable animatable) {
        if (animatable instanceof AnimatableMachine machine) {
            return machine.getMachine().getFrontFacing().orElse(Direction.NORTH);
        }
        return Direction.NORTH;
    }

    @Override
    public void renderRecursively(PoseStack poseStack, GeoAnimatable animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight,
                                  int packedOverlay, int colour) {
        if (bone.isTrackingMatrices()) {
            var poseState = new Matrix4f(poseStack.last().pose());
            var localMatrix = RenderUtil.invertAndMultiplyMatrices(poseState, this.blockRenderTranslations);

            bone.setModelSpaceMatrix(RenderUtil.invertAndMultiplyMatrices(poseState, this.modelRenderTranslations));
            bone.setLocalSpaceMatrix(localMatrix);

            if (this.animatable instanceof AnimatableMachine machine) {
                var worldState = new Matrix4f(localMatrix);
                var pos = machine.getMachine().getPos();
                bone.setWorldSpaceMatrix(worldState.translate(new Vector3f(pos.getX(), pos.getY(), pos.getZ())));
            } else {
                bone.setWorldSpaceMatrix(new Matrix4f().identity());
            }
        }

        GeoRenderer.super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay,
                colour);
    }

    protected void setupLightingForGuiRender() {
        if (this.useEntityGuiLighting) {
            Lighting.setupForEntityInInventory();
        } else {
            Lighting.setupForFlatItems();
        }
    }

    protected void renderInGui(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        setupLightingForGuiRender();
        var defaultBufferSource = bufferSource instanceof MultiBufferSource.BufferSource bufferSource2 ?
                bufferSource2 : ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getRenderBuffers().bufferSource();
        var partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        var renderType = getRenderType(this.animatable, getTextureLocation(this.animatable), defaultBufferSource, partialTick);
        var buffer = ItemRenderer.getFoilBufferDirect(bufferSource, renderType, true, this.currentItemStack != null && this.currentItemStack.hasFoil());

        poseStack.pushPose();
        defaultRender(poseStack, this.animatable, defaultBufferSource, renderType, buffer, 0, partialTick, packedLight);
        defaultBufferSource.endBatch();
        RenderSystem.enableDepthTest();
        Lighting.setupFor3DItems();
        poseStack.popPose();
    }

    @Override
    public void updateAnimatedTextureFrame(GeoAnimatable animatable) {
        AnimatableTexture.setAndUpdate(getTextureLocation(animatable));
    }

    @Override
    public void doPostRenderCleanup() {
        this.animatable = null;
        this.currentItemStack = null;
        this.renderPerspective = null;
    }

    @Override
    public List<GeoRenderLayer<GeoAnimatable>> getRenderLayers() {
        if (useGlowingLayer) {
            return renderLayers.getRenderLayers();
        }
        return List.of();
    }

    @Override
    public void fireCompileRenderLayersEvent() {
    }

    @Override
    public boolean firePreRenderEvent(PoseStack poseStack, BakedGeoModel model, MultiBufferSource bufferSource, float partialTick, int packedLight) {
        return true;
    }

    @Override
    public void firePostRenderEvent(PoseStack poseStack, BakedGeoModel model, MultiBufferSource bufferSource, float partialTick, int packedLight) {
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        attachAnimationInfoSources();
        createPreview(father);
        ConfiguratorParser.createConfigurators(father, new HashMap<>(), getClass(), this);
        father.addConfigurators(createAnimationInfoGroup());
        var animationGroup = new ArrayConfiguratorGroup<>(
                "geckolib_renderer.animation_group",
                false,
                () -> new ArrayList<>(animations),
                (getter, setter) -> {
                    var value = getter.get();
                    value.attachAnimationInfoSource(this::getAvailableAnimations);
                    var group = new ConfiguratorGroup("geckolib_renderer.raw_animation", false);
                    value.buildConfigurator(group);
                    return group;
                },
                true);
        animationGroup.setAddDefault(() -> new Animation().attachAnimationInfoSource(this::getAvailableAnimations));
        animationGroup.setOnUpdate(list -> {
            animations.clear();
            animations.addAll(list);
            attachAnimationInfoSources();
            animationCache.clear();
        });
        father.addConfigurators(animationGroup);
    }

    protected ConfiguratorGroup createAnimationInfoGroup() {
        var group = new ConfiguratorGroup("geckolib_renderer.animation_file_info", false);
        var signature = new String[1];
        group.addEventListener(UIEvents.TICK, event -> refreshAnimationInfoGroup(group, signature));
        refreshAnimationInfoGroup(group, signature);
        return group;
    }

    protected void refreshAnimationInfoGroup(ConfiguratorGroup group, String[] signature) {
        var infos = getAvailableAnimations();
        var newSignature = getAnimationPath() + "|" + infos.stream()
                .map(info -> info.name() + ":" + info.length() + ":" + info.loopType())
                .reduce("", (left, right) -> left + ";" + right);
        if (newSignature.equals(signature[0])) {
            return;
        }
        signature[0] = newSignature;
        group.removeAllConfigurators();
        if (infos.isEmpty()) {
            group.addConfigurators(new Configurator("")
                    .addInlineChild(new Label().setText("No animations loaded")));
            return;
        }
        for (var info : infos) {
            group.addConfigurators(createAnimationInfoRow(info));
        }
    }

    protected Configurator createAnimationInfoRow(AnimationInfo info) {
        var row = new Configurator(info.name());
        row.addInlineChild(new Label().setText(Component.literal(String.format(Locale.ROOT, "%.2f ticks", info.length()))));
        row.addInlineChild(new Label().setText(Component.literal(info.loop() ? "loop" : "once")));
        row.addInlineChild(new Label().setText(Component.literal(info.loopType())));
        row.addInlineChild(new Button()
                .setText(Component.translatable("geckolib_renderer.animation_file_info.play"))
                .setOnClick(event -> playPreviewAnimation(info)));
        return row;
    }

    protected void playPreviewAnimation(AnimationInfo info) {
        staticAnimatable.playPreview(RawAnimation.begin().then(info.name(), info.geckoLoopType()));
    }
}
