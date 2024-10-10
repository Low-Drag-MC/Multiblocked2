package com.lowdragmc.mbd2.api.pattern.predicates;

import com.google.common.base.Suppliers;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ArrayConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import lombok.NoArgsConstructor;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.Objects;

@LDLRegister(name = "tags", group = "predicate")
@NoArgsConstructor
public class PredicateTags extends SimplePredicate {

    protected ResourceLocation[] tags = new ResourceLocation[] {};

    public PredicateTags(ResourceLocation... tags) {
        this.tags = tags;
        buildPredicate();
    }

    @Override
    public SimplePredicate buildPredicate() {
        tags = Arrays.stream(tags).filter(Objects::nonNull).toArray(ResourceLocation[]::new);
        if (tags.length == 0) tags = new ResourceLocation[]{BlockTags.SAND.location()};
        var tagKeys = (TagKey<Block>[]) Arrays.stream(tags).map(BlockTags::create).toArray(TagKey[]::new);
        predicate = state -> Arrays.stream(tagKeys).anyMatch(tagKey -> state.getBlockState().getBlock().builtInRegistryHolder().is(tagKey));
        candidates = Suppliers.memoize(() -> Arrays.stream(tagKeys).flatMap(tag -> {
            var opt = BuiltInRegistries.BLOCK.getTag(tag);
            if (opt.isPresent()) {
                return opt.get().stream().map(Holder::get);
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
                new SearchComponentConfigurator<>("", getter, setter, BlockTags.SAND.location(), true, (word, find) -> {
                    var tags = ForgeRegistries.BLOCKS.tags();
                    if (tags == null) return;
                    for (var tag : tags) {
                        if (Thread.currentThread().isInterrupted()) return;
                        var tagKey = tag.getKey().location();
                        if (tagKey.toString().toLowerCase().contains(word.toLowerCase())) {
                            find.accept(tagKey);
                        }
                    }}, ResourceLocation::toString), true);
        tagsConfigurator.setAddDefault(BlockTags.SAND::location);
        tagsConfigurator.setOnAdd(value -> {
            tags = Arrays.copyOf(this.tags, this.tags.length + 1);
            tags[tags.length - 1] = value;
            buildPredicate();
        });
        tagsConfigurator.setOnRemove(value -> {
            tags = Arrays.stream(this.tags).filter(tag -> !tag.equals(value)).toArray(ResourceLocation[]::new);
            buildPredicate();
        });
        tagsConfigurator.setOnUpdate(list -> {
            tags = list.toArray(new ResourceLocation[0]);
            buildPredicate();
        });
        father.addConfigurators(tagsConfigurator);
    }
}
