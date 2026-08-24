package com.lowdragmc.mbd2.common.blueprint;

import com.lowdragmc.kilagraph.graph.type.KGTypeHandles;
import com.lowdragmc.kilagraph.graph.ui.KGUITypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandleHelpers;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.api.machine.IMultiPart;
import com.lowdragmc.mbd2.api.pattern.MultiblockState;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.api.recipe.content.ContentModifier;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import com.lowdragmc.mbd2.common.machine.MBDPartMachine;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineEvent;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.TraitDefinition;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * MBD2-defined {@link TypeHandle}s for {@link MachineBlueprintGraph}.
 *
 * <p>Every handle is minted with {@link TypeHandleHelpers#fromType(java.lang.reflect.Type, String)},
 * so its identification is the class name — which is exactly what {@link KGTypeHandles#handleFor}
 * falls through to. A node declaring {@code @OutputPort public MBDMachine machine} therefore lands on
 * the handle registered here without needing an override entry.</p>
 *
 * <p>Colour and default value are cached lazily <b>per handle instance</b> by LDLib2, so a property
 * attached after something has already asked for it is silently ignored. Both are attached in the one
 * call that mints the handle; see {@link #mbd}.</p>
 *
 * <p>Most of these are <b>wire-only</b>: they name a live game object ({@code MBDMachine},
 * {@code RecipeLogic}, {@code MBDRecipe}) with no {@code AccessorRegistries} entry, so KilaGraph's
 * {@code NodeMetadata} drops the inline editor and the embedded constant for them. Only the two enums
 * are authorable as literals — {@code EnumAccessor} covers every enum — which is why
 * {@link #LIBRARY_TYPES} is much shorter than {@link #ALL}.</p>
 */
public final class MBDTypeHandles {

    // ---- machines ----------------------------------------------------------------------------
    /** The machine a blueprint is running for. The central context object of every machine node. */
    public static final TypeHandle MACHINE;
    public static final TypeHandle MULTIBLOCK_MACHINE;
    public static final TypeHandle PART_MACHINE;
    public static final TypeHandle MACHINE_DEFINITION;
    public static final TypeHandle MACHINE_STATE;

    // ---- recipe -----------------------------------------------------------------------------
    public static final TypeHandle RECIPE_LOGIC;
    /** {@code RecipeLogic.Status} — an enum, so authorable as a literal. */
    public static final TypeHandle RECIPE_STATUS;
    public static final TypeHandle RECIPE;
    public static final TypeHandle RECIPE_TYPE;
    public static final TypeHandle RECIPE_CAPABILITY;
    public static final TypeHandle CONTENT;
    public static final TypeHandle CONTENT_MODIFIER;
    /** {@code IO} — an enum, so authorable as a literal. */
    public static final TypeHandle IO_HANDLE;

    // ---- traits -----------------------------------------------------------------------------
    public static final TypeHandle TRAIT;
    public static final TypeHandle TRAIT_DEFINITION;

    // ---- multiblock -------------------------------------------------------------------------
    public static final TypeHandle MULTIBLOCK_STATE;
    public static final TypeHandle MULTI_PART;
    public static final TypeHandle MULTI_CONTROLLER;

    // ---- events -----------------------------------------------------------------------------
    /** The event currently being dispatched. Mostly for the generic event-control nodes. */
    public static final TypeHandle MACHINE_EVENT;

    // ---- interaction --------------------------------------------------------------------------
    //
    // Vanilla types the machine events carry that LDLib2/KilaGraph do not mint. The three enums are
    // authorable (every enum gets LDLib2's EnumAccessor, so a dropdown and a serialisable constant come
    // for free) — which matters, because writing an InteractionResult back onto a use event is the whole
    // point of hooking it. BlockHitResult is wire-only.
    public static final TypeHandle INTERACTION_HAND;
    public static final TypeHandle INTERACTION_RESULT;
    public static final TypeHandle ITEM_INTERACTION_RESULT;
    public static final TypeHandle BLOCK_HIT_RESULT;
    //
    // No handle for LDLib2's UI here. KilaGraph mints that one in KGUITypeHandles, and minting it
    // again under a different friendly name is not a duplicate that merely wastes a line — TypeHandle
    // identification is the class name, and fromType throws when the same identification already
    // carries a different name. Two mods naming one type differently is a startup crash.

    /** Everything a port may carry — feeds {@code MachineBlueprintGraph.getSupportTypes()}. */
    public static final List<TypeHandle> ALL;
    /** The subset a literal can be authored for — feeds {@code getLibrarySupportTypes()}. */
    public static final List<TypeHandle> LIBRARY_TYPES;

    static {
        // KilaGraph's handles first: a couple of ours (recipe contents, machine info blocks) declare
        // ports of its types, and its LIST/MAP overrides must exist before any node class is scanned.
        KGTypeHandles.init();
        // The UI handles too, because MachineUIEvent's node declares a UI port. LDLib2 caches a
        // handle's colour on first read, so a port scanned before KGUITypeHandles has run would pin
        // the UI type colourless for the rest of the session.
        KGUITypeHandles.init();

        MACHINE = mbd(MBDMachine.class, "Machine", 0xFF5E9CD3);
        MULTIBLOCK_MACHINE = mbd(MBDMultiblockMachine.class, "Multiblock Machine", 0xFF4A7FB0);
        PART_MACHINE = mbd(MBDPartMachine.class, "Part Machine", 0xFF7FB3DE);
        MACHINE_DEFINITION = mbd(MBDMachineDefinition.class, "Machine Definition", 0xFF9AB8D4);
        MACHINE_STATE = mbd(MachineState.class, "Machine State", 0xFFB8CDE0);

        RECIPE_LOGIC = mbd(RecipeLogic.class, "Recipe Logic", 0xFFD9A05B);
        RECIPE_STATUS = mbd(RecipeLogic.Status.class, "Recipe Status", 0xFFE0B87A,
                () -> RecipeLogic.Status.IDLE);
        RECIPE = mbd(MBDRecipe.class, "Recipe", 0xFFC97F3E);
        RECIPE_TYPE = mbd(MBDRecipeType.class, "Recipe Type", 0xFFB06E33);
        RECIPE_CAPABILITY = mbd(RecipeCapability.class, "Recipe Capability", 0xFFE8C89A);
        CONTENT = mbd(Content.class, "Recipe Content", 0xFFDCC0A0);
        CONTENT_MODIFIER = mbd(ContentModifier.class, "Content Modifier", 0xFFCFAE7F);
        IO_HANDLE = mbd(IO.class, "IO", 0xFF8FBF7F, () -> IO.BOTH);

        TRAIT = mbd(ITrait.class, "Trait", 0xFFA88FD3);
        TRAIT_DEFINITION = mbd(TraitDefinition.class, "Trait Definition", 0xFFC0AEE0);

        MULTIBLOCK_STATE = mbd(MultiblockState.class, "Multiblock State", 0xFF7FC0BF);
        MULTI_PART = mbd(IMultiPart.class, "Multiblock Part", 0xFF9AD0CF);
        MULTI_CONTROLLER = mbd(IMultiController.class, "Multiblock Controller", 0xFF6BAAA9);

        MACHINE_EVENT = mbd(MachineEvent.class, "Machine Event", 0xFFD37F7F);

        INTERACTION_HAND = mbd(InteractionHand.class, "Interaction Hand", 0xFFBFA77F,
                () -> InteractionHand.MAIN_HAND);
        INTERACTION_RESULT = mbd(InteractionResult.class, "Interaction Result", 0xFFCFB78F,
                () -> InteractionResult.PASS);
        ITEM_INTERACTION_RESULT = mbd(ItemInteractionResult.class, "Item Interaction Result", 0xFFDFC79F,
                () -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION);
        BLOCK_HIT_RESULT = mbd(BlockHitResult.class, "Block Hit Result", 0xFF9FB8A8);

        ALL = List.of(MACHINE, MULTIBLOCK_MACHINE, PART_MACHINE, MACHINE_DEFINITION, MACHINE_STATE,
                RECIPE_LOGIC, RECIPE_STATUS, RECIPE, RECIPE_TYPE, RECIPE_CAPABILITY, CONTENT,
                CONTENT_MODIFIER, IO_HANDLE, TRAIT, TRAIT_DEFINITION, MULTIBLOCK_STATE, MULTI_PART,
                MULTI_CONTROLLER, MACHINE_EVENT, INTERACTION_HAND, INTERACTION_RESULT,
                ITEM_INTERACTION_RESULT, BLOCK_HIT_RESULT);

        // Only the enums. Everything else is a live object resolved from the machine or the event —
        // offering a constant node for it would render an empty inspector row and emit null, which is
        // worse than not offering it (same reasoning as KilaGraph's BlueprintGraph).
        LIBRARY_TYPES = List.of(RECIPE_STATUS, IO_HANDLE, INTERACTION_HAND, INTERACTION_RESULT,
                ITEM_INTERACTION_RESULT);
    }

    private MBDTypeHandles() {}

    private static TypeHandle mbd(Class<?> javaType, String display, int colour) {
        return mbd(javaType, display, colour, null);
    }

    private static TypeHandle mbd(Class<?> javaType, String display, int colour,
                                  @Nullable Supplier<Object> defaultValue) {
        var handle = TypeHandleHelpers.fromType(javaType, display);
        TypeHandleHelpers.setCustomColor(handle, colour);
        if (defaultValue != null) {
            TypeHandleHelpers.setCustomDefaultValue(handle, defaultValue);
        }
        return handle;
    }

    /** Force static init from elsewhere. */
    public static void init() {
    }
}
