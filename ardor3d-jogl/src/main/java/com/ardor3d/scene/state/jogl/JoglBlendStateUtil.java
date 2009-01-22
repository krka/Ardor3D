/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scene.state.jogl;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.jogl.JoglRenderer;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.BlendState.BlendEquation;
import com.ardor3d.renderer.state.BlendState.DestinationFunction;
import com.ardor3d.renderer.state.BlendState.SourceFunction;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.BlendStateRecord;

public class JoglBlendStateUtil {

    public static void apply(final JoglRenderer renderer, final BlendState state) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final BlendStateRecord record = (BlendStateRecord) context.getStateRecord(StateType.Blend);
        final ContextCapabilities caps = context.getCapabilities();
        context.setCurrentState(StateType.Blend, state);

        if (state.isEnabled()) {
            applyBlendEquations(state.isBlendEnabled(), state, record, caps);
            applyBlendColor(state.isBlendEnabled(), state, record, caps);
            applyBlendFunctions(state.isBlendEnabled(), state, record, caps);

            applyTest(state.isTestEnabled(), state, record);
        } else {
            // disable blend
            applyBlendEquations(false, state, record, caps);

            // disable alpha test
            applyTest(false, state, record);
        }

        if (!record.isValid()) {
            record.validate();
        }
    }

    private static void applyBlendEquations(final boolean enabled, final BlendState state,
            final BlendStateRecord record, final ContextCapabilities caps) {
        final GL gl = GLU.getCurrentGL();

        if (record.isValid()) {
            if (enabled) {
                if (!record.blendEnabled) {
                    gl.glEnable(GL.GL_BLEND);
                    record.blendEnabled = true;
                }
                final int blendEqRGB = getGLEquationValue(state.getBlendEquationRGB(), caps);
                if (caps.isSeparateBlendEquationsSupported()) {
                    final int blendEqAlpha = getGLEquationValue(state.getBlendEquationAlpha(), caps);
                    if (record.blendEqRGB != blendEqRGB || record.blendEqAlpha != blendEqAlpha) {
                        gl.glBlendEquationSeparateEXT(blendEqRGB, blendEqAlpha);
                        record.blendEqRGB = blendEqRGB;
                        record.blendEqAlpha = blendEqAlpha;
                    }
                } else if (caps.isBlendEquationSupported()) {
                    if (record.blendEqRGB != blendEqRGB) {
                        gl.glBlendEquation(blendEqRGB);
                        record.blendEqRGB = blendEqRGB;
                    }
                }
            } else if (record.blendEnabled) {
                gl.glDisable(GL.GL_BLEND);
                record.blendEnabled = false;
            }

        } else {
            if (enabled) {
                gl.glEnable(GL.GL_BLEND);
                record.blendEnabled = true;
                final int blendEqRGB = getGLEquationValue(state.getBlendEquationRGB(), caps);
                if (caps.isSeparateBlendEquationsSupported()) {
                    final int blendEqAlpha = getGLEquationValue(state.getBlendEquationAlpha(), caps);
                    gl.glBlendEquationSeparateEXT(blendEqRGB, blendEqAlpha);
                    record.blendEqRGB = blendEqRGB;
                    record.blendEqAlpha = blendEqAlpha;
                } else if (caps.isBlendEquationSupported()) {
                    gl.glBlendEquation(blendEqRGB);
                    record.blendEqRGB = blendEqRGB;
                }
            } else {
                gl.glDisable(GL.GL_BLEND);
                record.blendEnabled = false;
            }
        }
    }

    private static void applyBlendColor(final boolean enabled, final BlendState state, final BlendStateRecord record,
            final ContextCapabilities caps) {
        final GL gl = GLU.getCurrentGL();

        if (enabled) {
            final boolean applyConstant = state.getDestinationFunctionRGB().usesConstantColor()
                    || state.getSourceFunctionRGB().usesConstantColor()
                    || (caps.isConstantBlendColorSupported() && (state.getDestinationFunctionAlpha()
                            .usesConstantColor() || state.getSourceFunctionAlpha().usesConstantColor()));
            if (applyConstant && caps.isConstantBlendColorSupported()) {
                final ColorRGBA constant = state.getConstantColor(ColorRGBA.fetchTempInstance());
                if (!record.isValid() || (caps.isConstantBlendColorSupported() && !record.blendColor.equals(constant))) {
                    gl.glBlendColor(constant.getRed(), constant.getGreen(), constant.getBlue(), constant.getAlpha());
                    record.blendColor.set(constant);
                }
                ColorRGBA.releaseTempInstance(constant);
            }
        }
    }

    private static void applyBlendFunctions(final boolean enabled, final BlendState state,
            final BlendStateRecord record, final ContextCapabilities caps) {
        final GL gl = GLU.getCurrentGL();

        if (record.isValid()) {
            if (enabled) {
                final int glSrcRGB = getGLSrcValue(state.getSourceFunctionRGB(), caps);
                final int glDstRGB = getGLDstValue(state.getDestinationFunctionRGB(), caps);
                if (caps.isSeparateBlendFunctionsSupported()) {
                    final int glSrcAlpha = getGLSrcValue(state.getSourceFunctionAlpha(), caps);
                    final int glDstAlpha = getGLDstValue(state.getDestinationFunctionAlpha(), caps);
                    if (record.srcFactorRGB != glSrcRGB || record.dstFactorRGB != glDstRGB
                            || record.srcFactorAlpha != glSrcAlpha || record.dstFactorAlpha != glDstAlpha) {
                        gl.glBlendFuncSeparateEXT(glSrcRGB, glDstRGB, glSrcAlpha, glDstAlpha);
                        record.srcFactorRGB = glSrcRGB;
                        record.dstFactorRGB = glDstRGB;
                        record.srcFactorAlpha = glSrcAlpha;
                        record.dstFactorAlpha = glDstAlpha;
                    }
                } else if (record.srcFactorRGB != glSrcRGB || record.dstFactorRGB != glDstRGB) {
                    gl.glBlendFunc(glSrcRGB, glDstRGB);
                    record.srcFactorRGB = glSrcRGB;
                    record.dstFactorRGB = glDstRGB;
                }
            }
        } else {
            if (enabled) {
                final int glSrcRGB = getGLSrcValue(state.getSourceFunctionRGB(), caps);
                final int glDstRGB = getGLDstValue(state.getDestinationFunctionRGB(), caps);
                if (caps.isSeparateBlendFunctionsSupported()) {
                    final int glSrcAlpha = getGLSrcValue(state.getSourceFunctionAlpha(), caps);
                    final int glDstAlpha = getGLDstValue(state.getDestinationFunctionAlpha(), caps);
                    gl.glBlendFuncSeparateEXT(glSrcRGB, glDstRGB, glSrcAlpha, glDstAlpha);
                    record.srcFactorRGB = glSrcRGB;
                    record.dstFactorRGB = glDstRGB;
                    record.srcFactorAlpha = glSrcAlpha;
                    record.dstFactorAlpha = glDstAlpha;
                } else {
                    gl.glBlendFunc(glSrcRGB, glDstRGB);
                    record.srcFactorRGB = glSrcRGB;
                    record.dstFactorRGB = glDstRGB;
                }
            }
        }
    }

    private static int getGLSrcValue(final SourceFunction function, final ContextCapabilities caps) {
        switch (function) {
            case Zero:
                return GL.GL_ZERO;
            case DestinationColor:
                return GL.GL_DST_COLOR;
            case OneMinusDestinationColor:
                return GL.GL_ONE_MINUS_DST_COLOR;
            case SourceAlpha:
                return GL.GL_SRC_ALPHA;
            case OneMinusSourceAlpha:
                return GL.GL_ONE_MINUS_SRC_ALPHA;
            case DestinationAlpha:
                return GL.GL_DST_ALPHA;
            case OneMinusDestinationAlpha:
                return GL.GL_ONE_MINUS_DST_ALPHA;
            case SourceAlphaSaturate:
                return GL.GL_SRC_ALPHA_SATURATE;
            case ConstantColor:
                if (caps.isConstantBlendColorSupported()) {
                    return GL.GL_CONSTANT_COLOR;
                }
                // FALLS THROUGH
            case OneMinusConstantColor:
                if (caps.isConstantBlendColorSupported()) {
                    return GL.GL_ONE_MINUS_CONSTANT_COLOR;
                }
                // FALLS THROUGH
            case ConstantAlpha:
                if (caps.isConstantBlendColorSupported()) {
                    return GL.GL_CONSTANT_ALPHA;
                }
                // FALLS THROUGH
            case OneMinusConstantAlpha:
                if (caps.isConstantBlendColorSupported()) {
                    return GL.GL_ONE_MINUS_CONSTANT_ALPHA;
                }
                // FALLS THROUGH
            case One:
                return GL.GL_ONE;
        }
        throw new IllegalArgumentException("Invalid source function type: " + function);
    }

    private static int getGLDstValue(final DestinationFunction function, final ContextCapabilities caps) {
        switch (function) {
            case Zero:
                return GL.GL_ZERO;
            case SourceColor:
                return GL.GL_SRC_COLOR;
            case OneMinusSourceColor:
                return GL.GL_ONE_MINUS_SRC_COLOR;
            case SourceAlpha:
                return GL.GL_SRC_ALPHA;
            case OneMinusSourceAlpha:
                return GL.GL_ONE_MINUS_SRC_ALPHA;
            case DestinationAlpha:
                return GL.GL_DST_ALPHA;
            case OneMinusDestinationAlpha:
                return GL.GL_ONE_MINUS_DST_ALPHA;
            case ConstantColor:
                if (caps.isConstantBlendColorSupported()) {
                    return GL.GL_CONSTANT_COLOR;
                }
                // FALLS THROUGH
            case OneMinusConstantColor:
                if (caps.isConstantBlendColorSupported()) {
                    return GL.GL_ONE_MINUS_CONSTANT_COLOR;
                }
                // FALLS THROUGH
            case ConstantAlpha:
                if (caps.isConstantBlendColorSupported()) {
                    return GL.GL_CONSTANT_ALPHA;
                }
                // FALLS THROUGH
            case OneMinusConstantAlpha:
                if (caps.isConstantBlendColorSupported()) {
                    return GL.GL_ONE_MINUS_CONSTANT_ALPHA;
                }
                // FALLS THROUGH
            case One:
                return GL.GL_ONE;
        }
        throw new IllegalArgumentException("Invalid destination function type: " + function);
    }

    private static int getGLEquationValue(final BlendEquation eq, final ContextCapabilities caps) {
        switch (eq) {
            case Min:
                if (caps.isMinMaxBlendEquationsSupported()) {
                    return GL.GL_MIN;
                }
                // FALLS THROUGH
            case Max:
                if (caps.isMinMaxBlendEquationsSupported()) {
                    return GL.GL_MAX;
                } else {
                    return GL.GL_FUNC_ADD;
                }
            case Subtract:
                if (caps.isSubtractBlendEquationsSupported()) {
                    return GL.GL_FUNC_SUBTRACT;
                }
                // FALLS THROUGH
            case ReverseSubtract:
                if (caps.isSubtractBlendEquationsSupported()) {
                    return GL.GL_FUNC_REVERSE_SUBTRACT;
                }
                // FALLS THROUGH
            case Add:
                return GL.GL_FUNC_ADD;
        }
        throw new IllegalArgumentException("Invalid blend equation: " + eq);
    }

    private static void applyTest(final boolean enabled, final BlendState state, final BlendStateRecord record) {
        final GL gl = GLU.getCurrentGL();

        if (record.isValid()) {
            if (enabled) {
                if (!record.testEnabled) {
                    gl.glEnable(GL.GL_ALPHA_TEST);
                    record.testEnabled = true;
                }
                final int glFunc = getGLFuncValue(state.getTestFunction());
                if (record.alphaFunc != glFunc || record.alphaRef != state.getReference()) {
                    gl.glAlphaFunc(glFunc, state.getReference());
                    record.alphaFunc = glFunc;
                    record.alphaRef = state.getReference();
                }
            } else if (record.testEnabled) {
                gl.glDisable(GL.GL_ALPHA_TEST);
                record.testEnabled = false;
            }

        } else {
            if (enabled) {
                gl.glEnable(GL.GL_ALPHA_TEST);
                record.testEnabled = true;
                final int glFunc = getGLFuncValue(state.getTestFunction());
                gl.glAlphaFunc(glFunc, state.getReference());
                record.alphaFunc = glFunc;
                record.alphaRef = state.getReference();
            } else {
                gl.glDisable(GL.GL_ALPHA_TEST);
                record.testEnabled = false;
            }
        }
    }

    private static int getGLFuncValue(final BlendState.TestFunction function) {
        switch (function) {
            case Never:
                return GL.GL_NEVER;
            case LessThan:
                return GL.GL_LESS;
            case EqualTo:
                return GL.GL_EQUAL;
            case LessThanOrEqualTo:
                return GL.GL_LEQUAL;
            case GreaterThan:
                return GL.GL_GREATER;
            case NotEqualTo:
                return GL.GL_NOTEQUAL;
            case GreaterThanOrEqualTo:
                return GL.GL_GEQUAL;
            case Always:
                return GL.GL_ALWAYS;
        }
        throw new IllegalArgumentException("Invalid test function type: " + function);
    }
}
