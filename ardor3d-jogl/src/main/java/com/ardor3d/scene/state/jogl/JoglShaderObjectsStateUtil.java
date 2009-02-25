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

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.logging.Logger;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.jogl.JoglRenderer;
import com.ardor3d.renderer.state.GLSLShaderObjectsState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.ShaderObjectsStateRecord;
import com.ardor3d.scene.state.jogl.shader.JoglShaderUtil;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.shader.ShaderVariable;

public abstract class JoglShaderObjectsStateUtil {
    private static final Logger logger = Logger.getLogger(JoglShaderObjectsStateUtil.class.getName());

    protected static void sendToGL(final GLSLShaderObjectsState state) {
        final GL gl = GLU.getCurrentGL();

        if (state.getVertexShader() == null && state.getFragmentShader() == null) {
            logger.warning("Could not find shader resources!" + "(both inputbuffers are null)");
            state._needSendShader = false;
            return;
        }

        if (state._programID == -1) {
            state._programID = gl.glCreateProgramObjectARB();
        }

        if (state.getVertexShader() != null) {
            if (state._vertexShaderID != -1) {
                removeVertShader(state);
            }

            state._vertexShaderID = gl.glCreateShaderObjectARB(GL.GL_VERTEX_SHADER_ARB);

            // Create the sources
            final byte array[] = new byte[state.getVertexShader().limit()];
            state.getVertexShader().rewind();
            state.getVertexShader().get(array);
            gl.glShaderSourceARB(state._vertexShaderID, 1, new String[] { new String(array) },
                    new int[] { array.length }, 0);

            // Compile the vertex shader
            final IntBuffer compiled = BufferUtils.createIntBuffer(1);
            gl.glCompileShaderARB(state._vertexShaderID);
            gl.glGetObjectParameterivARB(state._vertexShaderID, GL.GL_OBJECT_COMPILE_STATUS_ARB, compiled);
            checkProgramError(compiled, state._vertexShaderID);

            // Attach the program
            gl.glAttachObjectARB(state._programID, state._vertexShaderID);
        } else if (state._vertexShaderID != -1) {
            removeVertShader(state);
            state._vertexShaderID = -1;
        }

        if (state.getFragmentShader() != null) {
            if (state._fragmentShaderID != -1) {
                removeFragShader(state);
            }

            state._fragmentShaderID = gl.glCreateShaderObjectARB(GL.GL_FRAGMENT_SHADER_ARB);

            // Create the sources
            final byte array[] = new byte[state.getFragmentShader().limit()];
            state.getFragmentShader().rewind();
            state.getFragmentShader().get(array);
            gl.glShaderSourceARB(state._fragmentShaderID, 1, new String[] { new String(array) },
                    new int[] { array.length }, 0);

            // Compile the fragment shader
            final IntBuffer compiled = BufferUtils.createIntBuffer(1);
            gl.glCompileShaderARB(state._fragmentShaderID);
            gl.glGetObjectParameterivARB(state._fragmentShaderID, GL.GL_OBJECT_COMPILE_STATUS_ARB, compiled);
            checkProgramError(compiled, state._fragmentShaderID);

            // Attatch the program
            gl.glAttachObjectARB(state._programID, state._fragmentShaderID);
        } else if (state._fragmentShaderID != -1) {
            removeFragShader(state);
            state._fragmentShaderID = -1;
        }

        gl.glLinkProgramARB(state._programID);
        state.setNeedsRefresh(true);
        state._needSendShader = false;
    }

    /** Removes the fragment shader */
    private static void removeFragShader(final GLSLShaderObjectsState state) {
        final GL gl = GLU.getCurrentGL();

        if (state._fragmentShaderID != -1) {
            gl.glDetachObjectARB(state._programID, state._fragmentShaderID);
            gl.glDeleteObjectARB(state._fragmentShaderID);
        }
    }

    /** Removes the vertex shader */
    private static void removeVertShader(final GLSLShaderObjectsState state) {
        final GL gl = GLU.getCurrentGL();

        if (state._vertexShaderID != -1) {
            gl.glDetachObjectARB(state._programID, state._vertexShaderID);
            gl.glDeleteObjectARB(state._vertexShaderID);
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
        final GL gl = GLU.getCurrentGL();

        if (compiled.get(0) == 0) {
            final IntBuffer iVal = BufferUtils.createIntBuffer(1);
            gl.glGetObjectParameterivARB(id, GL.GL_OBJECT_INFO_LOG_LENGTH_ARB, iVal);
            final int length = iVal.get();
            String out = null;

            if (length > 0) {
                final ByteBuffer infoLog = BufferUtils.createByteBuffer(length);

                iVal.flip();
                gl.glGetInfoLogARB(id, infoLog.limit(), iVal, infoLog);

                final byte[] infoBytes = new byte[length];
                infoLog.get(infoBytes);
                out = new String(infoBytes);
            }

            logger.severe(out);

            throw new Ardor3dException("Error compiling GLSL shader: " + out);
        }
    }

    public static void apply(final JoglRenderer renderer, final GLSLShaderObjectsState state) {
        final GL gl = GLU.getCurrentGL();
        final RenderContext context = ContextManager.getCurrentContext();
        final ContextCapabilities caps = context.getCapabilities();

        if (caps.isGLSLSupported()) {
            // Ask for the current state record
            final ShaderObjectsStateRecord record = (ShaderObjectsStateRecord) context
                    .getStateRecord(StateType.GLSLShader);
            context.setCurrentState(StateType.GLSLShader, state);

            if (state.isEnabled()) {
                if (state._needSendShader) {
                    sendToGL(state);
                }

                if (state._shaderDataLogic != null) {
                    state._shaderDataLogic.applyData(state, state._meshData, renderer);
                }
            }

            if (!record.isValid() || record.getReference() != state || state.needsRefresh()) {
                record.setReference(state);
                if (state.isEnabled()) {
                    if (state._programID != -1) {
                        gl.glUseProgramObjectARB(state._programID);

                        final List<ShaderVariable> attribs = state.getShaderAttributes();
                        for (int i = attribs.size(); --i >= 0;) {
                            final ShaderVariable shaderVariable = attribs.get(i);
                            if (shaderVariable.needsRefresh) {
                                JoglShaderUtil.updateAttributeLocation(shaderVariable, state._programID);
                                shaderVariable.needsRefresh = false;
                            }
                            JoglShaderUtil.updateShaderAttribute(shaderVariable);
                        }

                        final List<ShaderVariable> uniforms = state.getShaderUniforms();
                        for (int i = uniforms.size(); --i >= 0;) {
                            final ShaderVariable shaderVariable = uniforms.get(i);
                            if (shaderVariable.needsRefresh) {
                                JoglShaderUtil.updateUniformLocation(shaderVariable, state._programID);
                                JoglShaderUtil.updateShaderUniform(shaderVariable);
                                shaderVariable.needsRefresh = false;
                            }
                        }
                    }
                } else {
                    gl.glUseProgramObjectARB(0);
                }
            }

            if (!record.isValid()) {
                record.validate();
            }
        }
    }
}
