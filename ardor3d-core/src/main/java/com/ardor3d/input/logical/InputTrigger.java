/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.logical;

import com.ardor3d.annotation.Immutable;
import com.ardor3d.framework.Canvas;
import com.google.common.base.Predicate;

/**
 * Defines an action to be performed when a specific input condition is met.
 */
@Immutable
public final class InputTrigger {
    private final Predicate<TwoInputStates> condition;
    private final TriggerAction action;

    public InputTrigger(final Predicate<TwoInputStates> condition, final TriggerAction action) {
        this.condition = condition;
        this.action = action;
    }

    /**
     * Checks if the condition is applicable, and if so, performs the action.
     * 
     * @param source
     *            the Canvas that was the source of the current input
     * @param states
     *            the input states to check
     * @param tpf
     *            the time per frame in seconds
     */
    void performIfValid(final Canvas source, final TwoInputStates states, final double tpf) {
        if (condition.apply(states)) {
            action.perform(source, states, tpf);
        }
    }
}
