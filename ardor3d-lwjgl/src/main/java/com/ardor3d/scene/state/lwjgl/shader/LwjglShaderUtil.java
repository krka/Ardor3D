/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scene.state.lwjgl.shader;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexProgram;
import org.lwjgl.opengl.ARBVertexShader;

import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.shader.ShaderVariable;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableFloat;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableFloat2;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableFloat3;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableFloat4;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableInt;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableInt2;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableInt3;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableInt4;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableMatrix2;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableMatrix3;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableMatrix4;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableMatrix4Array;
import com.ardor3d.util.shader.uniformtypes.ShaderVariablePointerByte;
import com.ardor3d.util.shader.uniformtypes.ShaderVariablePointerFloat;
import com.ardor3d.util.shader.uniformtypes.ShaderVariablePointerInt;
import com.ardor3d.util.shader.uniformtypes.ShaderVariablePointerShort;

/** Utility class for updating shadervariables(uniforms and attributes) */
public class LwjglShaderUtil {
    private static final Logger logger = Logger.getLogger(LwjglShaderUtil.class.getName());

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
        } else if (shaderVariable instanceof ShaderVariableFloat) {
            updateShaderUniform((ShaderVariableFloat) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariableFloat2) {
            updateShaderUniform((ShaderVariableFloat2) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariableFloat3) {
            updateShaderUniform((ShaderVariableFloat3) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariableFloat4) {
            updateShaderUniform((ShaderVariableFloat4) shaderVariable);
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
        if (variable.variableID == -1) {
            final ByteBuffer nameBuf = BufferUtils.createByteBuffer(variable.name.getBytes().length + 1);
            nameBuf.clear();
            nameBuf.put(variable.name.getBytes());
            nameBuf.rewind();

            variable.variableID = ARBShaderObjects.glGetUniformLocationARB(programID, nameBuf);

            if (variable.variableID == -1) {
                logger.severe("Shader uniform [" + variable.name + "] could not be located in shader");
            }
        }
    }

    private static void updateShaderUniform(final ShaderVariableInt shaderUniform) {
        ARBShaderObjects.glUniform1iARB(shaderUniform.variableID, shaderUniform.value1);
    }

    private static void updateShaderUniform(final ShaderVariableInt2 shaderUniform) {
        ARBShaderObjects.glUniform2iARB(shaderUniform.variableID, shaderUniform.value1, shaderUniform.value2);
    }

    private static void updateShaderUniform(final ShaderVariableInt3 shaderUniform) {
        ARBShaderObjects.glUniform3iARB(shaderUniform.variableID, shaderUniform.value1, shaderUniform.value2,
                shaderUniform.value3);
    }

    private static void updateShaderUniform(final ShaderVariableInt4 shaderUniform) {
        ARBShaderObjects.glUniform4iARB(shaderUniform.variableID, shaderUniform.value1, shaderUniform.value2,
                shaderUniform.value3, shaderUniform.value4);
    }

    private static void updateShaderUniform(final ShaderVariableFloat shaderUniform) {
        ARBShaderObjects.glUniform1fARB(shaderUniform.variableID, shaderUniform.value1);
    }

    private static void updateShaderUniform(final ShaderVariableFloat2 shaderUniform) {
        ARBShaderObjects.glUniform2fARB(shaderUniform.variableID, shaderUniform.value1, shaderUniform.value2);
    }

    private static void updateShaderUniform(final ShaderVariableFloat3 shaderUniform) {
        ARBShaderObjects.glUniform3fARB(shaderUniform.variableID, shaderUniform.value1, shaderUniform.value2,
                shaderUniform.value3);
    }

    private static void updateShaderUniform(final ShaderVariableFloat4 shaderUniform) {
        ARBShaderObjects.glUniform4fARB(shaderUniform.variableID, shaderUniform.value1, shaderUniform.value2,
                shaderUniform.value3, shaderUniform.value4);
    }

    private static void updateShaderUniform(final ShaderVariableMatrix2 shaderUniform) {
        shaderUniform.matrixBuffer.rewind();
        ARBShaderObjects.glUniformMatrix2ARB(shaderUniform.variableID, shaderUniform.rowMajor,
                shaderUniform.matrixBuffer);
    }

    private static void updateShaderUniform(final ShaderVariableMatrix3 shaderUniform) {
        shaderUniform.matrixBuffer.rewind();
        ARBShaderObjects.glUniformMatrix3ARB(shaderUniform.variableID, shaderUniform.rowMajor,
                shaderUniform.matrixBuffer);
    }

    private static void updateShaderUniform(final ShaderVariableMatrix4 shaderUniform) {
        shaderUniform.matrixBuffer.rewind();
        ARBShaderObjects.glUniformMatrix4ARB(shaderUniform.variableID, shaderUniform.rowMajor,
                shaderUniform.matrixBuffer);
    }

    private static void updateShaderUniform(final ShaderVariableMatrix4Array shaderUniform) {
        shaderUniform.matrixBuffer.rewind();
        ARBShaderObjects.glUniformMatrix4ARB(shaderUniform.variableID, shaderUniform.rowMajor,
                shaderUniform.matrixBuffer);
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
        if (variable.variableID == -1) {
            final ByteBuffer nameBuf = BufferUtils.createByteBuffer(variable.name.getBytes().length + 1);
            nameBuf.clear();
            nameBuf.put(variable.name.getBytes());
            nameBuf.rewind();

            variable.variableID = ARBVertexShader.glGetAttribLocationARB(programID, nameBuf);

            if (variable.variableID == -1) {
                logger.severe("Shader attribute [" + variable.name + "] could not be located in shader");
            }
        }
    }

    /**
     * Updates an attribute shadervariable.
     * 
     * @param shaderVariable
     *            variable to update
     */
    public static void updateShaderAttribute(final ShaderVariable shaderVariable) {
        if (shaderVariable instanceof ShaderVariablePointerFloat) {
            updateShaderAttribute((ShaderVariablePointerFloat) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariablePointerByte) {
            updateShaderAttribute((ShaderVariablePointerByte) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariablePointerInt) {
            updateShaderAttribute((ShaderVariablePointerInt) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariablePointerShort) {
            updateShaderAttribute((ShaderVariablePointerShort) shaderVariable);
        } else {
            logger.warning("updateShaderAttribute: Unknown shaderVariable type!");
        }
    }

    private static void updateShaderAttribute(final ShaderVariablePointerFloat shaderUniform) {
        shaderUniform.data.rewind();
        ARBVertexProgram.glEnableVertexAttribArrayARB(shaderUniform.variableID);
        ARBVertexProgram.glVertexAttribPointerARB(shaderUniform.variableID, shaderUniform.size,
                shaderUniform.normalized, shaderUniform.stride, shaderUniform.data);
    }

    private static void updateShaderAttribute(final ShaderVariablePointerByte shaderUniform) {
        shaderUniform.data.rewind();
        ARBVertexProgram.glEnableVertexAttribArrayARB(shaderUniform.variableID);
        ARBVertexProgram.glVertexAttribPointerARB(shaderUniform.variableID, shaderUniform.size, shaderUniform.unsigned,
                shaderUniform.normalized, shaderUniform.stride, shaderUniform.data);
    }

    private static void updateShaderAttribute(final ShaderVariablePointerInt shaderUniform) {
        shaderUniform.data.rewind();
        ARBVertexProgram.glEnableVertexAttribArrayARB(shaderUniform.variableID);
        ARBVertexProgram.glVertexAttribPointerARB(shaderUniform.variableID, shaderUniform.size, shaderUniform.unsigned,
                shaderUniform.normalized, shaderUniform.stride, shaderUniform.data);
    }

    private static void updateShaderAttribute(final ShaderVariablePointerShort shaderUniform) {
        shaderUniform.data.rewind();
        ARBVertexProgram.glEnableVertexAttribArrayARB(shaderUniform.variableID);
        ARBVertexProgram.glVertexAttribPointerARB(shaderUniform.variableID, shaderUniform.size, shaderUniform.unsigned,
                shaderUniform.normalized, shaderUniform.stride, shaderUniform.data);
    }
}
