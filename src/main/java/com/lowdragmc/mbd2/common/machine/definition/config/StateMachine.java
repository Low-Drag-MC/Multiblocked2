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
