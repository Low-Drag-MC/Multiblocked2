package com.lowdragmc.mbd2.api.pattern.predicates;

import com.google.common.base.Suppliers;
import com.lowdragmc.lowdraglib.gui.editor.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import lombok.NoArgsConstructor;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Objects;

@LDLRegister(name = "fluids", group = "predicate")
@NoArgsConstructor
public class PredicateFluids extends SimplePredicate {

    @Configurable(name = "config.predicate.fluids", tips = "config.predicate.fluids.tooltip", collapse = false)
    protected Fluid[] fluids = new Fluid[] {Fluids.WATER};

    public PredicateFluids(Fluid... fluids) {
        this.fluids = fluids;
        buildPredicate();
    }

    @ConfigSetter(field = "fluids")
    public void setFluids(Fluid[] fluids) {
        this.fluids = fluids;
        buildPredicate();
    }

    @Override
    public SimplePredicate buildPredicate() {
        fluids = Arrays.stream(fluids).filter(Objects::nonNull).toArray(Fluid[]::new);
        if (fluids.length == 0) fluids = new Fluid[]{Fluids.WATER};
        predicate = state -> ArrayUtils.contains(fluids, state.getBlockState().getFluidState().getType());
        candidates = Suppliers.memoize(() -> Arrays.stream(fluids).map(fluid -> new BlockInfo(fluid.defaultFluidState().createLegacyBlock(), false,
                fluid.getBucket().getDefaultInstance(), null)).toArray(BlockInfo[]::new));
        return super.buildPredicate();
    }

}
