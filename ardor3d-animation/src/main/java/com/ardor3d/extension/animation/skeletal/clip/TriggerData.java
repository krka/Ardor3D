/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal.clip;

import java.io.IOException;

import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * Maintains the current trigger name and armed status for a TriggerChannel.
 */
public class TriggerData implements Savable {

    /** The current trigger name. */
    private String _currentTrigger = null;

    /**
     * The current channel sample index. We keep this to make sure we don't miss two channels in a row with the same
     * trigger name.
     */
    private int _currentIndex = 0;

    /** If true, we are armed - we have had a trigger set and have not executed it. */
    private boolean _armed = false;

    public String getCurrentTrigger() {
        return _currentTrigger;
    }

    public int getCurrentIndex() {
        return _currentIndex;
    }

    public void setArmed(final boolean armed) {
        _armed = armed;
    }

    public boolean isArmed() {
        return _armed;
    }

    /**
     * Try to set a given trigger/index as armed. If we already have this trigger and index set, we don't change the
     * state of armed.
     * 
     * @param trigger
     *            our trigger name
     * @param index
     *            our sample index
     */
    public synchronized void arm(final String trigger, final int index) {
        if (trigger == null) {
            _currentTrigger = null;
            _currentIndex = index;
            _armed = false;
        } else if (!trigger.equals(_currentTrigger) || index != _currentIndex) {
            _currentTrigger = trigger;
            _currentIndex = index;
            _armed = true;
        }
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    public Class<? extends TriggerData> getClassTag() {
        return this.getClass();
    }

    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_currentTrigger, "currentTrigger", null);
        capsule.write(_currentIndex, "currentIndex", 0);
        capsule.write(_armed, "armed", false);
    }

    public void read(final InputCapsule capsule) throws IOException {
        _currentTrigger = capsule.readString("currentTrigger", null);
        _currentIndex = capsule.readInt("currentIndex", 0);
        _armed = capsule.readBoolean("armed", false);
    }
}
