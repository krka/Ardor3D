/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ardor3d.annotation.Immutable;

@Immutable
public class ControllerState {

    public static final ControllerState NOTHING = new ControllerState();

    private Map<String, Map<String, Float>> controllerStates = new LinkedHashMap<String, Map<String, Float>>();
    private final List<ControllerEvent> eventsSinceLastState = new ArrayList<ControllerEvent>();

    /**
     * Sets a components state
     */
    public void set(final String controllerName, final String componentName, final float value) {
        Map<String, Float> controllerState = null;
        if (controllerStates.containsKey(controllerName)) {
            controllerState = controllerStates.get(controllerName);
        } else {
            controllerState = new LinkedHashMap<String, Float>();
            controllerStates.put(controllerName, controllerState);
        }

        controllerState.put(componentName, value);
    }

    @Override
    public boolean equals(final Object obj) {
        boolean retval = false;
        if (obj instanceof ControllerState) {
            final ControllerState other = (ControllerState) obj;
            retval = other.controllerStates.equals(controllerStates);
        }

        return retval;
    }

    @Override
    public String toString() {
        final StringBuilder stateString = new StringBuilder("ControllerState: ");

        for (final String controllerStateKey : controllerStates.keySet()) {
            final Map<String, Float> state = controllerStates.get(controllerStateKey);
            stateString.append("[").append(controllerStateKey);
            for (final String stateKey : state.keySet()) {
                stateString.append("[").append(stateKey).append(":").append(state.get(stateKey)).append("]");
            }
            stateString.append("]");
        }

        return stateString.toString();
    }

    public ControllerState snapshot() {
        final ControllerState snapshot = new ControllerState();
        snapshot.controllerStates = duplicateStates();
        snapshot.eventsSinceLastState.addAll(eventsSinceLastState);

        return snapshot;
    }

    private Map<String, Map<String, Float>> duplicateStates() {
        final Map<String, Map<String, Float>> duplicate = new LinkedHashMap<String, Map<String, Float>>();

        for (final String controllerStateKey : controllerStates.keySet()) {
            duplicate.put(controllerStateKey, duplicateState(controllerStates.get(controllerStateKey)));
        }

        return duplicate;
    }

    private Map<String, Float> duplicateState(final Map<String, Float> original) {
        final Map<String, Float> duplicate = new LinkedHashMap<String, Float>();

        for (final String key : original.keySet()) {
            duplicate.put(key, original.get(key));
        }

        return duplicate;
    }

    public void addEvent(final ControllerEvent event) {
        eventsSinceLastState.add(event);
        set(event.getControllerName(), event.getComponentName(), event.getValue());
    }

    public List<ControllerEvent> getEvents() {
        Collections.sort(eventsSinceLastState, new Comparator<ControllerEvent>() {
            public int compare(final ControllerEvent o1, final ControllerEvent o2) {
                return (int) (o2.getNanos() - o1.getNanos());
            }
        });

        return Collections.unmodifiableList(eventsSinceLastState);
    }

    public void clearEvents() {
        eventsSinceLastState.clear();
    }

    public List<String> getControllerNames() {
        return new ArrayList<String>(controllerStates.keySet());
    }

    public List<String> getControllerComponentNames(final String controller) {
        return new ArrayList<String>(controllerStates.get(controller).keySet());
    }
}
