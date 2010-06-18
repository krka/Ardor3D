/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scene.state.jogl.shader;

import java.nio.FloatBuffer;
import java.util.logging.Logger;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.ShaderObjectsStateRecord;
import com.ardor3d.util.shader.ShaderVariable;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableFloat;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableFloat2;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableFloat3;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableFloat4;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableFloatArray;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableInt;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableInt2;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableInt3;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableInt4;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableIntArray;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableMatrix2;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableMatrix3;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableMatrix4;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableMatrix4Array;
import com.ardor3d.util.shader.uniformtypes.ShaderVariablePointerByte;
import com.ardor3d.util.shader.uniformtypes.ShaderVariablePointerFloat;
import com.ardor3d.util.shader.uniformtypes.ShaderVariablePointerFloatMatrix;
import com.ardor3d.util.shader.uniformtypes.ShaderVariablePointerInt;
import com.ardor3d.util.shader.uniformtypes.ShaderVariablePointerShort;

/** Utility class for updating shadervariables(uniforms and attributes) */
public abstract class JoglShaderUtil {
    private static final Logger logger = Logger.getLogger(JoglShaderUtil.class.getName());

    /**
     * Updates a uniform shadervariable.
     * 
     * @param shaderVariable
     *            variable to update
     */
    public static void updateShaderUniform(final ShaderVariable shaderVariable) {
        if (shaderVariable instanceof ShaderVariableInt) {
            updateShaderUniform((ShaderVariableInt) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariableInt2) {
            updateShaderUniform((ShaderVariableInt2) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariableInt3) {
            updateShaderUniform((ShaderVariableInt3) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariableInt4) {
            updateShaderUniform((ShaderVariableInt4) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariableIntArray) {
            updateShaderUniform((ShaderVariableIntArray) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariableFloat) {
            updateShaderUniform((ShaderVariableFloat) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariableFloat2) {
            updateShaderUniform((ShaderVariableFloat2) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariableFloat3) {
            updateShaderUniform((ShaderVariableFloat3) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariableFloat4) {
            updateShaderUniform((ShaderVariableFloat4) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariableFloatArray) {
            updateShaderUniform((ShaderVariableFloatArray) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariableMatrix2) {
            updateShaderUniform((ShaderVariableMatrix2) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariableMatrix3) {
            updateShaderUniform((ShaderVariableMatrix3) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariableMatrix4) {
            updateShaderUniform((ShaderVariableMatrix4) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariableMatrix4Array) {
            updateShaderUniform((ShaderVariableMatrix4Array) shaderVariable);
        } else {
            logger.warning("updateShaderUniform: Unknown shaderVariable type!");
        }
    }

    /**
     * Update variableID for uniform shadervariable if needed.
     * 
     * @param variable
     *            shadervaribale to update ID on
     * @param programID
     *            shader program context ID
     */
    public static void updateUniformLocation(final ShaderVariable variable, final int programID) {
        final GL gl = GLU.getCurrentGL();

        if (variable.variableID == -1) {
            variable.variableID = gl.glGetUniformLocationARB(programID, variable.name); // TODO Check variable.name

            if (variable.variableID == -1 && !variable.errorLogged) {
                logger.severe("Shader uniform [" + variable.name + "] could not be located in shader");
                variable.errorLogged = true;
            }
        }
    }

    private static void updateShaderUniform(final ShaderVariableInt shaderUniform) {
        final GL gl = GLU.getCurrentGL();

        gl.glUniform1iARB(shaderUniform.variableID, shaderUniform.value1);
    }

    private static void updateShaderUniform(final ShaderVariableInt2 shaderUniform) {
        final GL gl = GLU.getCurrentGL();

        gl.glUniform2iARB(shaderUniform.variableID, shaderUniform.value1, shaderUniform.value2);
    }

    private static void updateShaderUniform(final ShaderVariableInt3 shaderUniform) {
        final GL gl = GLU.getCurrentGL();

        gl.glUniform3iARB(shaderUniform.variableID, shaderUniform.value1, shaderUniform.value2, shaderUniform.value3);
    }

    private static void updateShaderUniform(final ShaderVariableInt4 shaderUniform) {
        final GL gl = GLU.getCurrentGL();

        gl.glUniform4iARB(shaderUniform.variableID, shaderUniform.value1, shaderUniform.value2, shaderUniform.value3,
                shaderUniform.value4);
    }

    private static void updateShaderUniform(final ShaderVariableIntArray shaderUniform) {
        final GL gl = GLU.getCurrentGL();

        switch (shaderUniform.size) {
            case 1:
                gl.glUniform1ivARB(shaderUniform.variableID, shaderUniform.value.remaining(), shaderUniform.value);
                break;
            case 2:
                gl.glUniform2ivARB(shaderUniform.variableID, shaderUniform.value.remaining(), shaderUniform.value);
                break;
            case 3:
                gl.glUniform3ivARB(shaderUniform.variableID, shaderUniform.value.remaining(), shaderUniform.value);
                break;
            case 4:
                gl.glUniform4ivARB(shaderUniform.variableID, shaderUniform.value.remaining(), shaderUniform.value);
                break;
            default:
                throw new IllegalArgumentException("Wrong size: " + shaderUniform.size);
        }
    }

    private static void updateShaderUniform(final ShaderVariableFloat shaderUniform) {
        final GL gl = GLU.getCurrentGL();

        gl.glUniform1fARB(shaderUniform.variableID, shaderUniform.value1);
    }

    private static void updateShaderUniform(final ShaderVariableFloat2 shaderUniform) {
        final GL gl = GLU.getCurrentGL();

        gl.glUniform2fARB(shaderUniform.variableID, shaderUniform.value1, shaderUniform.value2);
    }

    private static void updateShaderUniform(final ShaderVariableFloat3 shaderUniform) {
        final GL gl = GLU.getCurrentGL();

        gl.glUniform3fARB(shaderUniform.variableID, shaderUniform.value1, shaderUniform.value2, shaderUniform.value3);
    }

    private static void updateShaderUniform(final ShaderVariableFloat4 shaderUniform) {
        final GL gl = GLU.getCurrentGL();

        gl.glUniform4fARB(shaderUniform.variableID, shaderUniform.value1, shaderUniform.value2, shaderUniform.value3,
                shaderUniform.value4);
    }

    private static void updateShaderUniform(final ShaderVariableFloatArray shaderUniform) {
        final GL gl = GLU.getCurrentGL();

        switch (shaderUniform.size) {
            case 1:
                gl.glUniform1fvARB(shaderUniform.variableID, shaderUniform.value.remaining(), shaderUniform.value);
                break;
            case 2:
                gl.glUniform2fvARB(shaderUniform.variableID, shaderUniform.value.remaining(), shaderUniform.value);
                break;
            case 3:
                gl.glUniform3fvARB(shaderUniform.variableID, shaderUniform.value.remaining(), shaderUniform.value);
                break;
            case 4:
                gl.glUniform4fvARB(shaderUniform.variableID, shaderUniform.value.remaining(), shaderUniform.value);
                break;
            default:
                throw new IllegalArgumentException("Wrong size: " + shaderUniform.size);
        }
    }

    private static void updateShaderUniform(final ShaderVariableMatrix2 shaderUniform) {
        final GL gl = GLU.getCurrentGL();

        shaderUniform.matrixBuffer.rewind();
        gl.glUniformMatrix2fv(shaderUniform.variableID, 1, shaderUniform.rowMajor, shaderUniform.matrixBuffer);
    }

    private static void updateShaderUniform(final ShaderVariableMatrix3 shaderUniform) {
        final GL gl = GLU.getCurrentGL();

        shaderUniform.matrixBuffer.rewind();
        gl.glUniformMatrix3fv(shaderUniform.variableID, 1, shaderUniform.rowMajor, shaderUniform.matrixBuffer);
    }

    private static void updateShaderUniform(final ShaderVariableMatrix4 shaderUniform) {
        final GL gl = GLU.getCurrentGL();

        shaderUniform.matrixBuffer.rewind();
        gl.glUniformMatrix4fv(shaderUniform.variableID, 1, shaderUniform.rowMajor, shaderUniform.matrixBuffer);
    }

    private static void updateShaderUniform(final ShaderVariableMatrix4Array shaderUniform) {
        final GL gl = GLU.getCurrentGL();

        shaderUniform.matrixBuffer.rewind();
        // count == number of matrices we are sending, or iotw, limit / 16
        gl.glUniformMatrix4fv(shaderUniform.variableID, shaderUniform.matrixBuffer.limit() >> 4,
                shaderUniform.rowMajor, shaderUniform.matrixBuffer);
    }

    /**
     * Update variableID for attribute shadervariable if needed.
     * 
     * @param variable
     *            shadervaribale to update ID on
     * @param programID
     *            shader program context ID
     */
    public static void updateAttributeLocation(final ShaderVariable variable, final int programID) {
        final GL gl = GLU.getCurrentGL();

        if (variable.variableID == -1) {
            variable.variableID = gl.glGetAttribLocationARB(programID, variable.name); // TODO Check variable.name

            if (variable.variableID == -1 && !variable.errorLogged) {
                logger.severe("Shader attribute [" + variable.name + "] could not be located in shader");
                variable.errorLogged = true;
            }
        }
    }

    /**
     * Updates an attribute shadervariable.
     * 
     * @param renderer
     *            the current renderer
     * @param shaderVariable
     *            variable to update
     */
    public static void updateShaderAttribute(final Renderer renderer, final ShaderVariable shaderVariable) {
        if (shaderVariable.variableID == -1) {
            // attribute is not bound, or was not found in shader.
            return;
        }

        final RenderContext context = ContextManager.getCurrentContext();
        final ContextCapabilities caps = context.getCapabilities();
        if (caps.isVBOSupported()) {
            renderer.unbindVBO();
        }

        final ShaderObjectsStateRecord record = (ShaderObjectsStateRecord) context.getStateRecord(StateType.GLSLShader);

        if (shaderVariable instanceof ShaderVariablePointerFloat) {
            updateShaderAttribute((ShaderVariablePointerFloat) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariablePointerFloatMatrix) {
            updateShaderAttribute((ShaderVariablePointerFloatMatrix) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariablePointerByte) {
            updateShaderAttribute((ShaderVariablePointerByte) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariablePointerInt) {
            updateShaderAttribute((ShaderVariablePointerInt) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariablePointerShort) {
            updateShaderAttribute((ShaderVariablePointerShort) shaderVariable);
        } else {
            logger.warning("updateShaderAttribute: Unknown shaderVariable type!");
            return;
        }

        record.enabledAttributes.add(shaderVariable.variableID);
    }

    private static void updateShaderAttribute(final ShaderVariablePointerFloat shaderUniform) {
        final GL gl = GLU.getCurrentGL();

        shaderUniform.data.rewind();
        gl.glEnableVertexAttribArrayARB(shaderUniform.variableID);
        gl.glVertexAttribPointerARB(shaderUniform.variableID, shaderUniform.size, GL.GL_FLOAT,
                shaderUniform.normalized, shaderUniform.stride, shaderUniform.data);
    }

    private static void updateShaderAttribute(final ShaderVariablePointerFloatMatrix shaderUniform) {
        final GL gl = GLU.getCurrentGL();
        final FloatBuffer data = shaderUniform.data.duplicate();
        final int size = shaderUniform.size;
        final int length = data.capacity() / size;
        int pos = 0;
        for (int i = 0; i < size; i++) {
            pos = (i * length);
            data.limit(pos + length - 1);
            data.position(pos);
            gl.glEnableVertexAttribArrayARB(shaderUniform.variableID + i);
            gl.glVertexAttribPointerARB(shaderUniform.variableID + i, size, GL.GL_FLOAT, shaderUniform.normalized, 0,
                    data);
        }
    }

    private static void updateShaderAttribute(final ShaderVariablePointerByte shaderUniform) {
        final GL gl = GLU.getCurrentGL();

        shaderUniform.data.rewind();
        gl.glEnableVertexAttribArrayARB(shaderUniform.variableID);
        gl.glVertexAttribPointerARB(shaderUniform.variableID, shaderUniform.size, GL.GL_UNSIGNED_BYTE,
                shaderUniform.normalized, shaderUniform.stride, shaderUniform.data);
    }

    private static void updateShaderAttribute(final ShaderVariablePointerInt shaderUniform) {
        final GL gl = GLU.getCurrentGL();

        shaderUniform.data.rewind();
        gl.glEnableVertexAttribArrayARB(shaderUniform.variableID);
        gl.glVertexAttribPointerARB(shaderUniform.variableID, shaderUniform.size, GL.GL_UNSIGNED_INT,
                shaderUniform.normalized, shaderUniform.stride, shaderUniform.data);
    }

    private static void updateShaderAttribute(final ShaderVariablePointerShort shaderUniform) {
        final GL gl = GLU.getCurrentGL();

        shaderUniform.data.rewind();
        gl.glEnableVertexAttribArrayARB(shaderUniform.variableID);
        gl.glVertexAttribPointerARB(shaderUniform.variableID, shaderUniform.size, GL.GL_UNSIGNED_SHORT,
                shaderUniform.normalized, shaderUniform.stride, shaderUniform.data);
    }
}
