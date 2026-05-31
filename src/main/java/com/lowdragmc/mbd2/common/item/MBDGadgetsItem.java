package com.lowdragmc.mbd2.common.item;

import com.lowdragmc.lowdraglib2.gui.factory.HeldItemUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.api.pattern.MultiblockState;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.data.MBDDataComponents;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import com.lowdragmc.mbd2.common.network.packets.SPatternErrorPosPacket;
import com.mojang.serialization.Codec;
import dev.vfyjxf.taffy.style.FlexDirection;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MBDGadgetsItem extends Item implements HeldItemUIMenuType.HeldItemUI {
    public static final ResourceLocation EMPTY_RECIPE = MBD2.id("empty");
    public enum Mode implements StringRepresentable {
        RECIPE_DEBUGGER("recipe_debugger", 0),
        MULTIBLOCK_BUILDER("multiblock_builder", 1),
        MULTIBLOCK_DEBUGGER("multiblock_debugger", 2);

        public final String name;
        public final int id;

        Mode(String name, int id) {
            this.name = name;
            this.id = id;
        }

        public static final Codec<Mode> CODEC = StringRepresentable.fromValues(Mode::values);
        public static final IntFunction<Mode> BY_ID = ByIdMap.continuous(mode -> mode.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        public static final StreamCodec<ByteBuf, Mode> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, mode -> mode.id);

        @Override
        public String getSerializedName() {
            return "";
        }
    }

    public MBDGadgetsItem() {
        super(new Item.Properties()
                .component(MBDDataComponents.GADGET_RECIPE, EMPTY_RECIPE)
                .component(MBDDataComponents.GADGET_MODE, Mode.RECIPE_DEBUGGER)
                .fireResistant()
                .stacksTo(1));
    }

    public static @Nullable Mode getMode(ItemStack stack) {
        return stack.get(MBDDataComponents.GADGET_MODE);
    }

    public static void setMode(ItemStack stack, Mode mode) {
        stack.set(MBDDataComponents.GADGET_MODE, mode);
    }

    public boolean isMultiblockBuilder(ItemStack stack) {
        return getMode(stack) == Mode.MULTIBLOCK_BUILDER;
    }

    public boolean isRecipeDebugger(ItemStack stack) {
        return getMode(stack) == Mode.RECIPE_DEBUGGER;
    }

    public boolean isMultiblockDebugger(ItemStack stack) {
        return getMode(stack) == Mode.MULTIBLOCK_DEBUGGER;
    }

    @Nullable
    public ResourceLocation getRecipe(ItemStack stack) {
        return stack.get(MBDDataComponents.GADGET_RECIPE.get());
    }

    public void setRecipe(ItemStack stack, ResourceLocation recipe) {
        stack.set(MBDDataComponents.GADGET_RECIPE.get(), recipe);
    }

    @Override
    public String getDescriptionId(ItemStack pStack) {
        var id = super.getDescriptionId(pStack);
        if (isMultiblockBuilder(pStack)) {
            return id + ".multiblock_builder";
        } else if (isRecipeDebugger(pStack)) {
            return id + ".recipe_debugger";
        } else if (isMultiblockDebugger(pStack)) {
            return id + ".multiblock_debugger";
        }
        return id;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> components, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, components, tooltipFlag);
        components.add(Component.translatable(getDescriptionId() + ".tooltip"));
        var id = getDescriptionId(stack);
        if (isMultiblockBuilder(stack))
            components.add(Component.translatable(id + ".tooltip"));
        else if (isRecipeDebugger(stack)) {
            components.add(Component.translatable(id + ".tooltip.0"));
            components.add(Component.translatable(id + ".tooltip.1"));
            var recipe = getRecipe(stack);
            if (recipe != null) {
                components.add(Component.translatable(id + ".tooltip.2", recipe.toString()));
            }
        } else if (isMultiblockDebugger(stack)) {
            components.add(Component.translatable(id + ".tooltip"));
        }
    }

    private boolean isUsed;

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        var stack = pPlayer.getItemInHand(pUsedHand);
        if (isUsed) {
            isUsed = false;
            return InteractionResultHolder.success(stack);
        }
        if (pPlayer.isCrouching()) {
            if (isMultiblockBuilder(stack)) {
                setMode(stack, Mode.RECIPE_DEBUGGER);
                return InteractionResultHolder.success(stack);
            } else if (isRecipeDebugger(stack)) {
                setMode(stack, Mode.MULTIBLOCK_DEBUGGER);
                return InteractionResultHolder.success(stack);
            } else if (isMultiblockDebugger(stack)) {
                setMode(stack, Mode.MULTIBLOCK_BUILDER);
                return InteractionResultHolder.success(stack);
            }
        } else if (pPlayer instanceof ServerPlayer serverPlayer && isRecipeDebugger(stack)) {
            HeldItemUIMenuType.openUI(serverPlayer, pUsedHand);
            return InteractionResultHolder.success(stack);
        }
        return super.use(pLevel, pPlayer, pUsedHand);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var player = context.getPlayer();
        if (player instanceof ServerPlayer serverPlayer && !serverPlayer.isCrouching()) {
            if (isMultiblockBuilder(stack)) {
                var controller = IMultiController.ofController(player.level(), context.getClickedPos()).orElse(null);
                if (controller != null) {
                    controller.getPattern().autoBuild(player,
                            new MultiblockState(player.level(), context.getClickedPos()));
                    isUsed = true;
                    return InteractionResult.SUCCESS;
                }
            } else if (isMultiblockDebugger(stack)) {
                var controller = IMultiController.ofController(player.level(), context.getClickedPos()).orElse(null);
                if (controller == null) {
                    serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.multiblock_debugger.failure.error.not_controller"));
                } else if (controller.isFormed()) {
                    serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.multiblock_debugger.is_formed"));
                } else if (controller.checkPatternWithLock()) {
                    serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.multiblock_debugger.success"));
                    if (controller instanceof MBDMultiblockMachine multiblock
                            && multiblock.getDefinition().multiblockSettings().catalyst().isEnable()) {
                        var catalyst = multiblock.getDefinition().multiblockSettings().catalyst();
                        if (!catalyst.getFilterItems().isEmpty()) {
                            var items = Component.literal("[");
                            for (ItemStack filterItem : catalyst.getFilterItems()) {
                                items.append(filterItem.getDisplayName()).append(Component.literal(", "));
                            }
                            items.append(Component.literal("]"));
                            serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.multiblock_debugger.catalyst.items", items));
                        }
                        if (!catalyst.getFilterTags().isEmpty()) {
                            var tags = Component.literal("[");
                            for (ResourceLocation filterTag : catalyst.getFilterTags()) {
                                tags.append(Component.literal(filterTag.toString())).append(Component.literal(", "));
                            }
                            tags.append(Component.literal("]"));
                            serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.multiblock_debugger.catalyst.tags", tags));
                        }
                    }
                } else {
                    var error = controller.getMultiblockState().error;
                    if (error != null) {
                        PacketDistributor.sendToPlayer(serverPlayer, new SPatternErrorPosPacket(error.getPos()));
                        serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.multiblock_debugger.failure.error.info", error.getErrorInfo()));
                    } else {
                        serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.multiblock_debugger.failure.no_error"));
                    }
                }
                isUsed = true;
                return InteractionResult.SUCCESS;
            } else if (isRecipeDebugger(stack) && getRecipe(stack) != null && serverPlayer.getServer() != null) {
                var machine = IMachine.ofMachine(player.level(), context.getClickedPos()).orElse(null);
                if (machine != null) {
                    var recipe = getRecipe(stack);
                    var recipeManager = serverPlayer.getServer().getRecipeManager();
                    for (MBDRecipeType recipeType : MBDRegistries.RECIPE_TYPES) {
                        for (var holder : recipeManager.getAllRecipesFor(recipeType)) {
                            if (Objects.equals(holder.id(), recipe)) {
                                var mbdRecipe = holder.value();
                                if (machine.getRecipeType() != recipeType) {
                                    serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.recipe_debugger.warning.recipe_type",
                                            Component.literal("id").withStyle(style ->
                                                    style.withColor(ChatFormatting.YELLOW)
                                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                                    Component.literal(machine.getRecipeType().toString())))),
                                            Component.literal("id").withStyle(style ->
                                                    style.withColor(ChatFormatting.YELLOW)
                                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                                    Component.literal(mbdRecipe.id.toString()))))
                                    ));
                                }
                                var result = mbdRecipe.matchRecipe(machine);
                                if (result.isSuccess()) {
                                    result = mbdRecipe.matchTickRecipe(machine);
                                    if (result.isSuccess()) {
                                        result = mbdRecipe.checkConditions(machine.getRecipeLogic());
                                    }
                                }
                                if (result.isSuccess()) {
                                    serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.recipe_debugger.raw.success",
                                            Component.literal("id").withStyle(style ->
                                                    style.withColor(ChatFormatting.YELLOW)
                                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                            Component.literal(mbdRecipe.id.toString()))))));
                                    var modifiedRecipe = machine.doModifyRecipe(mbdRecipe);
                                    if (modifiedRecipe == mbdRecipe) {
                                        serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.recipe_debugger.modified.empty"));
                                    } else if (modifiedRecipe == null) {
                                        serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.recipe_debugger.modified.failure.0"));
                                        serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.recipe_debugger.modified.failure.1"));
                                    } else {
                                        serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.recipe_debugger.modified.has"));
                                        result = modifiedRecipe.matchRecipe(machine);
                                        if (result.isSuccess()) {
                                            result = modifiedRecipe.matchTickRecipe(machine);
                                            if (result.isSuccess()) {
                                                result = modifiedRecipe.checkConditions(machine.getRecipeLogic());
                                            }
                                        }
                                        if (result.isSuccess()) {
                                            serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.recipe_debugger.modified.success"));
                                        } else {
                                            serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.recipe_debugger.modified.failure.0"));
                                            if (result.reason() != null) {
                                                serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.recipe_debugger.failure.reason").append(result.reason().get()));
                                            }
                                        }
                                    }
                                    isUsed = true;
                                    return InteractionResult.SUCCESS;
                                } else {
                                    serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.recipe_debugger.raw.failure.0",
                                            Component.literal("id").withStyle(style ->
                                                    style.withColor(ChatFormatting.YELLOW)
                                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                                    Component.literal(mbdRecipe.id.toString()))))));
                                    if (result.reason() != null) {
                                        serverPlayer.sendSystemMessage(Component.translatable("item.mbd2.mbd_gadgets.recipe_debugger.failure.reason").append(result.reason().get()));
                                    }
                                }
                                isUsed = true;
                                return InteractionResult.SUCCESS;
                            }
                        }
                    }
                    isUsed = true;
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public ModularUI createUI(HeldItemUIMenuType.HeldItemUIHolder holder) {
        var root = new UIElement();
        root.getLayout()
                .width(220).height(140)
                .paddingAll(6).gapAll(4)
                .flexDirection(FlexDirection.COLUMN);

        var title = new Label();
        title.setText(Component.translatable("item.mbd2.mbd_gadgets.recipe_debugger.recipe_id"));
        title.getLayout().widthPercent(100);
        root.addChild(title);

        var current = getRecipe(holder.itemStack);
        var textField = new TextField()
                .setAnyString()
                .setText(current == null ? "" : current.toString());
        textField.getLayout().widthPercent(100).height(14);

        var resultList = new ScrollerView();
        resultList.getLayout().widthPercent(100).flex(1);
        resultList.viewContainer.getLayout()
                .gapAll(1)
                .widthPercent(100);

        Runnable refreshResults = () -> {
            resultList.viewContainer.clearAllChildren();
            var query = textField.getText().toLowerCase();
            var level = holder.player.level();
            if (level == null) return;
            var recipeManager = level.getRecipeManager();
            int count = 0;
            outer:
            for (MBDRecipeType recipeType : MBDRegistries.RECIPE_TYPES) {
                for (var recipeHolder : recipeManager.getAllRecipesFor(recipeType)) {
                    var id = recipeHolder.id();
                    if (!query.isEmpty() && !id.toString().toLowerCase().contains(query)) continue;
                    var btn = new Button()
                            .setText(id.toString())
                            .setOnClick(e -> {
                                setRecipe(holder.player.getItemInHand(holder.hand), id);
                                textField.setText(id.toString());
                            });
                    btn.getLayout().widthPercent(100).height(12);
                    resultList.viewContainer.addChild(btn);
                    if (++count >= 32) break outer;
                }
            }
        };

        textField.setTextResponder(value -> refreshResults.run());
        refreshResults.run();

        root.addChildren(textField, resultList);

        return new ModularUI(UI.of(root, StylesheetManager.ORE_MERGED), holder.player);
    }
}
