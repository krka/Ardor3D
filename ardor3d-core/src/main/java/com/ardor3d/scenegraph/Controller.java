/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * <code>Controller</code> provides a base class for creation of controllers to modify nodes and render states over
 * time. The base controller provides a repeat type, min and max time, as well as speed. Subclasses of this will provide
 * the update method that takes the time between the last call and the current one and modifies an object in a
 * application specific way.
 */
public abstract class Controller implements Serializable, Savable {

    public enum RepeatType {
        /**
         * A clamped repeat type signals that the controller should look like its final state when it's done <br>
         * Example: 0 1 5 8 9 10 10 10 10 10 10 10 10 10 10 10...
         */
        CLAMP,

        /**
         * A wrapped repeat type signals that the controller should start back at the begining when it's final state is
         * reached <br>
         * Example: 0 1 5 8 9 10 0 1 5 8 9 10 0 1 5 ....
         */
        WRAP,

        /**
         * A cycled repeat type signals that the controller should cycle it's states forwards and backwards <br>
         * Example: 0 1 5 8 9 10 9 8 5 1 0 1 5 8 9 10 9 ....
         */
        CYCLE;
    }

    /**
     * Defines how this controller should repeat itself. This can be one of RT_CLAMP, RT_WRAP, RT_CYCLE, or an
     * application specific repeat flag.
     */
    private RepeatType repeatType;

    /**
     * The controller's minimum cycle time
     */
    private double minTime;

    /**
     * The controller's maximum cycle time
     */
    private double maxTime;

    /**
     * The 'speed' of this Controller. Generically speaking, less than 1 is slower, more than 1 is faster, and 1
     * represents the base speed
     */
    private double speed = 1;

    /**
     * True if this controller is active, false otherwise
     */
    private boolean active = true;

    private static final long serialVersionUID = 1;

    /**
     * Returns the speed of this controller. Speed is 1 by default.
     * 
     * @return
     */
    public double getSpeed() {
        return speed;
    }

    /**
     * Sets the speed of this controller
     * 
     * @param speed
     *            The new speed
     */
    public void setSpeed(final double speed) {
        this.speed = speed;
    }

    /**
     * Returns the current maximum time for this controller.
     * 
     * @return This controller's maximum time.
     */
    public double getMaxTime() {
        return maxTime;
    }

    /**
     * Sets the maximum time for this controller
     * 
     * @param maxTime
     *            The new maximum time
     */
    public void setMaxTime(final double maxTime) {
        this.maxTime = maxTime;
    }

    /**
     * Returns the current minimum time of this controller
     * 
     * @return This controller's minimum time
     */
    public double getMinTime() {
        return minTime;
    }

    /**
     * Sets the minimum time of this controller
     * 
     * @param minTime
     *            The new minimum time.
     */
    public void setMinTime(final double minTime) {
        this.minTime = minTime;
    }

    /**
     * Returns the current repeat type of this controller.
     * 
     * @return The current repeat type
     */
    public RepeatType getRepeatType() {
        return repeatType;
    }

    /**
     * Sets the repeat type of this controller.
     * 
     * @param repeatType
     *            The new repeat type.
     */
    public void setRepeatType(final RepeatType repeatType) {
        this.repeatType = repeatType;
    }

    /**
     * Sets the active flag of this controller. Note: updates on controllers are still called even if this flag is set
     * to false. It is the responsibility of the extending class to check isActive if it wishes to be turn-off-able.
     * 
     * @param active
     *            The new active state.
     */
    public void setActive(final boolean active) {
        this.active = active;
    }

    /**
     * Returns if this Controller is active or not.
     * 
     * @return True if this controller is set to active, false if not.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Defined by extending classes, <code>update</code> is a signal to Controller that it should update whatever
     * object(s) it is controlling.
     * 
     * @param time
     *            The time in seconds between the last call to update and the current one
     * @param caller
     *            The spatial currently executing this controller.
     */
    public abstract void update(double time, Spatial caller);

    public void write(final Ardor3DExporter e) throws IOException {
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(repeatType, "repeatType", RepeatType.CLAMP);
        capsule.write(minTime, "minTime", 0);
        capsule.write(maxTime, "maxTime", 0);
        capsule.write(speed, "speed", 1);
        capsule.write(active, "active", true);
    }

    public void read(final Ardor3DImporter e) throws IOException {
        final InputCapsule capsule = e.getCapsule(this);
        repeatType = capsule.readEnum("repeatType", RepeatType.class, RepeatType.CLAMP);
        minTime = capsule.readDouble("minTime", 0);
        maxTime = capsule.readDouble("maxTime", 0);
        speed = capsule.readDouble("speed", 1);
        active = capsule.readBoolean("active", true);
    }

    public Class<? extends Controller> getClassTag() {
        return this.getClass();
    }

    public void getControllerValues(final HashMap<String, Object> store) {

    }

    public void setControllerValues(final HashMap<String, Object> values) {

    }
}