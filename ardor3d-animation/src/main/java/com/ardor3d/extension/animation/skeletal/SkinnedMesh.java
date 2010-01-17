/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.ardor3d.math.Matrix4;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.GLSLShaderObjectsState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.util.geom.BufferUtils;

public class SkinnedMesh extends Mesh {

    /** Maximum number of joints per vertex. */
    public static final int MAX_JOINTS_PER_VERTEX = 4;

    /** Storage for per vertex joint indices. There should be 4 entries per vertex. */
    protected ShortBuffer _jointIndices;

    /**
     * Storage for per vertex joint indices. These should already be normalized (all joints affecting the vertex add to
     * 1.) There should be 4 entries per vertex.
     */
    protected FloatBuffer _weights;

    /**
     * The original bind pose form of this SkinnedMesh. When doing CPU skinning, this will be used as a source and the
     * destination will go into the normal _meshData field for rendering. For GPU skinning, _meshData will be ignored
     * and only _bindPose will be sent to the card.
     */
    protected MeshData _bindPoseData = new MeshData();

    /**
     * The current skeleton pose we are targeting.
     */
    protected SkeletonPose _currentPose;

    /**
     * Flag for switching between GPU and CPU skinning.
     */
    private boolean _useGPU;

    /**
     * The shader state to update with GLSL attributes/uniforms related to GPU skinning. See class doc for more.
     */
    private GLSLShaderObjectsState _gpuShader;

    /**
     * Constructs a new SkinnedMesh.
     */
    public SkinnedMesh() {
        super();
    }

    /**
     * Constructs a new SkinnedMesh with a given name.
     * 
     * @param name
     *            the name of the skinned mesh.
     */
    public SkinnedMesh(final String name) {
        super(name);
    }

    /**
     * @return the bind pose MeshData object used by this skinned mesh.
     */
    public MeshData getBindPoseData() {
        return _bindPoseData;
    }

    /**
     * Sets the bind pose mesh data object used by this skinned mesh.
     * 
     * @param poseData
     *            the new bind pose
     */
    public void setBindPoseData(final MeshData poseData) {
        _bindPoseData = poseData;
    }

    /**
     * @return this skinned mesh's joint influences as indices into a Skeleton's Joint array.
     * @see #setJointIndices(ShortBuffer)
     */
    public ShortBuffer getJointIndices() {
        return _jointIndices;
    }

    /**
     * Sets the joint indices used by this skinned mesh to compute mesh deformation. There should be 4 entries per
     * vertex. Each entry is interpreted as an 16bit signed integer index into a Skeleton's Joint.
     * 
     * @param jointIndices
     */
    public void setJointIndices(final ShortBuffer jointIndices) {
        _jointIndices = jointIndices;
    }

    /**
     * @return this skinned mesh's joint weights.
     * @see #setWeights(FloatBuffer)
     */
    public FloatBuffer getWeights() {
        return _weights;
    }

    /**
     * Sets the joint weights used by this skinned mesh. There should be 4 entries per bind pose vertex.
     * 
     * @param weights
     *            the new weights.
     */
    public void setWeights(final FloatBuffer weights) {
        _weights = weights;
    }

    /**
     * @return a representation of the pose and skeleton to use for morphing this mesh.
     */
    public SkeletonPose getCurrentPose() {
        return _currentPose;
    }

    /**
     * @param currentPose
     *            the representation responsible for the pose and skeleton to use for morphing this mesh.
     */
    public void setCurrentPose(final SkeletonPose currentPose) {
        _currentPose = currentPose;
    }

    /**
     * @return true if we are doing skinning on the card (GPU) or false if on the CPU.
     */
    public boolean isUseGPU() {
        return _useGPU;
    }

    /**
     * @param useGPU
     *            true if we should do skinning on the card (GPU) or false if on the CPU.
     */
    public void setUseGPU(final boolean useGPU) {
        _useGPU = useGPU;
    }

    /**
     * @return the shader being used for GPU skinning. Must first have been set via
     *         {@link #setGPUShader(GLSLShaderObjectsState)}
     */
    public GLSLShaderObjectsState getGPUShader() {
        return _gpuShader;
    }

    /**
     * @param shaderState
     *            the shader to use for GPU skinning. Should be set up to accept vec4 attributes "Weights" and
     *            "JointIDs" and a mat4[] uniform called "JointPalette". Applies the renderstate to this mesh as well.
     */
    public void setGPUShader(final GLSLShaderObjectsState shaderState) {
        _gpuShader = shaderState;
        setRenderState(_gpuShader);
    }

    /**
     * Apply skinning values for GPU or CPU skinning.
     */
    public void applyPose() {
        if (isUseGPU()) {
            if (_gpuShader != null) {
                _gpuShader.setAttributePointer("Weights", 4, false, 0, _weights);
                _gpuShader.setAttributePointer("JointIDs", 4, false, false, 0, _jointIndices);
                _gpuShader.setUniform("JointPalette", _currentPose.getMatrixPalette(), true);
            }
        } else {
            final float[] weights = new float[SkinnedMesh.MAX_JOINTS_PER_VERTEX * _bindPoseData.getVertexCount()];
            final short[] jointIndices = new short[SkinnedMesh.MAX_JOINTS_PER_VERTEX * _bindPoseData.getVertexCount()];

            // pull in joint data
            _weights.rewind();
            _weights.get(weights);
            _jointIndices.rewind();
            _jointIndices.get(jointIndices);

            // Get a handle to the source and dest vertices buffers
            final FloatBuffer bindVerts = _bindPoseData.getVertexBuffer();
            FloatBuffer storeVerts = _meshData.getVertexBuffer();
            bindVerts.rewind();
            if (storeVerts == null || storeVerts.capacity() != bindVerts.capacity()) {
                storeVerts = BufferUtils.createFloatBuffer(bindVerts.capacity());
                _meshData.setVertexBuffer(storeVerts);
            } else {
                storeVerts.rewind();
            }

            // Get a handle to the source and dest normals buffers
            final FloatBuffer bindNorms = _bindPoseData.getNormalBuffer();
            FloatBuffer storeNorms = _meshData.getNormalBuffer();
            if (bindNorms != null) {
                bindNorms.rewind();

                if (storeNorms == null || storeNorms.capacity() < bindNorms.capacity()) {
                    storeNorms = BufferUtils.createFloatBuffer(bindNorms.capacity());
                    _meshData.setVertexBuffer(storeNorms);
                } else {
                    storeNorms.rewind();
                }
            }

            Matrix4 jntMat;
            double bindVX, bindVY, bindVZ;
            double bindNX = 0, bindNY = 0, bindNZ = 0;
            double vSumX, vSumY, vSumZ;
            double nSumX = 0, nSumY = 0, nSumZ = 0;
            double tempX, tempY, tempZ;
            float weight;
            int jointIndex;

            // Cycle through each vertex
            for (int i = 0; i < _bindPoseData.getVertexCount(); i++) {
                // zero out our sum var
                vSumX = 0;
                vSumY = 0;
                vSumZ = 0;

                // Grab the bind pose vertex Vbp from _bindPoseData
                bindVX = bindVerts.get();
                bindVY = bindVerts.get();
                bindVZ = bindVerts.get();

                // See if we should do the corresponding normal as well
                if (bindNorms != null) {
                    // zero out our sum var
                    nSumX = 0;
                    nSumY = 0;
                    nSumZ = 0;

                    // Grab the bind pose norm Nbp from _bindPoseData
                    bindNX = bindNorms.get();
                    bindNY = bindNorms.get();
                    bindNZ = bindNorms.get();
                }

                // for each joint where the weight != 0
                for (int j = 0; j < SkinnedMesh.MAX_JOINTS_PER_VERTEX; j++) {
                    final int index = i * SkinnedMesh.MAX_JOINTS_PER_VERTEX + j;
                    if (weights[index] == 0) {
                        continue;
                    }

                    jointIndex = jointIndices[index];
                    jntMat = _currentPose.getMatrixPalette()[jointIndex];
                    weight = weights[index];

                    // Multiply our vertex by the matrix palette entry
                    tempX = jntMat.getValue(0, 0) * bindVX + jntMat.getValue(0, 1) * bindVY + jntMat.getValue(0, 2)
                            * bindVZ + jntMat.getValue(0, 3);
                    tempY = jntMat.getValue(1, 0) * bindVX + jntMat.getValue(1, 1) * bindVY + jntMat.getValue(1, 2)
                            * bindVZ + jntMat.getValue(1, 3);
                    tempZ = jntMat.getValue(2, 0) * bindVX + jntMat.getValue(2, 1) * bindVY + jntMat.getValue(2, 2)
                            * bindVZ + jntMat.getValue(2, 3);

                    // Sum, weighted.
                    vSumX += tempX * weight;
                    vSumY += tempY * weight;
                    vSumZ += tempZ * weight;

                    if (bindNorms != null) {
                        // Multiply our normal by the matrix palette entry
                        tempX = jntMat.getValue(0, 0) * bindNX + jntMat.getValue(0, 1) * bindNY + jntMat.getValue(0, 2)
                                * bindNZ;
                        tempY = jntMat.getValue(1, 0) * bindNX + jntMat.getValue(1, 1) * bindNY + jntMat.getValue(1, 2)
                                * bindNZ;
                        tempZ = jntMat.getValue(2, 0) * bindNX + jntMat.getValue(2, 1) * bindNY + jntMat.getValue(2, 2)
                                * bindNZ;

                        // Sum, weighted.
                        nSumX += tempX * weight;
                        nSumY += tempY * weight;
                        nSumZ += tempZ * weight;
                    }
                }

                // Store sum into _meshData
                storeVerts.put((float) vSumX).put((float) vSumY).put((float) vSumZ);

                if (bindNorms != null) {
                    storeNorms.put((float) nSumX).put((float) nSumY).put((float) nSumZ);
                }
            }
        }
    }

    /**
     * Override render to allow for GPU/CPU switch
     */
    @Override
    public void render(final Renderer renderer) {
        if (!_useGPU) {
            super.render(renderer);
        } else {
            super.render(renderer, getBindPoseData());
        }
    }
}
