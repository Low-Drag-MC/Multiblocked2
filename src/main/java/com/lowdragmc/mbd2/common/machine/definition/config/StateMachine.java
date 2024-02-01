package com.lowdragmc.mbd2.common.machine.definition.config;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class StateMachine {
    @Getter
    protected final MachineState rootState;
    protected final Map<String, MachineState> states = new HashMap<>();

    public StateMachine(MachineState rootState) {
        this.rootState = rootState;
        this.rootState.init(this);
    }

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
