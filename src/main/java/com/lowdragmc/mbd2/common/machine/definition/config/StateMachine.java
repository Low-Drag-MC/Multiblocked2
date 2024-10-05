package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class StateMachine<T extends MachineState> implements ITagSerializable<CompoundTag> {
    @Getter
    protected final T rootState;
    // runtime
    protected final Map<String, T> states = new HashMap<>();

    public StateMachine(T rootState) {
        this.rootState = rootState;
        initStateMachine();
    }

    public static <T extends MachineState> T createSingleDefault(Supplier<MachineState.Builder<T>> builderCreator, IRenderer renderer) {
        var builder = builderCreator.get().name("base")
                .renderer(renderer)
                .shape(Shapes.block())
                .lightLevel(0)
                .child(builderCreator.get()
                        .name("working")
                        .child(builderCreator.get()
                                .name("waiting")
                                .build())
                        .build())
                .child(builderCreator.get()
                        .name("suspend")
                        .build());
        return builder.build();
    }

    public static <T extends MachineState> T createMultiblockDefault(Supplier<MachineState.Builder<T>> builderCreator, IRenderer renderer) {
        var builder = builderCreator.get().name("base")
                .renderer(renderer)
                .shape(Shapes.block())
                .lightLevel(0)
                .child(builderCreator.get()
                        .name("formed")
                        .child(builderCreator.get()
                                .name("working")
                                .child(builderCreator.get()
                                        .name("waiting")
                                        .build())
                                .build())
                        .child(builderCreator.get()
                                .name("suspend")
                                .build())
                        .build());
        return builder.build();
    }

    public static <T extends MachineState> T createDefault(Supplier<MachineState.Builder<T>> builderCreator) {
        return createSingleDefault(builderCreator, IRenderer.EMPTY);
    }

    @Override
    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        tag.put("root", rootState.serializeNBT());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        rootState.deserializeNBT(tag.getCompound("root"));
        initStateMachine();
    }

    /**
     * Initialize the state machine. It should be called after the state machine is changed.
     */
    public void initStateMachine() {
        states.clear();
        this.rootState.init(this);
    }

    /**
     * Add a state to the state machine. It should be called only in the {@link MachineState#init(StateMachine)} method.
     * <br/>
     * In general, you don't need to call this method. it will be called automatically during {@link StateMachine#initStateMachine()}
     */
    protected void addState(T state) {
        states.put(state.name(), state);
    }

    public T getState(String name) {
        return states.getOrDefault(name, rootState);
    }

    public boolean hasState(String name) {
        return states.containsKey(name);
    }
}
