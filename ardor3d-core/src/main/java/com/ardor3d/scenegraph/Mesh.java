/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.EnumMap;
import java.util.Map;
import java.util.Stack;

import com.ardor3d.bounding.BoundingSphere;
import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.GLSLShaderObjectsState;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.renderer.state.LightUtil;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.event.DirtyType;
import com.ardor3d.scenegraph.hint.DataMode;
import com.ardor3d.scenegraph.hint.NormalsMode;
import com.ardor3d.util.Constants;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.scenegraph.RenderDelegate;
import com.ardor3d.util.stat.StatCollector;
import com.ardor3d.util.stat.StatType;

/**
 * Mesh
 */
public class Mesh extends Spatial implements Renderable {

    public static boolean RENDER_VERTEX_ONLY = false;

    /** Actual buffer representation of the mesh */
    protected MeshData _meshData = new MeshData();

    /** Local model bounding volume */
    protected BoundingVolume _modelBound = new BoundingSphere(Double.POSITIVE_INFINITY, Vector3.ZERO);

    /**
     * The compiled list of renderstates for this mesh, taking into account ancestors states - updated with
     * updateRenderStates()
     */
    protected final EnumMap<RenderState.StateType, RenderState> _states = new EnumMap<RenderState.StateType, RenderState>(
            RenderState.StateType.class);

    /** The compiled lightState for this mesh */
    protected LightState _lightState;

    protected ColorRGBA _defaultColor = new ColorRGBA(ColorRGBA.WHITE);

    protected boolean _castsShadows = true;

    /**
     * Constructs a new Spatial.
     */
    public Mesh() {
        super();
    }

    /**
     * Constructs a new <code>Mesh</code> with a given name.
     * 
     * @param name
     *            the name of the mesh. This is required for identification purposes.
     */
    public Mesh(final String name) {
        super(name);
    }

    /**
     * Retrieves the mesh data object used by this mesh.
     * 
     * @return the mesh data object
     */
    public MeshData getMeshData() {
        return _meshData;
    }

    /**
     * Sets the mesh data object for this mesh.
     * 
     * @return the mesh data object
     */
    public void setMeshData(final MeshData meshData) {
        _meshData = meshData;
    }

    /**
     * Retrieves the local bounding volume for this mesh.
     * 
     * @param store
     *            the bounding volume
     */
    public BoundingVolume getModelBound(final BoundingVolume store) {
        if (_modelBound == null) {
            return null;
        }
        return _modelBound.clone(store);
    }

    /**
     * Sets the local bounding volume for this mesh. This will mark the spatial as having dirty bounds.
     * 
     * @param store
     *            the bounding volume
     */
    public void setModelBound(final BoundingVolume modelBound) {
        _modelBound = modelBound != null ? modelBound.clone(_modelBound) : null;
        updateModelBound();
    }

    /**
     * <code>updateBound</code> recalculates the bounding object assigned to the geometry. This resets it parameters to
     * adjust for any changes to the vertex information.
     */
    public void updateModelBound() {
        if (_modelBound != null && _meshData.getVertexBuffer() != null) {
            _modelBound.computeFromPoints(_meshData.getVertexBuffer());
            markDirty(DirtyType.Bounding);
        }
    }

    @Override
    public void updateWorldBound(final boolean recurse) {
        if (_modelBound != null) {
            _worldBound = _modelBound.transform(_worldTransform, _worldBound);
        } else {
            _worldBound = null;
        }
        clearDirty(DirtyType.Bounding);
    }

    /**
     * translates/rotates and scales the vectors of this Mesh to world coordinates based on its world settings. The
     * results are stored in the given FloatBuffer. If given FloatBuffer is null, one is created.
     * 
     * @param store
     *            the FloatBuffer to store the results in, or null if you want one created.
     * @return store or new FloatBuffer if store == null.
     */
    public FloatBuffer getWorldVectors(FloatBuffer store) {
        final FloatBuffer vertBuf = _meshData.getVertexBuffer();
        if (store == null || store.capacity() != vertBuf.limit()) {
            store = BufferUtils.createFloatBuffer(vertBuf.limit());
        }

        final Vector3 compVect = Vector3.fetchTempInstance();
        for (int v = 0, vSize = store.capacity() / 3; v < vSize; v++) {
            BufferUtils.populateFromBuffer(compVect, vertBuf, v);
            _worldTransform.applyForward(compVect);
            BufferUtils.setInBuffer(compVect, store, v);
        }
        return store;
    }

    /**
     * rotates the normals of this Mesh to world normals based on its world settings. The results are stored in the
     * given FloatBuffer. If given FloatBuffer is null, one is created.
     * 
     * @param store
     *            the FloatBuffer to store the results in, or null if you want one created.
     * @return store or new FloatBuffer if store == null.
     */
    public FloatBuffer getWorldNormals(FloatBuffer store) {
        final FloatBuffer normBuf = _meshData.getNormalBuffer();
        if (store == null || store.capacity() != normBuf.limit()) {
            store = BufferUtils.createFloatBuffer(normBuf.limit());
        }

        final Vector3 compVect = Vector3.fetchTempInstance();
        for (int v = 0, vSize = store.capacity() / 3; v < vSize; v++) {
            BufferUtils.populateFromBuffer(compVect, normBuf, v);
            _worldTransform.applyForwardVector(compVect);
            BufferUtils.setInBuffer(compVect, store, v);
        }
        Vector3.releaseTempInstance(compVect);
        return store;
    }

    public void render(final Renderer renderer) {
        // Set up MeshData in GLSLShaderObjectsState if necessary
        // XXX: considered a hack until we settle on our shader model.
        final GLSLShaderObjectsState glsl = (GLSLShaderObjectsState) _states.get(RenderState.StateType.GLSLShader);
        if (glsl != null && glsl.getShaderDataLogic() != null) {
            glsl.setMesh(this);
            glsl.setNeedsRefresh(true);
        }

        // Apply fixed function states before mesh transforms for proper function
        for (final StateType type : StateType.values) {
            if (type != StateType.GLSLShader && type != StateType.FragmentProgram && type != StateType.VertexProgram) {
                renderer.applyState(type, _states.get(type));
            }
        }

        final boolean transformed = renderer.doTransforms(_worldTransform);

        // Apply shader states here for the ability to retrieve mesh matrices
        renderer.applyState(StateType.GLSLShader, _states.get(StateType.GLSLShader));
        renderer.applyState(StateType.FragmentProgram, _states.get(StateType.FragmentProgram));
        renderer.applyState(StateType.VertexProgram, _states.get(StateType.VertexProgram));

        final RenderContext context = ContextManager.getCurrentContext();
        final ContextCapabilities caps = context.getCapabilities();

        if ((getSceneHints().getDataMode() == DataMode.VBO || getSceneHints().getDataMode() == DataMode.VBOInterleaved)
                && caps.isVBOSupported()) {
            if (getSceneHints().getDataMode() == DataMode.VBOInterleaved) {
                if (_meshData.getColorCoords() == null) {
                    renderer.applyDefaultColor(_defaultColor);
                }
                renderer.applyNormalsMode(getSceneHints().getNormalsMode(), _worldTransform);
                // Make sure we have a FBD to hold our id.
                if (_meshData.getInterleavedData() == null) {
                    final FloatBufferData interleaved = new FloatBufferData(FloatBuffer.allocate(0), 1);
                    _meshData.setInterleavedData(interleaved);
                }
                renderer.setupInterleavedDataVBO(_meshData.getInterleavedData(), _meshData.getVertexCoords(), _meshData
                        .getNormalCoords(), _meshData.getColorCoords(), _meshData.getTextureCoords());
            } else {
                if (RENDER_VERTEX_ONLY) {
                    renderer.applyNormalsMode(NormalsMode.Off, null);
                    renderer.setupNormalDataVBO(null);
                    renderer.applyDefaultColor(null);
                    renderer.setupColorDataVBO(null);
                    renderer.setupTextureDataVBO(null);
                } else {
                    renderer.applyNormalsMode(getSceneHints().getNormalsMode(), _worldTransform);
                    if (getSceneHints().getNormalsMode() != NormalsMode.Off) {
                        renderer.setupNormalDataVBO(_meshData.getNormalCoords());
                    } else {
                        renderer.setupNormalDataVBO(null);
                    }

                    if (_meshData.getColorCoords() != null) {
                        renderer.setupColorDataVBO(_meshData.getColorCoords());
                    } else {
                        renderer.applyDefaultColor(_defaultColor);
                        renderer.setupColorDataVBO(null);
                    }

                    renderer.setupTextureDataVBO(_meshData.getTextureCoords());
                }
                renderer.setupVertexDataVBO(_meshData.getVertexCoords());
            }

            if (_meshData.getIndexBuffer() != null) {
                // TODO: Maybe ask for the IndexBuffer's dynamic/static type and fall back to arrays for indices?
                renderer
                        .drawElementsVBO(_meshData.getIndices(), _meshData.getIndexLengths(), _meshData.getIndexModes());
            } else {
                renderer
                        .drawArrays(_meshData.getVertexCoords(), _meshData.getIndexLengths(), _meshData.getIndexModes());
            }

            if (Constants.stats) {
                StatCollector.addStat(StatType.STAT_VERTEX_COUNT, _meshData.getVertexCount());
                StatCollector.addStat(StatType.STAT_MESH_COUNT, 1);
            }
        } else {
            // Use arrays
            if (caps.isVBOSupported()) {
                renderer.unbindVBO();
            }

            if (RENDER_VERTEX_ONLY) {
                renderer.applyNormalsMode(NormalsMode.Off, null);
                renderer.setupNormalData(null);
                renderer.applyDefaultColor(null);
                renderer.setupColorData(null);
                renderer.setupTextureData(null);
            } else {
                renderer.applyNormalsMode(getSceneHints().getNormalsMode(), _worldTransform);
                if (getSceneHints().getNormalsMode() != NormalsMode.Off) {
                    renderer.setupNormalData(_meshData.getNormalCoords());
                } else {
                    renderer.setupNormalData(null);
                }

                if (_meshData.getColorCoords() != null) {
                    renderer.setupColorData(_meshData.getColorCoords());
                } else {
                    renderer.applyDefaultColor(_defaultColor);
                    renderer.setupColorData(null);
                }

                renderer.setupTextureData(_meshData.getTextureCoords());
            }
            renderer.setupVertexData(_meshData.getVertexCoords());

            if (_meshData.getIndexBuffer() != null) {
                renderer.drawElements(_meshData.getIndices(), _meshData.getIndexLengths(), _meshData.getIndexModes());
            } else {
                renderer
                        .drawArrays(_meshData.getVertexCoords(), _meshData.getIndexLengths(), _meshData.getIndexModes());
            }

            if (Constants.stats) {
                StatCollector.addStat(StatType.STAT_VERTEX_COUNT, _meshData.getVertexCount());
                StatCollector.addStat(StatType.STAT_MESH_COUNT, 1);
            }
        }

        if (transformed) {
            renderer.undoTransforms(_worldTransform);
        }
    }

    @Override
    protected void applyWorldRenderStates(final boolean recurse,
            final Map<RenderState.StateType, Stack<RenderState>> states) {
        // start with a blank slate
        _states.clear();

        // Go through each state stack and apply to our states list.
        RenderState state;
        for (final Stack<RenderState> stack : states.values()) {
            if (!stack.isEmpty()) {
                state = stack.peek().extract(stack, this);
                _states.put(state.getType(), state);
            }
        }
    }

    public boolean isCastsShadows() {
        return _castsShadows;
    }

    public void setCastsShadows(final boolean castsShadows) {
        _castsShadows = castsShadows;
    }

    /**
     * <code>reconstruct</code> reinitializes the geometry with new data. This will reuse the geometry object.
     * 
     * @param vertices
     *            the new vertices to use.
     * @param normals
     *            the new normals to use.
     * @param colors
     *            the new colors to use.
     * @param coords
     *            the new texture coordinates to use (position 0).
     */
    public void reconstruct(final FloatBuffer vertices, final FloatBuffer normals, final FloatBuffer colors,
            final FloatBufferData coords) {

        _meshData.setVertexBuffer(vertices);
        _meshData.setNormalBuffer(normals);
        _meshData.setColorBuffer(colors);
        _meshData.setTextureCoords(coords, 0);
    }

    /**
     * <code>reconstruct</code> reinitializes the geometry with new data. This will reuse the geometry object.
     * 
     * @param vertices
     *            the new vertices to use.
     * @param normals
     *            the new normals to use.
     * @param colors
     *            the new colors to use.
     * @param coords
     *            the new texture coordinates to use (position 0).
     */
    public void reconstruct(final FloatBuffer vertices, final FloatBuffer normals, final FloatBuffer colors,
            final FloatBufferData coords, final IntBuffer indices) {

        reconstruct(vertices, normals, colors, coords);
        _meshData.setIndexBuffer(indices);
    }

    /**
     * 
     */
    @Override
    public void draw(final Renderer r) {
        if (!r.isProcessingQueue()) {
            if (r.checkAndAdd(this)) {
                return;
            }
        }

        final RenderDelegate delegate = getCurrentRenderDelegate();
        if (delegate == null) {
            r.draw((Renderable) this);
        } else {
            delegate.render(this, r);
        }
    }

    /**
     * Sorts the lights based on distance to mesh bounding volume
     */
    @Override
    public void sortLights() {
        if (_lightState != null && _lightState.getLightList().size() > LightState.MAX_LIGHTS_ALLOWED) {
            LightUtil.sort(this, _lightState.getLightList());
        }
    }

    public LightState getLightState() {
        return _lightState;
    }

    public void setLightState(final LightState lightState) {
        _lightState = lightState;
    }

    /**
     * <code>setDefaultColor</code> sets the color to be used if no per vertex color buffer is set.
     * 
     * @param color
     */
    public void setDefaultColor(final ReadOnlyColorRGBA color) {
        _defaultColor.set(color);
    }

    /**
     * 
     * @param store
     * @return
     */
    public ReadOnlyColorRGBA getDefaultColor() {
        return _defaultColor;
    }

    /**
     * @param type
     *            StateType of RenderState we want to grab
     * @return the compiled RenderState for this Mesh, either from RenderStates applied locally or those inherited from
     *         this Mesh's ancestors. May be null if a state of the given type was never applied in either place.
     */
    public RenderState getWorldRenderState(final StateType type) {
        return _states.get(type);
    }

    /**
     * <code>setSolidColor</code> sets the color array of this geometry to a single color. For greater efficiency, try
     * setting the the ColorBuffer to null and using DefaultColor instead.
     * 
     * @param color
     *            the color to set.
     */
    public void setSolidColor(final ReadOnlyColorRGBA color) {
        FloatBuffer colorBuf = _meshData.getColorBuffer();
        if (colorBuf == null) {
            colorBuf = BufferUtils.createColorBuffer(_meshData.getVertexCount());
            _meshData.setColorBuffer(colorBuf);
        }

        colorBuf.rewind();
        for (int x = 0, cLength = colorBuf.remaining(); x < cLength; x += 4) {
            colorBuf.put(color.getRed());
            colorBuf.put(color.getGreen());
            colorBuf.put(color.getBlue());
            colorBuf.put(color.getAlpha());
        }
        colorBuf.flip();
    }

    /**
     * Sets every color of this geometry's color array to a random color.
     */
    public void setRandomColors() {
        FloatBuffer colorBuf = _meshData.getColorBuffer();
        if (colorBuf == null) {
            colorBuf = BufferUtils.createColorBuffer(_meshData.getVertexCount());
            _meshData.setColorBuffer(colorBuf);
        }

        for (int x = 0, cLength = colorBuf.limit(); x < cLength; x += 4) {
            colorBuf.put(MathUtils.nextRandomFloat());
            colorBuf.put(MathUtils.nextRandomFloat());
            colorBuf.put(MathUtils.nextRandomFloat());
            colorBuf.put(1);
        }
        colorBuf.flip();
    }

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(_meshData, "meshData", null);
        capsule.write(_castsShadows, "castsShadows", true);
        capsule.write(_modelBound, "modelBound", null);
        capsule.write(_defaultColor, "defaultColor", new ColorRGBA(ColorRGBA.WHITE));
    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);
        _meshData = (MeshData) capsule.readSavable("meshData", null);
        _castsShadows = capsule.readBoolean("castsShadows", true);
        _modelBound = (BoundingVolume) capsule.readSavable("modelBound", null);
        _defaultColor = (ColorRGBA) capsule.readSavable("defaultColor", new ColorRGBA(ColorRGBA.WHITE));
    }

}
