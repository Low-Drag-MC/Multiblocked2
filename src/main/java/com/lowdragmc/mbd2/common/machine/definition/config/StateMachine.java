package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleInteger;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleRenderer;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleShape;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class StateMachine implements ITagSerializable<CompoundTag> {
    @Getter
    protected final MachineState rootState;
    protected final Map<String, MachineState> states = new HashMap<>();

    public StateMachine(MachineState rootState) {
        this.rootState = rootState;
        initStateMachine();
    }

    public static StateMachine create(Consumer<MachineState.MachineStateBuilder> builderConsumer) {
        var stateBuilder = MachineState.builder()
                .name("base")
                .child(MachineState.builder()
                        .name("working")
                        .build())
                .child(MachineState.builder()
                        .name("waiting")
                        .build())
                .child(MachineState.builder()
                        .name("suspend")
                        .build());
        builderConsumer.accept(stateBuilder);
        return new StateMachine(stateBuilder.build());
    }

    public static StateMachine createDefault() {
        return create(builder -> builder
                .renderer(new ToggleRenderer(IRenderer.EMPTY))
                .shape(new ToggleShape(Shapes.block()))
                .lightLevel(new ToggleInteger(0)));
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
    protected void addState(MachineState state) {
        states.put(state.name(), state);
    }

    public MachineState getState(String name) {
        return states.getOrDefault(name, rootState);
    }

    public boolean hasState(String name) {
        return states.containsKey(name);
    }
}
