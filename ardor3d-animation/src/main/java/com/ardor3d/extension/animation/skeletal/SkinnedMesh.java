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

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.ardor3d.math.Matrix4;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.GLSLShaderObjectsState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.geom.BufferUtils;

/**
 * Mesh supporting deformation via skeletal animation.
 */
public class SkinnedMesh extends Mesh implements PoseListener {

    /**
     * Number of weights per vertex.
     */
    protected int _weightsPerVert = 1;

    /**
     * If true and we are using gpu skinning, we'll reorder our weights for matrix attribute use.
     */
    protected boolean _gpuUseMatrixAttribute = false;

    /**
     * Size to pad our attributes to. If we are using matrices (see {@link #setGpuUseMatrixAttribute(boolean)}) then
     * this is the size of an edge of the matrix. eg. 4 would mean either a vec4 or a mat4 object is expected in the
     * shader.
     */
    protected int _gpuAttributeSize = 4;

    /**
     * Storage for per vertex joint indices. There should be "weightsPerVert" entries per vertex.
     */
    protected short[] _jointIndices;
    protected FloatBuffer _jointIndicesBuf;

    /**
     * Storage for per vertex joint indices. These should already be normalized (all joints affecting the vertex add to
     * 1.) There should be "weightsPerVert" entries per vertex.
     */
    protected float[] _weights;
    protected FloatBuffer _weightsBuf;

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
     * <p>
     * Flag for enabling automatically updating the skin's model bound when the pose changes. Only effective in CPU
     * skinning mode. Default is false as this is currently expensive.
     * </p>
     * 
     * XXX: If we can find a better way to update the bounds, maybe we should make this default to true or remove this
     * altogether.
     */
    private boolean _autoUpdateSkinBound = false;

    /**
     * Custom update apply logic.
     */
    private SkinPoseApplyLogic _customApplier = null;

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
     * @return the number of weights and jointIndices this skin uses per vertex.
     */
    public int getWeightsPerVert() {
        return _weightsPerVert;
    }

    /**
     * @param weightsPerVert
     *            the number of weights and jointIndices this skin should use per vertex. Make sure this value matches
     *            up with the contents of jointIndices and weights.
     */
    public void setWeightsPerVert(final int weightsPerVert) {
        _weightsPerVert = weightsPerVert;
    }

    /**
     * @return true if we should use a matrix to send joints and weights to a gpu shader.
     */
    public boolean isGpuUseMatrixAttribute() {
        return _gpuUseMatrixAttribute;
    }

    /**
     * @param useMatrix
     *            true if we should use a matrix to send joints and weights to a gpu shader.
     */
    public void setGpuUseMatrixAttribute(final boolean useMatrix) {
        _gpuUseMatrixAttribute = useMatrix;
    }

    /**
     * @return size to pad our attributes to. If we are using matrices (see {@link #setGpuUseMatrixAttribute(boolean)})
     *         then this is the size of an edge of the matrix. eg. 4 would mean either a vec4 or a mat4 object is
     *         expected in the shader.
     */
    public int getGpuAttributeSize() {
        return _gpuAttributeSize;
    }

    /**
     * @param size
     *            Size to pad our attributes to. If we are using matrices (see
     *            {@link #setGpuUseMatrixAttribute(boolean)}) then this is the size of an edge of the matrix. eg. 4
     *            would mean either a vec4 or a mat4 object is expected in the shader.
     */
    public void setGpuAttributeSize(final int size) {
        _gpuAttributeSize = size;
    }

    /**
     * @return this skinned mesh's joint influences as indices into a Skeleton's Joint array.
     * @see #setJointIndices(ShortBuffer)
     */
    public short[] getJointIndices() {
        return _jointIndices;
    }

    /**
     * Sets the joint indices used by this skinned mesh to compute mesh deformation. Each entry is interpreted as an
     * 16bit signed integer index into a Skeleton's Joint.
     * 
     * @param jointIndices
     */
    public void setJointIndices(final short[] jointIndices) {
        _jointIndices = jointIndices;
        if (_jointIndices != null && _jointIndicesBuf != null) {
            createJointBuffer();
            updateWeightsAndJointsOnGPUShader();
        }
    }

    /**
     * @return this skinned mesh's joint weights.
     * @see #setWeights(FloatBuffer)
     */
    public float[] getWeights() {
        return _weights;
    }

    /**
     * Sets the joint weights used by this skinned mesh.
     * 
     * @param weights
     *            the new weights.
     */
    public void setWeights(final float[] weights) {
        _weights = weights;
        if (_weights != null && _weightsBuf != null) {
            createWeightBuffer();
            updateWeightsAndJointsOnGPUShader();
        }
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
        if (_currentPose != null) {
            _currentPose.removePoseListener(this);
        }
        _currentPose = currentPose;
        _currentPose.addPoseListener(this);
    }

    /**
     * @return true if we should automatically update our model bounds when our pose updates. If useGPU is true, bounds
     *         are ignored.
     */
    public boolean isAutoUpdateSkinBounds() {
        return _autoUpdateSkinBound;
    }

    /**
     * @param autoUpdateSkinBound
     *            true if we should automatically update our model bounds when our pose updates. If useGPU is true,
     *            bounds are ignored.
     */
    public void setAutoUpdateSkinBounds(final boolean autoUpdateSkinBound) {
        _autoUpdateSkinBound = autoUpdateSkinBound;
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

        updateWeightsAndJointsOnGPUShader();

    }

    private void updateWeightsAndJointsOnGPUShader() {
        if (isUseGPU() && _gpuShader != null) {
            if (_weightsBuf == null) {
                createWeightBuffer();
            }
            _gpuShader.setAttributePointerMatrix("Weights", getGpuAttributeSize(), false, _weightsBuf);
            if (_jointIndicesBuf == null) {
                createJointBuffer();
            }
            _gpuShader.setAttributePointerMatrix("JointIDs", getGpuAttributeSize(), false, _jointIndicesBuf);
        }
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
     * @return any custom apply logic set on this skin or null if default logic is used.
     * @see #setCustomApplier(SkinPoseApplyLogic)
     */
    public SkinPoseApplyLogic getCustomApplier() {
        return _customApplier;
    }

    /**
     * Set custom logic for how this skin should react when it is told its pose has updated. This might include
     * throttling skin application, ignoring skin application when the skin is outside of the camera view, etc. If null,
     * (the default) the skin will always apply the new pose and optionally update the model bound.
     * 
     * @param customApplier
     *            the new custom logic, or null to use the default behavior.
     */
    public void setCustomApplier(final SkinPoseApplyLogic customApplier) {
        _customApplier = customApplier;
    }

    /**
     * Apply skinning values for GPU or CPU skinning.
     */
    public void applyPose() {
        if (isUseGPU()) {
            if (_gpuShader != null) {
                _gpuShader.setUniform("JointPalette", _currentPose.getMatrixPalette(), true);
            }
        } else {
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
                for (int j = 0; j < getWeightsPerVert(); j++) {
                    final int index = i * getWeightsPerVert() + j;
                    if (_weights[index] == 0) {
                        continue;
                    }

                    jointIndex = _jointIndices[index];
                    jntMat = _currentPose.getMatrixPalette()[jointIndex];
                    weight = _weights[index];

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

            _meshData.getVertexCoords().setNeedsRefresh(true);
            if (bindNorms != null) {
                _meshData.getNormalCoords().setNeedsRefresh(true);
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

    /**
     * Calls to apply our pose on pose update.
     */
    public void poseUpdated(final SkeletonPose pose) {
        // custom behavior?
        if (_customApplier != null) {
            _customApplier.doApply(this, pose);
        }

        // Just run our default behavior
        else {
            // update our pose
            applyPose();

            // update our model bounds
            if (!isUseGPU() && isAutoUpdateSkinBounds()) {
                updateModelBound();
            }
        }
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    @Override
    public Class<? extends SkinnedMesh> getClassTag() {
        return this.getClass();
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_weightsPerVert, "weightsPerVert", 1);
        capsule.write(_jointIndices, "jointIndices", null);
        capsule.write(_weights, "weights", null);
        capsule.write(_bindPoseData, "bindPoseData", null);
        capsule.write(_currentPose, "currentPose", null);
        capsule.write(_useGPU, "useGPU", false);
        capsule.write(_gpuShader, "gpuShader", null);
        capsule.write(_autoUpdateSkinBound, "autoUpdateSkinBound", false);
        if (_customApplier instanceof Savable) {
            capsule.write((Savable) _customApplier, "customApplier", null);
        }
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _weightsPerVert = capsule.readInt("weightsPerVert", 1);
        _jointIndices = capsule.readShortArray("jointIndices", null);
        _weights = capsule.readFloatArray("weights", null);
        _bindPoseData = (MeshData) capsule.readSavable("bindPoseData", null);
        _currentPose = (SkeletonPose) capsule.readSavable("currentPose", null);
        _useGPU = capsule.readBoolean("useGPU", false);
        _gpuShader = (GLSLShaderObjectsState) capsule.readSavable("gpuShader", null);
        _autoUpdateSkinBound = capsule.readBoolean("autoUpdateSkinBound", false);
        final SkinPoseApplyLogic customApplier = (SkinPoseApplyLogic) capsule.readSavable("customApplier", null);
        if (customApplier != null) {
            _customApplier = customApplier;
        }

        // make sure pose listener added
        if (_currentPose != null) {
            _currentPose.addPoseListener(this);
        }
    }

    private void createJointBuffer() {
        final float[] data = reorderAndPad(convert(_jointIndices), getWeightsPerVert(), getGpuAttributeSize());
        _jointIndicesBuf = BufferUtils.createFloatBuffer(_jointIndicesBuf, data);
    }

    private void createWeightBuffer() {
        final float[] data = reorderAndPad(_weights, getWeightsPerVert(), getGpuAttributeSize());
        _weightsBuf = BufferUtils.createFloatBuffer(_weightsBuf, data);
    }

    /**
     * Convert a short array to a float array
     * 
     * @param shorts
     *            the short values
     * @return our new float array
     */
    private float[] convert(final short... shorts) {
        final float[] rval = new float[shorts.length];
        for (int i = 0; i < rval.length; i++) {
            rval[i] = shorts[i];
        }
        return rval;
    }

    /**
     * Rearrange the data from data per element, to a list of matSide x matSide matrices, output by row as such:
     * 
     * row0element0, row0element1, row0element2...<br>
     * row1element0, row1element1, row1element2...<br>
     * row2element0, row2element1, row2element2...<br>
     * 
     * If there is not enough values in the source data to fill out a row, 0 is used.
     * 
     * @param src
     *            our source data, stored as element0, element1, etc.
     * @param size
     *            the number of values per element in our source element
     * @param matSide
     *            the size of the matrix edge... eg. 4 would mean a 4x4 matrix.
     * @return our new data array.
     */
    private float[] reorderAndPad(final float[] src, final int size, final int matSide) {
        final int elements = src.length / size;
        final float[] rVal = new float[elements * matSide * matSide];

        // size of each attribute (a row from each element)
        final int length = matSide * elements;

        for (int i = 0; i < elements; i++) {
            // index into src for our element data
            final int srcStart = i * size;
            // index into a destination row.
            final int dstOffset = i * matSide;

            // Go through each row of the current src element. Go through only as many rows of data as we have.
            // (eg. if size is 6 and matSide is 4, we only need to go through j=0 and j=1)
            for (int j = 0; j <= (size - 1) / matSide; j++) {
                // How much to copy. Generally matSide, except for last bit of data.
                final int copySize = Math.min(size - j * matSide, matSide);
                // Copy the data from src to rVal
                System.arraycopy(src, srcStart + j * matSide, rVal, j * length + dstOffset, copySize);
            }
        }

        return rVal;
    }
}
