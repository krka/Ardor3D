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

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.renderer.state.record.BlendStateRecord;
import com.ardor3d.renderer.state.record.StateRecord;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * <code>BlendState</code> maintains the state of the blending values of a particular node and its children. The blend
 * state provides a method for blending a source pixel with a destination pixel. The blend value provides a transparent
 * or translucent surfaces. For example, this would allow for the rendering of green glass. Where you could see all
 * objects behind this green glass but they would be tinted green.
 */
public class BlendState extends RenderState {

    public enum SourceFunction {
        /**
         * The source value of the blend function is all zeros.
         */
        Zero(false),
        /**
         * The source value of the blend function is all ones.
         */
        One(false),
        /**
         * The source value of the blend function is the destination color.
         */
        DestinationColor(false),
        /**
         * The source value of the blend function is 1 - the destination color.
         */
        OneMinusDestinationColor(false),
        /**
         * The source value of the blend function is the source alpha value.
         */
        SourceAlpha(false),
        /**
         * The source value of the blend function is 1 - the source alpha value.
         */
        OneMinusSourceAlpha(false),
        /**
         * The source value of the blend function is the destination alpha.
         */
        DestinationAlpha(false),
        /**
         * The source value of the blend function is 1 - the destination alpha.
         */
        OneMinusDestinationAlpha(false),
        /**
         * The source value of the blend function is the minimum of alpha or 1 - alpha.
         */
        SourceAlphaSaturate(false),
        /**
         * The source value of the blend function is the value of the constant color. (Rc, Gc, Bc, Ac) If not set, black
         * with alpha = 0 is used. If not supported, falls back to One.
         */
        ConstantColor(true),
        /**
         * The source value of the blend function is 1 minus the value of the constant color. (1-Rc, 1-Gc, 1-Bc, 1-Ac)
         * If color is not set, black with alpha = 0 is used. If not supported, falls back to One.
         */
        OneMinusConstantColor(true),
        /**
         * The source value of the blend function is the value of the constant color's alpha. (Ac, Ac, Ac, Ac) If not
         * set, black with alpha = 0 is used. If not supported, falls back to One.
         */
        ConstantAlpha(true),
        /**
         * The source value of the blend function is 1 minus the value of the constant color's alpha. (1-Ac, 1-Ac, 1-Ac,
         * 1-Ac) If color is not set, black with alpha = 0 is used. If not supported, falls back to One.
         */
        OneMinusConstantAlpha(true);

        private boolean usesConstantColor;

        private SourceFunction(final boolean usesConstantColor) {
            this.usesConstantColor = usesConstantColor;
        }

        public boolean usesConstantColor() {
            return usesConstantColor;
        }
    }

    public enum DestinationFunction {
        /**
         * The destination value of the blend function is all zeros.
         */
        Zero(false),
        /**
         * The destination value of the blend function is all ones.
         */
        One(false),
        /**
         * The destination value of the blend function is the source color.
         */
        SourceColor(false),
        /**
         * The destination value of the blend function is 1 - the source color.
         */
        OneMinusSourceColor(false),
        /**
         * The destination value of the blend function is the source alpha value.
         */
        SourceAlpha(false),
        /**
         * The destination value of the blend function is 1 - the source alpha value.
         */
        OneMinusSourceAlpha(false),
        /**
         * The destination value of the blend function is the destination alpha value.
         */
        DestinationAlpha(false),
        /**
         * The destination value of the blend function is 1 - the destination alpha value.
         */
        OneMinusDestinationAlpha(false),
        /**
         * The destination value of the blend function is the value of the constant color. (Rc, Gc, Bc, Ac) If not set,
         * black with alpha = 0 is used. If not supported, falls back to One.
         */
        ConstantColor(true),
        /**
         * The destination value of the blend function is 1 minus the value of the constant color. (1-Rc, 1-Gc, 1-Bc,
         * 1-Ac) If color is not set, black with alpha = 0 is used. If not supported, falls back to One.
         */
        OneMinusConstantColor(true),
        /**
         * The destination value of the blend function is the value of the constant color's alpha. (Ac, Ac, Ac, Ac) If
         * not set, black with alpha = 0 is used. If not supported, falls back to One.
         */
        ConstantAlpha(true),
        /**
         * The destination value of the blend function is 1 minus the value of the constant color's alpha. (1-Ac, 1-Ac,
         * 1-Ac, 1-Ac) If color is not set, black with alpha = 0 is used. If not supported, falls back to One.
         */
        OneMinusConstantAlpha(true);

        private boolean usesConstantColor;

        private DestinationFunction(final boolean usesConstantColor) {
            this.usesConstantColor = usesConstantColor;
        }

        public boolean usesConstantColor() {
            return usesConstantColor;
        }
    }

    public enum TestFunction {
        /**
         * Never passes the depth test.
         */
        Never,
        /**
         * Always passes the depth test.
         */
        Always,
        /**
         * Pass the test if this alpha is equal to the reference alpha.
         */
        EqualTo,
        /**
         * Pass the test if this alpha is not equal to the reference alpha.
         */
        NotEqualTo,
        /**
         * Pass the test if this alpha is less than the reference alpha.
         */
        LessThan,
        /**
         * Pass the test if this alpha is less than or equal to the reference alpha.
         */
        LessThanOrEqualTo,
        /**
         * Pass the test if this alpha is less than the reference alpha.
         */
        GreaterThan,
        /**
         * Pass the test if this alpha is less than or equal to the reference alpha.
         */
        GreaterThanOrEqualTo;

    }

    public enum BlendEquation {
        /**
         * Sets the blend equation so that the source and destination data are added. (Default) Clamps to [0,1] Useful
         * for things like antialiasing and transparency.
         */
        Add,
        /**
         * Sets the blend equation so that the source and destination data are subtracted (Src - Dest). Clamps to [0,1]
         * Falls back to Add if supportsSubtract is false.
         */
        Subtract,
        /**
         * Same as Subtract, but the order is reversed (Dst - Src). Clamps to [0,1] Falls back to Add if
         * supportsSubtract is false.
         */
        ReverseSubtract,
        /**
         * sets the blend equation so that each component of the result color is the minimum of the corresponding
         * components of the source and destination colors. This and Max are useful for applications that analyze image
         * data (image thresholding against a constant color, for example). Falls back to Add if supportsMinMax is
         * false.
         */
        Min,
        /**
         * sets the blend equation so that each component of the result color is the maximum of the corresponding
         * components of the source and destination colors. This and Min are useful for applications that analyze image
         * data (image thresholding against a constant color, for example). Falls back to Add if supportsMinMax is
         * false.
         */
        Max;
    }

    // attributes
    /** The current value of if blend is enabled. */
    private boolean blendEnabled = false;

    /** The blend color used in constant blend operations. */
    private ColorRGBA constantColor = new ColorRGBA(0, 0, 0, 0);

    /** The current source blend function. */
    private SourceFunction sourceFunctionRGB = SourceFunction.SourceAlpha;
    /** The current destination blend function. */
    private DestinationFunction destinationFunctionRGB = DestinationFunction.OneMinusSourceAlpha;
    /** The current blend equation. */
    private BlendEquation blendEquationRGB = BlendEquation.Add;

    /** The current source blend function. */
    private SourceFunction sourceFunctionAlpha = SourceFunction.SourceAlpha;
    /** The current destination blend function. */
    private DestinationFunction destinationFunctionAlpha = DestinationFunction.OneMinusSourceAlpha;
    /** The current blend equation. */
    private BlendEquation blendEquationAlpha = BlendEquation.Add;

    /** If enabled, alpha testing done. */
    private boolean testEnabled = false;
    /** Alpha test value. */
    private TestFunction testFunction = TestFunction.GreaterThan;
    /** The reference value to which incoming alpha values are compared. */
    private float reference = 0;

    /**
     * Constructor instantiates a new <code>BlendState</code> object with default values.
     */
    public BlendState() {}

    @Override
    public StateType getType() {
        return StateType.Blend;
    }

    /**
     * <code>isBlendEnabled</code> returns true if blending is turned on, otherwise false is returned.
     * 
     * @return true if blending is enabled, false otherwise.
     */
    public boolean isBlendEnabled() {
        return blendEnabled;
    }

    /**
     * <code>setBlendEnabled</code> sets whether or not blending is enabled.
     * 
     * @param value
     *            true to enable the blending, false to disable it.
     */
    public void setBlendEnabled(final boolean value) {
        blendEnabled = value;
        setNeedsRefresh(true);
    }

    /**
     * <code>setSrcFunction</code> sets the source function for the blending equation for both rgb and alpha values.
     * 
     * @param function
     *            the source function for the blending equation.
     * @throws IllegalArgumentException
     *             if function is null
     */
    public void setSourceFunction(final SourceFunction function) {
        setSourceFunctionRGB(function);
        setSourceFunctionAlpha(function);
    }

    /**
     * <code>setSrcFunction</code> sets the source function for the blending equation. If supportsSeparateFunc is false,
     * this value will be used for RGB and Alpha.
     * 
     * @param function
     *            the source function for the blending equation.
     * @throws IllegalArgumentException
     *             if function is null
     */
    public void setSourceFunctionRGB(final SourceFunction function) {
        if (function == null) {
            throw new IllegalArgumentException("function can not be null.");
        }
        sourceFunctionRGB = function;
        setNeedsRefresh(true);
    }

    /**
     * <code>setSourceFunctionAlpha</code> sets the source function for the blending equation used with alpha values.
     * 
     * @param function
     *            the source function for the blending equation for alpha values.
     * @throws IllegalArgumentException
     *             if function is null
     */
    public void setSourceFunctionAlpha(final SourceFunction function) {
        if (function == null) {
            throw new IllegalArgumentException("function can not be null.");
        }
        sourceFunctionAlpha = function;
        setNeedsRefresh(true);
    }

    /**
     * <code>getSourceFunction</code> returns the source function for the blending function.
     * 
     * @return the source function for the blending function.
     */
    public SourceFunction getSourceFunctionRGB() {
        return sourceFunctionRGB;
    }

    /**
     * <code>getSourceFunction</code> returns the source function for the blending function.
     * 
     * @return the source function for the blending function.
     */
    public SourceFunction getSourceFunctionAlpha() {
        return sourceFunctionAlpha;
    }

    /**
     * <code>setDestinationFunction</code> sets the destination function for the blending equation for both Alpha and
     * RGB values.
     * 
     * @param function
     *            the destination function for the blending equation.
     * @throws IllegalArgumentException
     *             if function is null
     */
    public void setDestinationFunction(final DestinationFunction function) {
        setDestinationFunctionRGB(function);
        setDestinationFunctionAlpha(function);
    }

    /**
     * <code>setDestinationFunctionRGB</code> sets the destination function for the blending equation. If
     * supportsSeparateFunc is false, this value will be used for RGB and Alpha.
     * 
     * @param function
     *            the destination function for the blending equation for RGB values.
     * @throws IllegalArgumentException
     *             if function is null
     */
    public void setDestinationFunctionRGB(final DestinationFunction function) {
        if (function == null) {
            throw new IllegalArgumentException("function can not be null.");
        }
        destinationFunctionRGB = function;
        setNeedsRefresh(true);
    }

    /**
     * <code>setDestinationFunctionAlpha</code> sets the destination function for the blending equation.
     * 
     * @param function
     *            the destination function for the blending equation for Alpha values.
     * @throws IllegalArgumentException
     *             if function is null
     */
    public void setDestinationFunctionAlpha(final DestinationFunction function) {
        if (function == null) {
            throw new IllegalArgumentException("function can not be null.");
        }
        destinationFunctionAlpha = function;
        setNeedsRefresh(true);
    }

    /**
     * <code>getDestinationFunction</code> returns the destination function for the blending function.
     * 
     * @return the destination function for the blending function.
     */
    public DestinationFunction getDestinationFunctionRGB() {
        return destinationFunctionRGB;
    }

    /**
     * <code>getDestinationFunction</code> returns the destination function for the blending function.
     * 
     * @return the destination function for the blending function.
     */
    public DestinationFunction getDestinationFunctionAlpha() {
        return destinationFunctionAlpha;
    }

    public void setBlendEquation(final BlendEquation blendEquation) {
        setBlendEquationRGB(blendEquation);
        setBlendEquationAlpha(blendEquation);
    }

    public void setBlendEquationRGB(final BlendEquation blendEquation) {
        if (blendEquation == null) {
            throw new IllegalArgumentException("blendEquation can not be null.");
        }
        blendEquationRGB = blendEquation;
    }

    public void setBlendEquationAlpha(final BlendEquation blendEquation) {
        if (blendEquation == null) {
            throw new IllegalArgumentException("blendEquation can not be null.");
        }
        blendEquationAlpha = blendEquation;
    }

    public BlendEquation getBlendEquationRGB() {
        return blendEquationRGB;
    }

    public BlendEquation getBlendEquationAlpha() {
        return blendEquationAlpha;
    }

    /**
     * <code>isTestEnabled</code> returns true if alpha testing is enabled, false otherwise.
     * 
     * @return true if alpha testing is enabled, false otherwise.
     */
    public boolean isTestEnabled() {
        return testEnabled;
    }

    /**
     * <code>setTestEnabled</code> turns alpha testing on and off. True turns on the testing, while false diables it.
     * 
     * @param value
     *            true to enabled alpha testing, false to disable it.
     */
    public void setTestEnabled(final boolean value) {
        testEnabled = value;
        setNeedsRefresh(true);
    }

    /**
     * <code>setTestFunction</code> sets the testing function used for the alpha testing. If an invalid value is passed,
     * the default TF_ALWAYS is used.
     * 
     * @param function
     *            the testing function used for the alpha testing.
     * @throws IllegalArgumentException
     *             if function is null
     */
    public void setTestFunction(final TestFunction function) {
        if (function == null) {
            throw new IllegalArgumentException("function can not be null.");
        }
        testFunction = function;
        setNeedsRefresh(true);
    }

    /**
     * <code>getTestFunction</code> returns the testing function used for the alpha testing.
     * 
     * @return the testing function used for the alpha testing.
     */
    public TestFunction getTestFunction() {
        return testFunction;
    }

    /**
     * <code>setReference</code> sets the reference value that incoming alpha values are compared to when doing alpha
     * testing. This is clamped to [0, 1].
     * 
     * @param reference
     *            the reference value that alpha values are compared to.
     */
    public void setReference(float reference) {
        if (reference < 0) {
            reference = 0;
        }

        if (reference > 1) {
            reference = 1;
        }
        this.reference = reference;
        setNeedsRefresh(true);
    }

    /**
     * <code>getReference</code> returns the reference value that incoming alpha values are compared to.
     * 
     * @return the reference value that alpha values are compared to.
     */
    public float getReference() {
        return reference;
    }

    /**
     * @return the color used in constant blending functions. (0,0,0,0) is the default.
     */
    public ColorRGBA getConstantColor(final ColorRGBA store) {
        return constantColor;
    }

    public void setConstantColor(final ColorRGBA constantColor) {
        this.constantColor.set(constantColor);
    }

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(blendEnabled, "blendEnabled", false);
        capsule.write(sourceFunctionRGB, "sourceFunctionRGB", SourceFunction.SourceAlpha);
        capsule.write(destinationFunctionRGB, "destinationFunctionRGB", DestinationFunction.OneMinusSourceAlpha);
        capsule.write(blendEquationRGB, "blendEquationRGB", BlendEquation.Add);
        capsule.write(sourceFunctionAlpha, "sourceFunctionAlpha", SourceFunction.SourceAlpha);
        capsule.write(destinationFunctionAlpha, "destinationFunctionAlpha", DestinationFunction.OneMinusSourceAlpha);
        capsule.write(blendEquationAlpha, "blendEquationAlpha", BlendEquation.Add);
        capsule.write(testEnabled, "testEnabled", false);
        capsule.write(testFunction, "test", TestFunction.GreaterThan);
        capsule.write(reference, "reference", 0);
        capsule.write(constantColor, "constantColor", null);
    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);
        blendEnabled = capsule.readBoolean("blendEnabled", false);
        sourceFunctionRGB = capsule.readEnum("sourceFunctionRGB", SourceFunction.class, SourceFunction.SourceAlpha);
        destinationFunctionRGB = capsule.readEnum("destinationFunctionRGB", DestinationFunction.class,
                DestinationFunction.OneMinusSourceAlpha);
        blendEquationRGB = capsule.readEnum("blendEquationRGB", BlendEquation.class, BlendEquation.Add);
        sourceFunctionAlpha = capsule.readEnum("sourceFunctionAlpha", SourceFunction.class, SourceFunction.SourceAlpha);
        destinationFunctionAlpha = capsule.readEnum("destinationFunctionAlpha", DestinationFunction.class,
                DestinationFunction.OneMinusSourceAlpha);
        blendEquationAlpha = capsule.readEnum("blendEquationAlpha", BlendEquation.class, BlendEquation.Add);
        testEnabled = capsule.readBoolean("testEnabled", false);
        testFunction = capsule.readEnum("test", TestFunction.class, TestFunction.GreaterThan);
        reference = capsule.readFloat("reference", 0);
        constantColor = (ColorRGBA) capsule.readSavable("constantColor", null);
    }

    @Override
    public StateRecord createStateRecord() {
        return new BlendStateRecord();
    }

}
