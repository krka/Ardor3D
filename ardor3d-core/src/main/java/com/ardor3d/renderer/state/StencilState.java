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
import com.ardor3d.renderer.state.record.StencilStateRecord;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * The StencilState RenderState allows the user to set the attributes of the stencil buffer of the renderer. The
 * Stenciling is similar to Z-Buffering in that it allows enabling and disabling drawing on a per pixel basis. You can
 * use the stencil plane to mask out portions of the rendering to create special effects, such as outlining or planar
 * shadows. Our stencil state supports setting operations for front and back facing polygons separately. If your card
 * does not support setting faces independently, the front face values will be used for both sides.
 */
public class StencilState extends RenderState {

    public enum StencilFunction {
        /** A stencil function that never passes. */
        Never,
        /** A stencil function that passes if (ref & mask) < (stencil & mask). */
        LessThan,
        /** A stencil function that passes if (ref & max) <= (stencil & mask). */
        LessThanOrEqualTo,
        /** A stencil function that passes if (ref & max) > (stencil & mask). */
        GreaterThan,
        /** A stencil function that passes if (ref & max) >= (stencil & mask). */
        GreaterThanOrEqualTo,
        /** A stencil function that passes if (ref & max) == (stencil & mask). */
        EqualTo,
        /** A stencil function that passes if (ref & max) != (stencil & mask). */
        NotEqualTo,
        /** A stencil function that always passes. (Default) */
        Always;
    }

    public enum StencilOperation {
        /** A stencil function result that keeps the current value. */
        Keep,
        /** A stencil function result that sets the stencil buffer value to 0. */
        Zero,
        /**
         * A stencil function result that sets the stencil buffer value to ref, as specified by stencil function.
         */
        Replace,
        /**
         * A stencil function result that increments the current stencil buffer value.
         */
        Increment,
        /**
         * A stencil function result that decrements the current stencil buffer value.
         */
        Decrement,
        /**
         * A stencil function result that increments the current stencil buffer value and wraps around to the lowest
         * stencil value if it reaches the max. (if the renderer does not support stencil wrap, we'll fall back to
         * Increment)
         */
        IncrementWrap,
        /**
         * A stencil function result that decrements the current stencil buffer and wraps around to the highest stencil
         * value if it reaches the min. value. (if the renderer does not support stencil wrap, we'll fall back to
         * Decrement)
         */
        DecrementWrap,
        /**
         * A stencil function result that bitwise inverts the current stencil buffer value.
         */
        Invert;
    }

    private boolean useTwoSided = false;

    private StencilFunction stencilFunctionFront = StencilFunction.Always;
    private int stencilReferenceFront = 0;
    private int stencilFuncMaskFront = ~0;
    private int stencilWriteMaskFront = ~0;
    private StencilOperation stencilOpFailFront = StencilOperation.Keep;
    private StencilOperation stencilOpZFailFront = StencilOperation.Keep;
    private StencilOperation stencilOpZPassFront = StencilOperation.Keep;

    private StencilFunction stencilFunctionBack = StencilFunction.Always;
    private int stencilReferenceBack = 0;
    private int stencilFuncMaskBack = ~0;
    private int stencilWriteMaskBack = ~0;
    private StencilOperation stencilOpFailBack = StencilOperation.Keep;
    private StencilOperation stencilOpZFailBack = StencilOperation.Keep;
    private StencilOperation stencilOpZPassBack = StencilOperation.Keep;

    @Override
    public StateType getType() {
        return StateType.Stencil;
    }

    /**
     * Sets the function that defines if a stencil test passes or not for both faces.
     * 
     * @param function
     *            The new stencil function for both faces.
     * @throws IllegalArgumentException
     *             if function is null
     */
    public void setStencilFunction(final StencilFunction function) {
        setStencilFunctionFront(function);
        setStencilFunctionBack(function);
    }

    /**
     * Sets the stencil reference to be used during the stencil function for both faces.
     * 
     * @param reference
     *            The new stencil reference for both faces.
     */
    public void setStencilReference(final int reference) {
        setStencilReferenceFront(reference);
        setStencilReferenceBack(reference);
    }

    /**
     * Convienence method for setting both types of stencil masks at once for both faces.
     * 
     * @param mask
     *            The new stencil write and func mask for both faces.
     */
    public void setStencilMask(final int mask) {
        setStencilMaskFront(mask);
        setStencilMaskBack(mask);
    }

    /**
     * Controls which stencil bitplanes are written for both faces.
     * 
     * @param mask
     *            The new stencil write mask for both faces.
     */
    public void setStencilWriteMask(final int mask) {
        setStencilWriteMaskFront(mask);
        setStencilWriteMaskBack(mask);
    }

    /**
     * Sets the stencil mask to be used during stencil functions for both faces.
     * 
     * @param mask
     *            The new stencil function mask for both faces.
     */
    public void setStencilFuncMask(final int mask) {
        setStencilFuncMaskFront(mask);
        setStencilFuncMaskBack(mask);
    }

    /**
     * Specifies the aciton to take when the stencil test fails for both faces.
     * 
     * @param operation
     *            The new stencil operation for both faces.
     * @throws IllegalArgumentException
     *             if operation is null
     */
    public void setStencilOpFail(final StencilOperation operation) {
        setStencilOpFailFront(operation);
        setStencilOpFailBack(operation);
    }

    /**
     * Specifies stencil action when the stencil test passes, but the depth test fails for both faces.
     * 
     * @param operation
     *            The Z test operation to set for both faces.
     * @throws IllegalArgumentException
     *             if operation is null
     */
    public void setStencilOpZFail(final StencilOperation operation) {
        setStencilOpZFailFront(operation);
        setStencilOpZFailBack(operation);
    }

    /**
     * Specifies stencil action when both the stencil test and the depth test pass, or when the stencil test passes and
     * either there is no depth buffer or depth testing is not enabled.
     * 
     * @param operation
     *            The new Z test pass operation to set for both faces.
     * @throws IllegalArgumentException
     *             if operation is null
     */
    public void setStencilOpZPass(final StencilOperation operation) {
        setStencilOpZPassFront(operation);
        setStencilOpZPassBack(operation);
    }

    /**
     * Sets the function that defines if a stencil test passes or not for front faces.
     * 
     * @param function
     *            The new stencil function for front faces.
     * @throws IllegalArgumentException
     *             if function is null
     */
    public void setStencilFunctionFront(final StencilFunction function) {
        if (function == null) {
            throw new IllegalArgumentException("function can not be null.");
        }
        stencilFunctionFront = function;
        setNeedsRefresh(true);
    }

    /**
     * @return The current stencil function for front faces. Default is StencilFunction.Always
     */
    public StencilFunction getStencilFunctionFront() {
        return stencilFunctionFront;
    }

    /**
     * Sets the stencil reference to be used during the stencil function for front faces.
     * 
     * @param reference
     *            The new stencil reference for front faces.
     */
    public void setStencilReferenceFront(final int reference) {
        stencilReferenceFront = reference;
        setNeedsRefresh(true);
    }

    /**
     * @return The current stencil reference for front faces. Default is 0
     */
    public int getStencilReferenceFront() {
        return stencilReferenceFront;
    }

    /**
     * Convienence method for setting both types of stencil masks at once for front faces.
     * 
     * @param mask
     *            The new stencil write and func mask for front faces.
     */
    public void setStencilMaskFront(final int mask) {
        setStencilWriteMaskFront(mask);
        setStencilFuncMaskFront(mask);
    }

    /**
     * Controls which stencil bitplanes are written for front faces.
     * 
     * @param mask
     *            The new stencil write mask for front faces.
     */
    public void setStencilWriteMaskFront(final int mask) {
        stencilWriteMaskFront = mask;
        setNeedsRefresh(true);
    }

    /**
     * @return The current stencil write mask for front faces. Default is all 1's (~0)
     */
    public int getStencilWriteMaskFront() {
        return stencilWriteMaskFront;
    }

    /**
     * Sets the stencil mask to be used during stencil functions for front faces.
     * 
     * @param mask
     *            The new stencil function mask for front faces.
     */
    public void setStencilFuncMaskFront(final int mask) {
        stencilFuncMaskFront = mask;
        setNeedsRefresh(true);
    }

    /**
     * @return The current stencil function mask for front faces. Default is all 1's (~0)
     */
    public int getStencilFuncMaskFront() {
        return stencilFuncMaskFront;
    }

    /**
     * Specifies the aciton to take when the stencil test fails for front faces.
     * 
     * @param operation
     *            The new stencil operation for front faces.
     * @throws IllegalArgumentException
     *             if operation is null
     */
    public void setStencilOpFailFront(final StencilOperation operation) {
        if (operation == null) {
            throw new IllegalArgumentException("operation can not be null.");
        }
        stencilOpFailFront = operation;
        setNeedsRefresh(true);
    }

    /**
     * @return The current stencil operation for front faces. Default is StencilOperation.Keep
     */
    public StencilOperation getStencilOpFailFront() {
        return stencilOpFailFront;
    }

    /**
     * Specifies stencil action when the stencil test passes, but the depth test fails for front faces.
     * 
     * @param operation
     *            The Z test operation to set for front faces.
     * @throws IllegalArgumentException
     *             if operation is null
     */
    public void setStencilOpZFailFront(final StencilOperation operation) {
        if (operation == null) {
            throw new IllegalArgumentException("operation can not be null.");
        }
        stencilOpZFailFront = operation;
        setNeedsRefresh(true);
    }

    /**
     * @return The current Z op fail function for front faces. Default is StencilOperation.Keep
     */
    public StencilOperation getStencilOpZFailFront() {
        return stencilOpZFailFront;
    }

    /**
     * Specifies stencil action when both the stencil test and the depth test pass, or when the stencil test passes and
     * either there is no depth buffer or depth testing is not enabled.
     * 
     * @param operation
     *            The new Z test pass operation to set for front faces.
     * @throws IllegalArgumentException
     *             if operation is null
     */
    public void setStencilOpZPassFront(final StencilOperation operation) {
        if (operation == null) {
            throw new IllegalArgumentException("operation can not be null.");
        }
        stencilOpZPassFront = operation;
        setNeedsRefresh(true);
    }

    /**
     * @return The current Z op pass function for front faces. Default is StencilOperation.Keep
     */
    public StencilOperation getStencilOpZPassFront() {
        return stencilOpZPassFront;
    }

    /**
     * Sets the function that defines if a stencil test passes or not for back faces.
     * 
     * @param function
     *            The new stencil function for back faces.
     * @throws IllegalArgumentException
     *             if function is null
     */
    public void setStencilFunctionBack(final StencilFunction function) {
        if (function == null) {
            throw new IllegalArgumentException("function can not be null.");
        }
        stencilFunctionBack = function;
        setNeedsRefresh(true);
    }

    /**
     * @return The current stencil function for back faces. Default is StencilFunction.Always
     */
    public StencilFunction getStencilFunctionBack() {
        return stencilFunctionBack;
    }

    /**
     * Sets the stencil reference to be used during the stencil function for back faces.
     * 
     * @param reference
     *            The new stencil reference for back faces.
     */
    public void setStencilReferenceBack(final int reference) {
        stencilReferenceBack = reference;
        setNeedsRefresh(true);
    }

    /**
     * @return The current stencil reference for back faces. Default is 0
     */
    public int getStencilReferenceBack() {
        return stencilReferenceBack;
    }

    /**
     * Convienence method for setting both types of stencil masks at once for back faces.
     * 
     * @param mask
     *            The new stencil write and func mask for back faces.
     */
    public void setStencilMaskBack(final int mask) {
        setStencilWriteMaskBack(mask);
        setStencilFuncMaskBack(mask);
    }

    /**
     * Controls which stencil bitplanes are written for back faces.
     * 
     * @param mask
     *            The new stencil write mask for back faces.
     */
    public void setStencilWriteMaskBack(final int mask) {
        stencilWriteMaskBack = mask;
        setNeedsRefresh(true);
    }

    /**
     * @return The current stencil write mask for back faces. Default is all 1's (~0)
     */
    public int getStencilWriteMaskBack() {
        return stencilWriteMaskBack;
    }

    /**
     * Sets the stencil mask to be used during stencil functions for back faces.
     * 
     * @param mask
     *            The new stencil function mask for back faces.
     */
    public void setStencilFuncMaskBack(final int mask) {
        stencilFuncMaskBack = mask;
        setNeedsRefresh(true);
    }

    /**
     * @return The current stencil function mask for back faces. Default is all 1's (~0)
     */
    public int getStencilFuncMaskBack() {
        return stencilFuncMaskBack;
    }

    /**
     * Specifies the aciton to take when the stencil test fails for back faces.
     * 
     * @param operation
     *            The new stencil operation for back faces.
     * @throws IllegalArgumentException
     *             if operation is null
     */
    public void setStencilOpFailBack(final StencilOperation operation) {
        if (operation == null) {
            throw new IllegalArgumentException("operation can not be null.");
        }
        stencilOpFailBack = operation;
        setNeedsRefresh(true);
    }

    /**
     * @return The current stencil operation for back faces. Default is StencilOperation.Keep
     */
    public StencilOperation getStencilOpFailBack() {
        return stencilOpFailBack;
    }

    /**
     * Specifies stencil action when the stencil test passes, but the depth test fails.
     * 
     * @param operation
     *            The Z test operation to set for back faces.
     * @throws IllegalArgumentException
     *             if operation is null
     */
    public void setStencilOpZFailBack(final StencilOperation operation) {
        if (operation == null) {
            throw new IllegalArgumentException("operation can not be null.");
        }
        stencilOpZFailBack = operation;
        setNeedsRefresh(true);
    }

    /**
     * @return The current Z op fail function for back faces. Default is StencilOperation.Keep
     */
    public StencilOperation getStencilOpZFailBack() {
        return stencilOpZFailBack;
    }

    /**
     * Specifies stencil action when both the stencil test and the depth test pass, or when the stencil test passes and
     * either there is no depth buffer or depth testing is not enabled.
     * 
     * @param operation
     *            The new Z test pass operation to set for back faces.
     * @throws IllegalArgumentException
     *             if operation is null
     */
    public void setStencilOpZPassBack(final StencilOperation operation) {
        if (operation == null) {
            throw new IllegalArgumentException("operation can not be null.");
        }
        stencilOpZPassBack = operation;
        setNeedsRefresh(true);
    }

    /**
     * @return The current Z op pass function for back faces. Default is StencilOperation.Keep
     */
    public StencilOperation getStencilOpZPassBack() {
        return stencilOpZPassBack;
    }

    public boolean isUseTwoSided() {
        return useTwoSided;
    }

    public void setUseTwoSided(final boolean useTwoSided) {
        this.useTwoSided = useTwoSided;
    }

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(useTwoSided, "useTwoSided", false);
        capsule.write(stencilFunctionFront, "stencilFuncFront", StencilFunction.Always);
        capsule.write(stencilReferenceFront, "stencilRefFront", 0);
        capsule.write(stencilWriteMaskFront, "stencilWriteMaskFront", ~0);
        capsule.write(stencilFuncMaskFront, "stencilFuncMaskFront", ~0);
        capsule.write(stencilOpFailFront, "stencilOpFailFront", StencilOperation.Keep);
        capsule.write(stencilOpZFailFront, "stencilOpZFailFront", StencilOperation.Keep);
        capsule.write(stencilOpZPassFront, "stencilOpZPassFront", StencilOperation.Keep);

        capsule.write(stencilFunctionBack, "stencilFuncBack", StencilFunction.Always);
        capsule.write(stencilReferenceBack, "stencilRefBack", 0);
        capsule.write(stencilWriteMaskBack, "stencilWriteMaskBack", ~0);
        capsule.write(stencilFuncMaskBack, "stencilFuncMaskBack", ~0);
        capsule.write(stencilOpFailBack, "stencilOpFailBack", StencilOperation.Keep);
        capsule.write(stencilOpZFailBack, "stencilOpZFailBack", StencilOperation.Keep);
        capsule.write(stencilOpZPassBack, "stencilOpZPassBack", StencilOperation.Keep);
    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);
        useTwoSided = capsule.readBoolean("useTwoSided", false);
        stencilFunctionFront = capsule.readEnum("stencilFuncFront", StencilFunction.class, StencilFunction.Always);
        stencilReferenceFront = capsule.readInt("stencilRefFront", 0);
        stencilWriteMaskFront = capsule.readInt("stencilWriteMaskFront", ~0);
        stencilFuncMaskFront = capsule.readInt("stencilFuncMaskFront", ~0);
        stencilOpFailFront = capsule.readEnum("stencilOpFailFront", StencilOperation.class, StencilOperation.Keep);
        stencilOpZFailFront = capsule.readEnum("stencilOpZFailFront", StencilOperation.class, StencilOperation.Keep);
        stencilOpZPassFront = capsule.readEnum("stencilOpZPassFront", StencilOperation.class, StencilOperation.Keep);

        stencilFunctionBack = capsule.readEnum("stencilFuncBack", StencilFunction.class, StencilFunction.Always);
        stencilReferenceBack = capsule.readInt("stencilRefBack", 0);
        stencilWriteMaskBack = capsule.readInt("stencilWriteMaskBack", ~0);
        stencilFuncMaskBack = capsule.readInt("stencilFuncMaskBack", ~0);
        stencilOpFailBack = capsule.readEnum("stencilOpFailBack", StencilOperation.class, StencilOperation.Keep);
        stencilOpZFailBack = capsule.readEnum("stencilOpZFailBack", StencilOperation.class, StencilOperation.Keep);
        stencilOpZPassBack = capsule.readEnum("stencilOpZPassBack", StencilOperation.class, StencilOperation.Keep);
    }

    @Override
    public StateRecord createStateRecord() {
        return new StencilStateRecord();
    }

}
