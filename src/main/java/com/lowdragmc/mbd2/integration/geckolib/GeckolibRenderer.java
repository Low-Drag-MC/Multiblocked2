package com.lowdragmc.mbd2.integration.geckolib;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.client.model.ModelFactory;
import com.lowdragmc.lowdraglib.client.renderer.IItemRendererProvider;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.client.renderer.ISerializableRenderer;
import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ArrayConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.StringConfigurator;
import com.lowdragmc.lowdraglib.gui.editor.configurator.WrapperConfigurator;
import com.lowdragmc.lowdraglib.gui.editor.runtime.ConfiguratorParser;
import com.lowdragmc.lowdraglib.gui.editor.ui.Editor;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.utils.ResourceHelper;
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
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.texture.AnimatableTexture;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.util.RenderUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.function.Consumer;

@LDLRegisterClient(name = "geckolib", group = "renderer", modID = "geckolib")
@Getter
@OnlyIn(Dist.CLIENT)
public class GeckolibRenderer implements ISerializableRenderer, GeoRenderer<GeoAnimatable> {
    public static final ResourceLocation DEFAULT_MODEL_PATH = MBD2.id("geo/fire_pedestal.geo.json");
    public static final ResourceLocation DEFAULT_TEXTURE_PATH = MBD2.id("textures/block/fire_pedestal.png");
    public static final ResourceLocation DEFAULT_ANIMATION_PATH = MBD2.id("animations/fire_pedestal.animation.json");
    public static final ResourceLocation DEFAULT_ITEM_TRANSFORM_MODEL = MBD2.id("item/model");
    @Setter
    @Persisted
    protected ResourceLocation modelPath = DEFAULT_MODEL_PATH;
    @Setter
    @Persisted
    protected ResourceLocation texturePath = DEFAULT_TEXTURE_PATH;
    @Setter
    @Persisted
    protected ResourceLocation animationPath = DEFAULT_ANIMATION_PATH;
    @Persisted
    protected ResourceLocation itemTransformModel = DEFAULT_ITEM_TRANSFORM_MODEL;
    @Configurable(name = "geckolib_renderer.scale_width")
    @NumberRange(range = {0, 10000}, wheel = 0.1f)
    protected float scaleWidth = 1;
    @Configurable(name = "geckolib_renderer.scale_height")
    @NumberRange(range = {0, 10000}, wheel = 0.1f)
    protected float scaleHeight = 1;
    @Configurable(name = "geckolib_renderer.use_translucent", tips="geckolib_renderer.use_translucent.tips")
    protected boolean useTranslucent = false;
    @Configurable(name = "geckolib_renderer.use_entity_gui_lighting", tips="geckolib_renderer.use_entity_gui_lighting.tips")
    protected boolean useEntityGuiLighting = false;
    protected List<Animation> animations = new ArrayList<>();

    // runtime
    @Nullable
    private ResourceLocation particleTexture;
    private final StaticAnimatable staticAnimatable = new StaticAnimatable();
    private final GeckolibRendererModel model = new GeckolibRendererModel(this);
    protected ItemStack currentItemStack;
    protected ItemDisplayContext renderPerspective;
    protected GeoAnimatable animatable; // only available in a frame rendering
    protected Matrix4f blockRenderTranslations = new Matrix4f();
    protected Matrix4f modelRenderTranslations = new Matrix4f();
    protected BakedModel itemModel;
    protected Map<String, RawAnimation> animationCache = new HashMap<>();

    public GeckolibRenderer() {
    }

    public GeckolibRenderer(ResourceLocation modelPath, ResourceLocation texturePath, ResourceLocation animationPath) {
        this.modelPath = modelPath;
        this.texturePath = texturePath;
        this.animationPath = animationPath;
    }

    @Override
    public void initRenderer() {
        if (LDLib.isClient()) {
            registerEvent();
        }
    }

    /********* IRenderer *********/

    public void setItemTransformModel(ResourceLocation itemTransformModel) {
        this.itemTransformModel = itemTransformModel;
        this.itemModel = null;
    }

    @Override
    public boolean hasTESR(BlockEntity blockEntity) {
        return true;
    }

    /**
     * register texture to atlas for particles.
     */
    @Override
    public void onPrepareTextureAtlas(ResourceLocation atlasName, Consumer<ResourceLocation> register) {
        if (atlasName.equals(TextureAtlas.LOCATION_BLOCKS)) {
            particleTexture = getTexturePath();
            particleTexture = new ResourceLocation(particleTexture.getNamespace(), particleTexture.getPath().replace("textures/", "").replace(".png", ""));
            register.accept(particleTexture);
        }
    }

    @NotNull
    @Override
    public TextureAtlasSprite getParticleTexture() {
        return particleTexture == null ? IRenderer.EMPTY.getParticleTexture() : Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(particleTexture);
    }

    protected BakedModel getItemTransformModel() {
        if (itemModel == null) {
            itemModel = ModelFactory.getUnBakedModel(itemTransformModel).bake(
                    ModelFactory.getModeBaker(),
                    Material::sprite,
                    BlockModelRotation.X0_Y0,
                    itemTransformModel);
        }
        return itemModel;
    }

    @Override
    public void renderItem(ItemStack stack, ItemDisplayContext transformType, boolean leftHand, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, BakedModel model) {
        IItemRendererProvider.disabled.set(true);
        this.animatable = staticAnimatable;
        this.currentItemStack = stack;
        this.renderPerspective = transformType;

        poseStack.translate(0, -0.25, 0);
        ForgeHooksClient.handleCameraTransforms(poseStack, getItemTransformModel(), transformType, leftHand);
        if (transformType == ItemDisplayContext.GUI) {
            renderInGui(transformType, poseStack, bufferSource, packedLight, packedOverlay);
        }
        else {
            var renderType = getRenderType(this.animatable, getTextureLocation(this.animatable), bufferSource, Minecraft.getInstance().getFrameTime());
            var buffer = ItemRenderer.getFoilBufferDirect(bufferSource, renderType, false, this.currentItemStack != null && this.currentItemStack.hasFoil());

            defaultRender(poseStack, this.animatable, bufferSource, renderType, buffer,
                    0, Minecraft.getInstance().getFrameTime(), packedLight);
        }
        IItemRendererProvider.disabled.set(false);
    }

    public GeoAnimatable getAnimatableFromMachine(MBDMachine machine) {
        GeoAnimatable animatable;
        if (machine.getAnimatableMachine().get(this) instanceof GeoAnimatable geoAnimatable) {
            animatable = geoAnimatable;
        } else {
            var animatableMachine = new AnimatableMachine(machine, this);
            machine.getAnimatableMachine().put(this, animatableMachine);
            animatable = animatableMachine;
        }
        return animatable;
    }

    @Override
    public void render(BlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int combinedOverlay) {
        // check resource existence
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
    public CompoundTag serializeNBT() {
        var tag = ISerializableRenderer.super.serializeNBT();
        var animations = new ListTag();
        this.animations.forEach(animation -> animations.add(animation.serializeNBT()));
        tag.put("animations", animations);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        ISerializableRenderer.super.deserializeNBT(tag);
        this.animations.clear();
        var animations = tag.getList("animations", Tag.TAG_COMPOUND);
        animations.forEach(nbt -> {
            var animation = new Animation();
            animation.deserializeNBT((CompoundTag)nbt);
            this.animations.add(animation);
        });
    }

    /********* GeoRenderer *********/

    public boolean hasAnimation(String name) {
        return getRawAnimation(name) != null;
    }

    @Nullable
    public RawAnimation getRawAnimation(String name) {
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

    /**
     * Gets the id that represents the current animatable's instance for animation purposes.
     * This is mostly useful for things like items, which have a single registered instance for all objects
     */
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


    /**
     * Called before rendering the model to buffer. Allows for render modifications and preparatory
     * work such as scaling and translating.<br>
     * {@link PoseStack} translations made here are kept until the end of the render process
     */
    @Override
    public void preRender(PoseStack poseStack, GeoAnimatable animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue,
                          float alpha) {
        this.blockRenderTranslations = new Matrix4f(poseStack.last().pose());;

        scaleModelForRender(this.scaleWidth, this.scaleHeight, poseStack, animatable, model, isReRender, partialTick, packedLight, packedOverlay);
    }

    /**
     * The actual render method that subtype renderers should override to handle their specific rendering tasks.<br>
     * {@link GeoRenderer#preRender} has already been called by this stage, and {@link GeoRenderer#postRender} will be called directly after
     */
    @Override
    public void actuallyRender(PoseStack poseStack, GeoAnimatable animatable, BakedGeoModel model, RenderType renderType,
                               MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        if (!isReRender) {
            // apply bone transform by animation file
            var animationState = new AnimationState<>(animatable, 0, 0, partialTick, false);
            var instanceId = getInstanceId(animatable);
            var currentModel = getGeoModel();

            animationState.setData(DataTickets.TICK, animatable.getTick(animatable));
            // TODO MORE DATA?
            if (currentItemStack == null) { // block
                poseStack.translate(0.5, 0, 0.5);
                rotateBlock(getFacing(animatable), poseStack);
            } else { // itemstack
                poseStack.translate(0, 0.01f, 0);
            }
            currentModel.addAdditionalStateData(animatable, instanceId, animationState::setData);
            currentModel.handleAnimations(animatable, instanceId, animationState);
        }

        this.modelRenderTranslations = new Matrix4f(poseStack.last().pose());

        GeoRenderer.super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha);
    }

    /**
     * Rotate the {@link PoseStack} based on the determined {@link Direction} the block is facing
     */
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

    /**
     * Attempt to extract a direction from the block so that the model can be oriented correctly
     */
    protected Direction getFacing(GeoAnimatable animatable) {
        if (animatable instanceof AnimatableMachine machine) {
            return machine.getMachine().getFrontFacing().orElse(Direction.NORTH);
        }
        return Direction.NORTH;
    }

    /**
     * Renders the provided {@link GeoBone} and its associated child bones
     */
    @Override
    public void renderRecursively(PoseStack poseStack, GeoAnimatable animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight,
                                  int packedOverlay, float red, float green, float blue, float alpha) {
        if (bone.isTrackingMatrices()) {
            var poseState = new Matrix4f(poseStack.last().pose());
            var localMatrix = RenderUtils.invertAndMultiplyMatrices(poseState, this.blockRenderTranslations);

            bone.setModelSpaceMatrix(RenderUtils.invertAndMultiplyMatrices(poseState, this.modelRenderTranslations));
            bone.setLocalSpaceMatrix(localMatrix);

            if (this.animatable instanceof AnimatableMachine machine) {
                var worldState = new Matrix4f(localMatrix);
                var pos = machine.getMachine().getPos();
                bone.setWorldSpaceMatrix(worldState.translate(new Vector3f(pos.getX(), pos.getY(), pos.getZ())));
            } else {
                bone.setWorldSpaceMatrix(new Matrix4f().identity());
            }
        }

        GeoRenderer.super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue,
                alpha);
    }

    /**
     * Set the current lighting normals for the current render pass
     * <p>
     * Only used for {@link ItemDisplayContext#GUI} rendering
     */
    protected void setupLightingForGuiRender() {
        if (this.useEntityGuiLighting) {
            Lighting.setupForEntityInInventory();
        }
        else {
            Lighting.setupForFlatItems();
        }
    }

    /**
     * Wrapper method to handle rendering the item in a GUI context
     * (defined by {@link ItemDisplayContext#GUI} normally).<br>
     * Just includes some additional required transformations and settings.
     */
    protected void renderInGui(ItemDisplayContext transformType, PoseStack poseStack,
                               MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        setupLightingForGuiRender();

        var defaultBufferSource = bufferSource instanceof MultiBufferSource.BufferSource bufferSource2 ?
                bufferSource2 : ((LevelRendererAccessor)Minecraft.getInstance().levelRenderer).getRenderBuffers().bufferSource();
        var renderType = getRenderType(this.animatable, getTextureLocation(this.animatable), defaultBufferSource, Minecraft.getInstance().getFrameTime());
        var buffer = ItemRenderer.getFoilBufferDirect(bufferSource, renderType, true, this.currentItemStack != null && this.currentItemStack.hasFoil());

        poseStack.pushPose();

        defaultRender(poseStack, this.animatable, defaultBufferSource, renderType, buffer,
                0, Minecraft.getInstance().getFrameTime(), packedLight);
        defaultBufferSource.endBatch();
        RenderSystem.enableDepthTest();
        Lighting.setupFor3DItems();
        poseStack.popPose();
    }

    /**
     * Update the current frame of a {@link AnimatableTexture potentially animated} texture used by this GeoRenderer.<br>
     * This should only be called immediately prior to rendering, and only
     * @see AnimatableTexture#setAndUpdate
     */
    @Override
    public void updateAnimatedTextureFrame(GeoAnimatable animatable) {
        AnimatableTexture.setAndUpdate(getTextureLocation(animatable));
    }

    /**
     * Called after all render operations are completed and the render pass is considered functionally complete.
     * <p>
     * Use this method to clean up any leftover persistent objects stored during rendering or any other post-render maintenance tasks as required
     */
    @Override
    public void doPostRenderCleanup() {
        this.animatable = null;
        this.currentItemStack = null;
        this.renderPerspective = null;
    }

    @Override
    public void fireCompileRenderLayersEvent() {
        // TODO shall we post a render event in the future?
    }

    @Override
    public boolean firePreRenderEvent(PoseStack poseStack, BakedGeoModel model, MultiBufferSource bufferSource, float partialTick, int packedLight) {
        // TODO shall we post a render event in the future?
        return true;
    }

    @Override
    public void firePostRenderEvent(PoseStack poseStack, BakedGeoModel model, MultiBufferSource bufferSource, float partialTick, int packedLight) {
        // TODO shall we post a render event in the future?
    }

    /********* IConfigurator *********/
    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        createPreview(father);
        prepareResourceConfigurator(father);
        ConfiguratorParser.createConfigurators(father, new HashMap<>(), getClass(), this);
        var animationGroup = new ArrayConfiguratorGroup<>("geckolib_renderer.animation_group", false,
                () -> new ArrayList<>(animations), (getter, setter) -> {
            var value = getter.get();
            var group = new ConfiguratorGroup("geckolib_renderer.raw_animation", false);
            value.buildConfigurator(group);
            return group;
        }, true);
        animationGroup.setAddDefault(Animation::new);
        animationGroup.setOnAdd(newAnimation -> {
            animations.add(newAnimation);
            animationCache.clear();
        });
        animationGroup.setOnRemove(removedAnimation -> {
            animations.remove(removedAnimation);
            animationCache.clear();
        });
        animationGroup.setOnUpdate(list -> {
            animations.clear();
            animations.addAll(list);
            animationCache.clear();
        });
        father.addConfigurators(animationGroup);
    }

    protected void prepareResourceConfigurator(ConfiguratorGroup father) {
        var selectTexture = new GuiTextureGroup(
                ColorPattern.T_GRAY.rectTexture().setRadius(5),
                new TextTexture("editor.select_from_file"));
        var selectTextureHover = ColorPattern.WHITE.borderTexture(1).setRadius(5);
        var modelPathGroup = new ConfiguratorGroup("geckolib_renderer.model_path", false);
        modelPathGroup.setCanCollapse(false);
        var modelConfigurator = new StringConfigurator("", () -> modelPath.toString(),
                s -> modelPath = new ResourceLocation(s), DEFAULT_MODEL_PATH.toString(), false);
        modelPathGroup.addConfigurators(modelConfigurator, new WrapperConfigurator(new WidgetGroup(0, 0, 100, 15)
                .addWidget(new ButtonWidget(0, 2, 100, 10, selectTexture,
                        cd -> DialogWidget.showFileDialog(Editor.INSTANCE, "geckolib_renderer.model_path",
                                new File(Editor.INSTANCE.getWorkSpace(), "geo"), true,
                                DialogWidget.suffixFilter(".geo.json"), s -> {
                                    if (s != null && s.isFile()) {
                                        var location = getResourceFromFile(Editor.INSTANCE.getWorkSpace(), s);
                                        modelConfigurator.setValue(location.toString());
                                        setModelPath(location);
                                        if (!checkModelAvailable()) {
                                            Minecraft.getInstance().reloadResourcePacks();
                                        }
                                    }
                                }))
                        .setHoverTexture(selectTextureHover))));

        var animationPathGroup = new ConfiguratorGroup("geckolib_renderer.animation_path", false);
        animationPathGroup.setCanCollapse(false);
        var animationConfigurator = new StringConfigurator("", () -> animationPath.toString(),
                s -> animationPath = new ResourceLocation(s), DEFAULT_ANIMATION_PATH.toString(), false);
        animationPathGroup.addConfigurators(animationConfigurator, new WrapperConfigurator(new WidgetGroup(0, 0, 100, 15)
                .addWidget(new ButtonWidget(0, 2, 100, 10, selectTexture,
                        cd -> DialogWidget.showFileDialog(Editor.INSTANCE, "geckolib_renderer.animation_path",
                                new File(Editor.INSTANCE.getWorkSpace(), "animations"), true,
                                DialogWidget.suffixFilter(".animation.json"), s -> {
                                    if (s != null && s.isFile()) {
                                        var location = getResourceFromFile(Editor.INSTANCE.getWorkSpace(), s);
                                        animationConfigurator.setValue(location.toString());
                                        setAnimationPath(location);
                                        if (!checkAnimationAvailable()) {
                                            Minecraft.getInstance().reloadResourcePacks();
                                        }
                                    }
                                }))
                        .setHoverTexture(selectTextureHover))));

        var texturePathGroup = new ConfiguratorGroup("geckolib_renderer.texture_path", false);
        texturePathGroup.setCanCollapse(false);
        var textureConfigurator = new StringConfigurator("", () -> texturePath.toString(),
                s -> texturePath = new ResourceLocation(s), DEFAULT_TEXTURE_PATH.toString(), false);
        texturePathGroup.addConfigurators(textureConfigurator, new WrapperConfigurator(new WidgetGroup(0, 0, 100, 15)
                .addWidget(new ButtonWidget(0, 2, 100, 10, selectTexture,
                        cd -> DialogWidget.showFileDialog(Editor.INSTANCE, "geckolib_renderer.texture_path",
                                new File(Editor.INSTANCE.getWorkSpace(), "textures"), true,
                                DialogWidget.suffixFilter(".png"), s -> {
                                    if (s != null && s.isFile()) {
                                        var location = getResourceFromFile(Editor.INSTANCE.getWorkSpace(), s);
                                        textureConfigurator.setValue(location.toString());
                                        setTexturePath(location);
                                    }
                                }))
                        .setHoverTexture(selectTextureHover))));

        var itemTransformModelGroup = new ConfiguratorGroup("geckolib_renderer.item_transform_model", false);
        itemTransformModelGroup.setCanCollapse(false);
        itemTransformModelGroup.setTips("geckolib_renderer.item_transform_model.tips");
        var itemTransformModelConfigurator = new StringConfigurator("", () -> itemTransformModel.toString(),
                s -> itemTransformModel = new ResourceLocation(s), DEFAULT_ITEM_TRANSFORM_MODEL.toString(), false);
        itemTransformModelGroup.addConfigurators(itemTransformModelConfigurator, new WrapperConfigurator(new WidgetGroup(0, 0, 100, 15)
                .addWidget(new ButtonWidget(0, 2, 100, 10, selectTexture,
                        cd -> DialogWidget.showFileDialog(Editor.INSTANCE, "geckolib_renderer.item_transform_model",
                                new File(Editor.INSTANCE.getWorkSpace(), "models/item"), true,
                                DialogWidget.suffixFilter(".json"), s -> {
                                    if (s != null && s.isFile()) {
                                        var location = getResourceFromFile(new File(Editor.INSTANCE.getWorkSpace(), "models"), s);
                                        location = new ResourceLocation(location.getNamespace(), location.getPath().replace(".json", ""));
                                        itemTransformModelConfigurator.setValue(location.toString());
                                        setItemTransformModel(location);
                                    }
                                }))
                        .setHoverTexture(selectTextureHover))));


        father.addConfigurators(modelPathGroup, animationPathGroup, texturePathGroup, itemTransformModelGroup);
    }

    private static ResourceLocation getResourceFromFile(File path, File r){
        var id = path.getPath().replace('\\', '/').split("assets/")[1].split("/")[0];
        return new ResourceLocation(id, r.getPath().replace(path.getPath(), "").substring(1));
    }

}
