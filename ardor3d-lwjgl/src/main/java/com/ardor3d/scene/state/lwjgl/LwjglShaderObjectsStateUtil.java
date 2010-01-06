/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scene.state.lwjgl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.logging.Logger;

import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.lwjgl.LwjglRenderer;
import com.ardor3d.renderer.state.GLSLShaderObjectsState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.ShaderObjectsStateRecord;
import com.ardor3d.scene.state.lwjgl.shader.LwjglShaderUtil;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.shader.ShaderVariable;

public abstract class LwjglShaderObjectsStateUtil {
    private static final Logger logger = Logger.getLogger(LwjglShaderObjectsStateUtil.class.getName());

    protected static void sendToGL(final GLSLShaderObjectsState state) {
        if (state.getVertexShader() == null && state.getFragmentShader() == null) {
            logger.warning("Could not find shader resources!" + "(both inputbuffers are null)");
            state._needSendShader = false;
            return;
        }

        if (state._programID == -1) {
            state._programID = ARBShaderObjects.glCreateProgramObjectARB();
        }

        if (state.getVertexShader() != null) {
            if (state._vertexShaderID != -1) {
                removeVertShader(state);
            }

            state._vertexShaderID = ARBShaderObjects.glCreateShaderObjectARB(ARBVertexShader.GL_VERTEX_SHADER_ARB);

            // Create the sources
            ARBShaderObjects.glShaderSourceARB(state._vertexShaderID, state.getVertexShader());

            // Compile the vertex shader
            final IntBuffer compiled = BufferUtils.createIntBuffer(1);
            ARBShaderObjects.glCompileShaderARB(state._vertexShaderID);
            ARBShaderObjects.glGetObjectParameterARB(state._vertexShaderID,
                    ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB, compiled);
            checkProgramError(compiled, state._vertexShaderID);

            // Attach the program
            ARBShaderObjects.glAttachObjectARB(state._programID, state._vertexShaderID);
        } else if (state._vertexShaderID != -1) {
            removeVertShader(state);
            state._vertexShaderID = -1;
        }

        if (state.getFragmentShader() != null) {
            if (state._fragmentShaderID != -1) {
                removeFragShader(state);
            }

            state._fragmentShaderID = ARBShaderObjects
                    .glCreateShaderObjectARB(ARBFragmentShader.GL_FRAGMENT_SHADER_ARB);

            // Create the sources
            ARBShaderObjects.glShaderSourceARB(state._fragmentShaderID, state.getFragmentShader());

            // Compile the fragment shader
            final IntBuffer compiled = BufferUtils.createIntBuffer(1);
            ARBShaderObjects.glCompileShaderARB(state._fragmentShaderID);
            ARBShaderObjects.glGetObjectParameterARB(state._fragmentShaderID,
                    ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB, compiled);
            checkProgramError(compiled, state._fragmentShaderID);

            // Attach the program
            ARBShaderObjects.glAttachObjectARB(state._programID, state._fragmentShaderID);
        } else if (state._fragmentShaderID != -1) {
            removeFragShader(state);
            state._fragmentShaderID = -1;
        }

        ARBShaderObjects.glLinkProgramARB(state._programID);
        checkLinkError(state._programID);
        state.setNeedsRefresh(true);
        state._needSendShader = false;
    }

    private static void checkLinkError(final int programId) {
        final IntBuffer compiled = BufferUtils.createIntBuffer(1);
        ARBShaderObjects.glGetObjectParameterARB(programId, GL20.GL_LINK_STATUS, compiled);
        if (compiled.get(0) == GL11.GL_FALSE) {
            ARBShaderObjects.glGetObjectParameterARB(programId, GL20.GL_INFO_LOG_LENGTH, compiled);
            final int length = compiled.get(0);
            String out = null;
            if (length > 0) {
                final ByteBuffer infoLog = BufferUtils.createByteBuffer(length);

                ARBShaderObjects.glGetInfoLogARB(programId, compiled, infoLog);

                final byte[] infoBytes = new byte[length];
                infoLog.get(infoBytes);
                out = new String(infoBytes);
            }

            logger.severe(out);

            throw new Ardor3dException("Error linking GLSL shader: " + out);
        }
    }

    /** Removes the fragment shader */
    private static void removeFragShader(final GLSLShaderObjectsState state) {
        if (state._fragmentShaderID != -1) {
            ARBShaderObjects.glDetachObjectARB(state._programID, state._fragmentShaderID);
            ARBShaderObjects.glDeleteObjectARB(state._fragmentShaderID);
        }
    }

    /**
     * Removes the vertex shader
     */
    private static void removeVertShader(final GLSLShaderObjectsState state) {
        if (state._vertexShaderID != -1) {
            ARBShaderObjects.glDetachObjectARB(state._programID, state._vertexShaderID);
            ARBShaderObjects.glDeleteObjectARB(state._vertexShaderID);
        }
    }

    /**
     * Check for program errors. If an error is detected, program exits.
     * 
     * @param compiled
     *            the compiler state for a given shader
     * @param id
     *            shader's id
     */
    private static void checkProgramError(final IntBuffer compiled, final int id) {
        if (compiled.get(0) == GL11.GL_FALSE) {
            final IntBuffer iVal = BufferUtils.createIntBuffer(1);
            ARBShaderObjects.glGetObjectParameterARB(id, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB, iVal);
            final int length = iVal.get(0);
            String out = null;

            if (length > 0) {
                final ByteBuffer infoLog = BufferUtils.createByteBuffer(length);

                ARBShaderObjects.glGetInfoLogARB(id, iVal, infoLog);

                final byte[] infoBytes = new byte[length];
                infoLog.get(infoBytes);
                out = new String(infoBytes);
            }

            logger.severe(out);

            throw new Ardor3dException("Error compiling GLSL shader: " + out);
        }
    }

    public static void apply(final LwjglRenderer renderer, final GLSLShaderObjectsState state) {
        final RenderContext context = ContextManager.getCurrentContext();
        if (context.getCapabilities().isGLSLSupported()) {
            // Ask for the current state record
            final ShaderObjectsStateRecord record = (ShaderObjectsStateRecord) context
                    .getStateRecord(StateType.GLSLShader);
            context.setCurrentState(StateType.GLSLShader, state);

            if (state.isEnabled()) {
                if (state._needSendShader) {
                    sendToGL(state);
                }

                if (state._shaderDataLogic != null) {
                    state._shaderDataLogic.applyData(state, state._mesh, renderer);
                }
            }

            if (!record.isValid() || record.getReference() != state || state.needsRefresh()) {
                record.setReference(state);
                if (state.isEnabled()) {
                    if (state._programID != -1) {
                        ARBShaderObjects.glUseProgramObjectARB(state._programID);

                        for (int i = state.getShaderAttributes().size(); --i >= 0;) {
                            final ShaderVariable shaderVariable = state.getShaderAttributes().get(i);
                            if (shaderVariable.needsRefresh) {
                                LwjglShaderUtil.updateAttributeLocation(shaderVariable, state._programID);
                                shaderVariable.needsRefresh = false;
                            }
                            LwjglShaderUtil.updateShaderAttribute(shaderVariable);
                        }

                        for (int i = state.getShaderUniforms().size(); --i >= 0;) {
                            final ShaderVariable shaderVariable = state.getShaderUniforms().get(i);
                            if (shaderVariable.needsRefresh) {
                                LwjglShaderUtil.updateUniformLocation(shaderVariable, state._programID);
                                LwjglShaderUtil.updateShaderUniform(shaderVariable);
                                shaderVariable.needsRefresh = false;
                            }
                        }
                    }
                } else {
                    ARBShaderObjects.glUseProgramObjectARB(0);
                }
            }

            if (!record.isValid()) {
                record.validate();
            }
        }
    }
}
