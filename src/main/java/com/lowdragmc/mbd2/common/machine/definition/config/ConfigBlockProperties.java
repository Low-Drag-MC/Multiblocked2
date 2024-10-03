package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.syncdata.IPersistedSerializable;
import com.lowdragmc.mbd2.api.block.RotationState;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

@Getter
@Accessors(fluent = true)
@Builder
public class ConfigBlockProperties implements IPersistedSerializable {
    @Getter
    @Setter
    public static class RenderTypes implements IPersistedSerializable {
        @Configurable(name = "config.block_properties.render_types.solid")
        private boolean solid;
        @Configurable(name = "config.block_properties.render_types.cutout")
        private boolean cutout = true;
        @Configurable(name = "config.block_properties.render_types.cutout_mipped")
        private boolean cutoutMipped;
        @Configurable(name = "config.block_properties.render_types.translucent")
        private boolean translucent;
    }

    @Configurable(name = "config.block_properties.render_types", subConfigurable = true, tips = "config.block_properties.render_types.tooltip")
    @Builder.Default
    private final RenderTypes renderTypes = new RenderTypes();

    @Configurable(name = "config.block_properties.use_ao", tips = "config.block_properties.use_ao.tooltip")
    @Builder.Default
    private boolean useAO = true;

    @Configurable(name = "config.block_properties.rotation_state", tips = "config.block_properties.rotation_state.tooltip")
    @Builder.Default
    private RotationState rotationState = RotationState.NON_Y_AXIS;

    @Configurable(name = "config.block_properties.has_collision", tips = "config.block_properties.has_collision.tooltip")
    @Builder.Default
    private boolean hasCollision = true;

    @Configurable(name = "config.block_properties.can_occlude", tips = "config.block_properties.can_occlude.tooltip")
    @Builder.Default
    private boolean canOcclude = true;

    @Configurable(name = "config.block_properties.dynamic_shape", tips = "config.block_properties.dynamic_shape.tooltip")
    @Builder.Default
    private boolean dynamicShape = false;

    @Configurable(name = "config.block_properties.ignited_by_lava", tips = "config.block_properties.ignited_by_lava.tooltip")
    @Builder.Default
    private boolean ignitedByLava = false;

    @Configurable(name = "config.block_properties.is_air", tips = "config.block_properties.is_air.tooltip")
    @Builder.Default
    private boolean isAir = false;

    @Configurable(name = "config.block_properties.is_suffocating", tips = "config.block_properties.is_suffocating.tooltip")
    @Builder.Default
    private boolean isSuffocating = true;

    @Configurable(name = "config.block_properties.emissive", tips = "config.block_properties.emissive.tooltip")
    @Builder.Default
    private boolean emissive = false;

    @Configurable(name = "config.block_properties.friction", tips = "config.block_properties.friction.tooltip")
    @NumberRange(range = {0, Float.MAX_VALUE})
    @Builder.Default
    private float friction = 0.6f;

    @Configurable(name = "config.block_properties.speed_factor", tips = "config.block_properties.speed_factor.tooltip")
    @NumberRange(range = {0, Float.MAX_VALUE})
    @Builder.Default
    private float speedFactor = 1.0f;

    @Configurable(name = "config.block_properties.jump_factor", tips = "config.block_properties.jump_factor.tooltip")
    @NumberRange(range = {0, Float.MAX_VALUE})
    @Builder.Default
    private float jumpFactor = 1.0f;

    @Configurable(name = "config.block_properties.destroy_time", tips = "config.block_properties.destroy_time.tooltip")
    @NumberRange(range = {0, Float.MAX_VALUE})
    @Builder.Default
    private float destroyTime = 1.5f;

    @Configurable(name = "config.block_properties.explosion_resistance", tips = "config.block_properties.explosion_resistance.tooltip")
    @NumberRange(range = {0, Float.MAX_VALUE})
    @Builder.Default
    private float explosionResistance = 6.0f;

    @Configurable(name = "config.block_properties.sound", tips = "config.block_properties.sound.tooltip")
    @Builder.Default
    private Sound sound = Sound.STONE;

    public BlockBehaviour.Properties apply(StateMachine stateMachine, BlockBehaviour.Properties properties) {
        if (hasCollision) {
            properties = properties.noOcclusion();
        }
        if (!canOcclude) {
            properties = properties.noOcclusion();
        }
        if (dynamicShape) {
            properties = properties.dynamicShape();
        }
        if (ignitedByLava) {
            properties = properties.ignitedByLava();
        }
        if (isAir) {
            properties = properties.air();
        }
        if (isSuffocating) {
            properties = properties.isSuffocating((state, level, pos) -> true);
        }
        if (emissive) {
            properties = properties.emissiveRendering((state, level, pos) -> true);
        }
        properties = properties.friction(friction);
        properties = properties.speedFactor(speedFactor);
        properties = properties.jumpFactor(jumpFactor);
        properties = properties.destroyTime(destroyTime);
        properties = properties.explosionResistance(explosionResistance);
        properties = properties.sound(sound.soundType);
        return properties;
    }

    public enum Sound {
        EMPTY(SoundType.EMPTY),
        WOOD(SoundType.WOOD),
        GRAVEL(SoundType.GRAVEL),
        GRASS(SoundType.GRASS),
        LILY_PAD(SoundType.LILY_PAD),
        STONE(SoundType.STONE),
        METAL(SoundType.METAL),
        GLASS(SoundType.GLASS),
        WOOL(SoundType.WOOL),
        SAND(SoundType.SAND),
        SNOW(SoundType.SNOW),
        POWDER_SNOW(SoundType.POWDER_SNOW),
        LADDER(SoundType.LADDER),
        ANVIL(SoundType.ANVIL),
        SLIME_BLOCK(SoundType.SLIME_BLOCK),
        HONEY_BLOCK(SoundType.HONEY_BLOCK),
        WET_GRASS(SoundType.WET_GRASS),
        CORAL_BLOCK(SoundType.CORAL_BLOCK),
        BAMBOO(SoundType.BAMBOO),
        BAMBOO_SAPLING(SoundType.BAMBOO_SAPLING),
        SCAFFOLDING(SoundType.SCAFFOLDING),
        SWEET_BERRY_BUSH(SoundType.SWEET_BERRY_BUSH),
        CROP(SoundType.CROP),
        HARD_CROP(SoundType.HARD_CROP),
        VINE(SoundType.VINE),
        NETHER_WART(SoundType.NETHER_WART),
        LANTERN(SoundType.LANTERN),
        STEM(SoundType.STEM),
        NYLIUM(SoundType.NYLIUM),
        FUNGUS(SoundType.FUNGUS),
        ROOTS(SoundType.ROOTS),
        SHROOMLIGHT(SoundType.SHROOMLIGHT),
        WEEPING_VINES(SoundType.WEEPING_VINES),
        TWISTING_VINES(SoundType.TWISTING_VINES),
        SOUL_SAND(SoundType.SOUL_SAND),
        SOUL_SOIL(SoundType.SOUL_SOIL),
        BASALT(SoundType.BASALT),
        WART_BLOCK(SoundType.WART_BLOCK),
        NETHERRACK(SoundType.NETHERRACK),
        NETHER_BRICKS(SoundType.NETHER_BRICKS),
        NETHER_SPROUTS(SoundType.NETHER_SPROUTS),
        NETHER_ORE(SoundType.NETHER_ORE),
        BONE_BLOCK(SoundType.BONE_BLOCK),
        NETHERITE_BLOCK(SoundType.NETHERITE_BLOCK),
        ANCIENT_DEBRIS(SoundType.ANCIENT_DEBRIS),
        LODESTONE(SoundType.LODESTONE),
        CHAIN(SoundType.CHAIN),
        NETHER_GOLD_ORE(SoundType.NETHER_GOLD_ORE),
        GILDED_BLACKSTONE(SoundType.GILDED_BLACKSTONE),
        CANDLE(SoundType.CANDLE),
        AMETHYST(SoundType.AMETHYST),
        AMETHYST_CLUSTER(SoundType.AMETHYST_CLUSTER),
        SMALL_AMETHYST_BUD(SoundType.SMALL_AMETHYST_BUD),
        MEDIUM_AMETHYST_BUD(SoundType.MEDIUM_AMETHYST_BUD),
        LARGE_AMETHYST_BUD(SoundType.LARGE_AMETHYST_BUD),
        TUFF(SoundType.TUFF),
        CALCITE(SoundType.CALCITE),
        DRIPSTONE_BLOCK(SoundType.DRIPSTONE_BLOCK),
        POINTED_DRIPSTONE(SoundType.POINTED_DRIPSTONE),
        COPPER(SoundType.COPPER),
        CAVE_VINES(SoundType.CAVE_VINES),
        SPORE_BLOSSOM(SoundType.SPORE_BLOSSOM),
        AZALEA(SoundType.AZALEA),
        FLOWERING_AZALEA(SoundType.FLOWERING_AZALEA),
        MOSS_CARPET(SoundType.MOSS_CARPET),
        PINK_PETALS(SoundType.PINK_PETALS),
        MOSS(SoundType.MOSS),
        BIG_DRIPLEAF(SoundType.BIG_DRIPLEAF),
        SMALL_DRIPLEAF(SoundType.SMALL_DRIPLEAF),
        ROOTED_DIRT(SoundType.ROOTED_DIRT),
        HANGING_ROOTS(SoundType.HANGING_ROOTS),
        AZALEA_LEAVES(SoundType.AZALEA_LEAVES),
        SCULK_SENSOR(SoundType.SCULK_SENSOR),
        SCULK_CATALYST(SoundType.SCULK_CATALYST),
        SCULK(SoundType.SCULK),
        SCULK_VEIN(SoundType.SCULK_VEIN),
        SCULK_SHRIEKER(SoundType.SCULK_SHRIEKER),
        GLOW_LICHEN(SoundType.GLOW_LICHEN),
        DEEPSLATE(SoundType.DEEPSLATE),
        DEEPSLATE_BRICKS(SoundType.DEEPSLATE_BRICKS),
        DEEPSLATE_TILES(SoundType.DEEPSLATE_TILES),
        POLISHED_DEEPSLATE(SoundType.POLISHED_DEEPSLATE),
        FROGLIGHT(SoundType.FROGLIGHT),
        FROGSPAWN(SoundType.FROGSPAWN),
        MANGROVE_ROOTS(SoundType.MANGROVE_ROOTS),
        MUDDY_MANGROVE_ROOTS(SoundType.MUDDY_MANGROVE_ROOTS),
        MUD(SoundType.MUD),
        MUD_BRICKS(SoundType.MUD_BRICKS),
        PACKED_MUD(SoundType.PACKED_MUD),
        HANGING_SIGN(SoundType.HANGING_SIGN),
        NETHER_WOOD_HANGING_SIGN(SoundType.NETHER_WOOD_HANGING_SIGN),
        BAMBOO_WOOD_HANGING_SIGN(SoundType.BAMBOO_WOOD_HANGING_SIGN),
        BAMBOO_WOOD(SoundType.BAMBOO_WOOD),
        NETHER_WOOD(SoundType.NETHER_WOOD),
        CHERRY_WOOD(SoundType.CHERRY_WOOD),
        CHERRY_SAPLING(SoundType.CHERRY_SAPLING),
        CHERRY_LEAVES(SoundType.CHERRY_LEAVES),
        CHERRY_WOOD_HANGING_SIGN(SoundType.CHERRY_WOOD_HANGING_SIGN),
        CHISELED_BOOKSHELF(SoundType.CHISELED_BOOKSHELF),
        SUSPICIOUS_SAND(SoundType.SUSPICIOUS_SAND),
        SUSPICIOUS_GRAVEL(SoundType.SUSPICIOUS_GRAVEL),
        DECORATED_POT(SoundType.DECORATED_POT),
        DECORATED_POT_CRACKED(SoundType.DECORATED_POT_CRACKED);

        public final SoundType soundType;

        Sound(SoundType soundType) {
            this.soundType = soundType;
        }
    }
}
