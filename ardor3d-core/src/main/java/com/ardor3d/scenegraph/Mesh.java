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
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.renderer.state.LightUtil;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.event.DirtyType;
import com.ardor3d.util.Debug;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.stat.StatCollector;
import com.ardor3d.util.stat.StatType;

/**
 * Mesh
 */
public class Mesh extends Spatial implements Renderable {

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

    /** The mesh's VBO information. */
    protected transient VBOInfo _vboInfo;

    protected ColorRGBA _defaultColor = new ColorRGBA(ColorRGBA.WHITE);

    protected boolean _castsShadows = true;

    /**
     * Non -1 values signal that drawing this scene should use the provided display list instead of drawing from the
     * buffers.
     */
    protected int _displayListID = -1;

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
        renderer.applyStates(_states);

        final boolean transformed = renderer.doTransforms(_worldTransform);

        if (getDisplayListID() != -1) {
            renderer.renderDisplayList(getDisplayListID());
        } else {
            if (_meshData.getInterleavedBuffer() != null) {
                renderer.setupInterleavedData(_meshData.getInterleavedBuffer(), _meshData.getInterleavedFormat(),
                        _vboInfo);
            } else {
                renderer.setupVertexData(_meshData.getVertexBuffer(), _vboInfo);
                renderer.setupNormalData(_meshData.getNormalBuffer(), getNormalsMode(), _worldTransform, _vboInfo);
                renderer.setupColorData(_meshData.getColorBuffer(), _vboInfo, _defaultColor);
                renderer.setupTextureData(_meshData.getTextureCoords(), _vboInfo);
            }

            if (_meshData.getIndexBuffer() != null) {
                renderer.drawElements(_meshData.getIndexBuffer(), _vboInfo, _meshData.getIndexLengths(), _meshData
                        .getIndexModes());
            } else {
                renderer
                        .drawArrays(_meshData.getVertexBuffer(), _meshData.getIndexLengths(), _meshData.getIndexModes());
            }
            if (Debug.stats) {
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

    public int getDisplayListID() {
        return _displayListID;
    }

    public void setDisplayListID(final int displayListID) {
        _displayListID = displayListID;
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
            final TexCoords coords) {

        _meshData.setVertexBuffer(vertices);
        _meshData.setNormalBuffer(normals);
        _meshData.setColorBuffer(colors);
        _meshData.setTextureCoords(coords, 0);

        if (getVBOInfo() != null) {
            resizeTextureIds(1);
        }
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
            final TexCoords coords, final IntBuffer indices) {

        reconstruct(vertices, normals, colors, coords);
        _meshData.setIndexBuffer(indices);
    }

    public void resizeTextureIds(final int i) {
        _vboInfo.resizeTextureIds(i);
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

        r.draw((Renderable) this);
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
     * Sets VBO info on this Geometry.
     * 
     * @param info
     *            the VBO info to set
     * @see VBOInfo
     */
    public void setVBOInfo(final VBOInfo info) {
        _vboInfo = info;
        if (_vboInfo != null) {
            _vboInfo.resizeTextureIds(_meshData.getTextureCoords().size());
        }
    }

    /**
     * @return VBO info object
     * @see VBOInfo
     */
    public VBOInfo getVBOInfo() {
        return _vboInfo;
    }

    public RenderState _getWorldRenderState(final StateType type) {
        return _states.get(type);
    }

    public EnumMap<StateType, RenderState> _getWorldRenderStates() {
        return _states;
    }

    public RenderState _setWorldRenderState(final RenderState state) {
        return _states.put(state.getType(), state);
    }

    public RenderState _clearWorldRenderState(final StateType type) {
        return _states.remove(type);
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
        capsule.write(_vboInfo, "vboInfo", null);
    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);
        _meshData = (MeshData) capsule.readSavable("meshData", null);
        _castsShadows = capsule.readBoolean("castsShadows", true);
        _modelBound = (BoundingVolume) capsule.readSavable("modelBound", null);
        _defaultColor = (ColorRGBA) capsule.readSavable("defaultColor", new ColorRGBA(ColorRGBA.WHITE));
        _vboInfo = (VBOInfo) capsule.readSavable("vboInfo", null);
    }

}
