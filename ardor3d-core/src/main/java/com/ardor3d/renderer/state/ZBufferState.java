/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.state;

import java.io.IOException;

import com.ardor3d.renderer.state.record.StateRecord;
import com.ardor3d.renderer.state.record.ZBufferStateRecord;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * <code>ZBufferState</code> maintains how the use of the depth buffer is to occur. Depth buffer comparisons are used to
 * evaluate what incoming fragment will be used. This buffer is based on z depth, or distance between the pixel source
 * and the eye.
 */
public class ZBufferState extends RenderState {

    public enum TestFunction {
        /**
         * Depth comparison never passes.
         */
        Never,
        /**
         * Depth comparison always passes.
         */
        Always,
        /**
         * Passes if the incoming value is the same as the stored value.
         */
        EqualTo,
        /**
         * Passes if the incoming value is not equal to the stored value.
         */
        NotEqualTo,
        /**
         * Passes if the incoming value is less than the stored value.
         */
        LessThan,
        /**
         * Passes if the incoming value is less than or equal to the stored value.
         */
        LessThanOrEqualTo,
        /**
         * Passes if the incoming value is greater than the stored value.
         */
        GreaterThan,
        /**
         * Passes if the incoming value is greater than or equal to the stored value.
         */
        GreaterThanOrEqualTo;

    }

    /** Depth function. */
    protected TestFunction function = TestFunction.LessThan;
    /** Depth mask is writable or not. */
    protected boolean writable = true;

    /**
     * Constructor instantiates a new <code>ZBufferState</code> object. The initial values are TestFunction.LessThan and
     * depth writing on.
     */
    public ZBufferState() {}

    /**
     * <code>getFunction</code> returns the current depth function.
     * 
     * @return the depth function currently used.
     */
    public TestFunction getFunction() {
        return function;
    }

    /**
     * <code>setFunction</code> sets the depth function.
     * 
     * @param function
     *            the depth function.
     * @throws IllegalArgumentException
     *             if function is null
     */
    public void setFunction(final TestFunction function) {
        if (function == null) {
            throw new IllegalArgumentException("function can not be null.");
        }
        this.function = function;
        setNeedsRefresh(true);
    }

    /**
     * <code>isWritable</code> returns if the depth mask is writable or not.
     * 
     * @return true if the depth mask is writable, false otherwise.
     */
    public boolean isWritable() {
        return writable;
    }

    /**
     * <code>setWritable</code> sets the depth mask writable or not.
     * 
     * @param writable
     *            true to turn on depth writing, false otherwise.
     */
    public void setWritable(final boolean writable) {
        this.writable = writable;
        setNeedsRefresh(true);
    }

    @Override
    public StateType getType() {
        return StateType.ZBuffer;
    }

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(function, "function", TestFunction.LessThan);
        capsule.write(writable, "writable", true);
    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);
        function = capsule.readEnum("function", TestFunction.class, TestFunction.LessThan);
        writable = capsule.readBoolean("writable", true);
    }

    @Override
    public StateRecord createStateRecord() {
        return new ZBufferStateRecord();
    }
}
