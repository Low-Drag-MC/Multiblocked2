package com.lowdragmc.mbd2.api.pattern.predicates;

import com.google.common.base.Suppliers;
import com.lowdragmc.lowdraglib2.configurator.ui.ArrayConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.TagKeySearchComponent;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.data.BlockInfo;
import lombok.NoArgsConstructor;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Arrays;
import java.util.Objects;

@LDLRegister(name = "tags", registry = "mbd2:pattern_predicate", group = "predicate")
@NoArgsConstructor
public class PredicateTags extends PatternPredicate {

    @Persisted
    protected ResourceLocation[] tags = new ResourceLocation[] {};

    public PredicateTags(ResourceLocation... tags) {
        this.tags = tags;
        buildPredicate();
    }

    @Override
    public PatternPredicate buildPredicate() {
        tags = Arrays.stream(tags).filter(Objects::nonNull).toArray(ResourceLocation[]::new);
        if (tags.length == 0) tags = new ResourceLocation[]{BlockTags.SAND.location()};
        var tagKeys = (TagKey<Block>[]) Arrays.stream(tags).map(BlockTags::create).toArray(TagKey[]::new);
        predicate = state -> Arrays.stream(tagKeys).anyMatch(tagKey -> state.getBlockState().getBlock().builtInRegistryHolder().is(tagKey));
        candidates = Suppliers.memoize(() -> Arrays.stream(tagKeys).flatMap(tag -> {
            var opt = BuiltInRegistries.BLOCK.getTag(tag);
            if (opt.isPresent()) {
                return opt.get().stream().map(Holder::value);
            }
            return Arrays.stream(new Block[]{Blocks.BARRIER});
        }).map(BlockInfo::fromBlock).toArray(BlockInfo[]::new));
        return super.buildPredicate();
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        super.buildConfigurator(father);
        var tagsConfigurator = new ArrayConfiguratorGroup<>("config.predicate.tags", false,
                () -> Arrays.stream(tags).toList(), (getter, setter) ->
                new TagKeySearchComponent.Block("",
                        () -> BlockTags.create(getter.get()),
                        tagKey -> setter.accept(tagKey.location()),
                        BlockTags.SAND,
                        true
                ), true);
        tagsConfigurator.setAddDefault(BlockTags.SAND::location);
        tagsConfigurator.setOnUpdate(list -> {
            tags = list.toArray(new ResourceLocation[0]);
            buildPredicate();
        });
        father.addConfigurators(tagsConfigurator);
    }
}
