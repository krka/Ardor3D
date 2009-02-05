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
import java.io.Serializable;

import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * <code>VBOInfo</code> provides a single class for dealing with the VBO characteristics of a Mesh object(s)
 */
public class VBOInfo implements Serializable, Savable {
    private static final long serialVersionUID = 1L;

    private boolean _useVBOVertex = false;
    private boolean _useVBOTexture = false;
    private boolean _useVBOColor = false;
    private boolean _useVBONormal = false;
    private boolean _useVBOFogCoords = false;
    private boolean _useVBOIndex = false;
    private int _vboVertexID = -1;
    private int _vboColorID = -1;
    private int _vboNormalID = -1;
    private int _vboFogCoordsID = -1;
    private int[] _vboTextureIDs = null;
    private int _vboIndexID = -1;

    public VBOInfo() {
        this(false);
    }

    /**
     * Create a VBOInfo object that sets VBO to enabled or disabled for all types except Index, which is always disabled
     * unless set with setVBOIndexEnabled(true)
     * 
     * @param defaultVBO
     *            true for enabled, false for disabled.
     */
    public VBOInfo(final boolean defaultVBO) {
        _useVBOColor = defaultVBO;
        _useVBOTexture = defaultVBO;
        _useVBOVertex = defaultVBO;
        _useVBONormal = defaultVBO;
        _useVBOFogCoords = defaultVBO;
        _useVBOIndex = false;

        _vboTextureIDs = new int[2];
    }

    /**
     * Creates a copy of this VBOInfo. Does not copy any IDs.
     * 
     * @return a copy of this VBOInfo instance
     */
    public VBOInfo copy() {
        final VBOInfo copy = new VBOInfo();
        copy._useVBOVertex = _useVBOVertex;
        copy._useVBOTexture = _useVBOTexture;
        copy._useVBOColor = _useVBOColor;
        copy._useVBONormal = _useVBONormal;
        copy._useVBOIndex = _useVBOIndex;
        copy._useVBOFogCoords = _useVBOFogCoords;
        return copy;
    }

    /**
     * <code>resizeTextureIds</code> forces the texid array to be the given size, maintaining any old id values that can
     * fit in the new sized array. size of 0 is ignored.
     * 
     * @param size
     *            new size of texcoord id array
     */
    public void resizeTextureIds(final int size) {
        if (_vboTextureIDs.length == size || size == 0) {
            return;
        }

        final int[] newIDArray = new int[size];
        for (int x = Math.min(size, _vboTextureIDs.length); --x >= 0;) {
            newIDArray[x] = _vboTextureIDs[x];
        }

        _vboTextureIDs = newIDArray;
    }

    /**
     * Returns true if VBO (Vertex Buffer) is enabled for vertex information. This is used during rendering.
     * 
     * @return If VBO is enabled for vertexes.
     */
    public boolean isVBOVertexEnabled() {
        return _useVBOVertex;
    }

    /**
     * Returns true if VBO (Vertex Buffer) is enabled for fog coords information. This is used during rendering.
     * 
     * @return If VBO is enabled for fog coords.
     */
    public boolean isVBOFogCoordsEnabled() {
        return _useVBOFogCoords;
    }

    /**
     * Returns true if VBO (Vertex Buffer) is enabled for indices information. This is used during rendering.
     * 
     * @return If VBO is enabled for indices.
     */
    public boolean isVBOIndexEnabled() {
        return _useVBOIndex;
    }

    /**
     * Returns true if VBO (Vertex Buffer) is enabled for texture information. This is used during rendering.
     * 
     * @return If VBO is enabled for textures.
     */
    public boolean isVBOTextureEnabled() {
        return _useVBOTexture;
    }

    /**
     * Returns true if VBO (Vertex Buffer) is enabled for normal information. This is used during rendering.
     * 
     * @return If VBO is enabled for normals.
     */
    public boolean isVBONormalEnabled() {
        return _useVBONormal;
    }

    /**
     * Returns true if VBO (Vertex Buffer) is enabled for color information. This is used during rendering.
     * 
     * @return If VBO is enabled for colors.
     */
    public boolean isVBOColorEnabled() {
        return _useVBOColor;
    }

    /**
     * Enables or disables Vertex Buffer Objects for vertex information.
     * 
     * @param enabled
     *            If true, VBO enabled for vertexes.
     */
    public void setVBOVertexEnabled(final boolean enabled) {
        _useVBOVertex = enabled;
    }

    /**
     * Enables or disables Vertex Buffer Objects for fog coords information.
     * 
     * @param enabled
     *            If true, VBO enabled for fog coords.
     */
    public void setVBOFogCoordsEnabled(final boolean enabled) {
        _useVBOFogCoords = enabled;
    }

    /**
     * Enables or disables Vertex Buffer Objects for indices information.
     * 
     * @param enabled
     *            If true, VBO enabled for indices.
     */
    public void setVBOIndexEnabled(final boolean enabled) {
        _useVBOIndex = enabled;
    }

    /**
     * Enables or disables Vertex Buffer Objects for texture coordinate information.
     * 
     * @param enabled
     *            If true, VBO enabled for texture coordinates.
     */
    public void setVBOTextureEnabled(final boolean enabled) {
        _useVBOTexture = enabled;
    }

    /**
     * Enables or disables Vertex Buffer Objects for normal information.
     * 
     * @param enabled
     *            If true, VBO enabled for normals
     */
    public void setVBONormalEnabled(final boolean enabled) {
        _useVBONormal = enabled;
    }

    /**
     * Enables or disables Vertex Buffer Objects for color information.
     * 
     * @param enabled
     *            If true, VBO enabled for colors
     */
    public void setVBOColorEnabled(final boolean enabled) {
        _useVBOColor = enabled;
    }

    public int getVBOVertexID() {
        return _vboVertexID;
    }

    public int getVBOTextureID(final int index) {
        if (index >= _vboTextureIDs.length) {
            return -1;
        }
        return _vboTextureIDs[index];
    }

    public int getVBONormalID() {
        return _vboNormalID;
    }

    public int getVBOFogCoordsID() {
        return _vboFogCoordsID;
    }

    public int getVBOColorID() {
        return _vboColorID;
    }

    public void setVBOVertexID(final int id) {
        _vboVertexID = id;
    }

    public void setVBOTextureID(final int index, final int id) {
        if (index >= _vboTextureIDs.length) {
            resizeTextureIds(index + 1);
        }
        _vboTextureIDs[index] = id;
    }

    public void setVBONormalID(final int id) {
        _vboNormalID = id;
    }

    public void setVBOFogCoordsID(final int id) {
        _vboFogCoordsID = id;
    }

    public void setVBOColorID(final int id) {
        _vboColorID = id;
    }

    public int getVBOIndexID() {
        return _vboIndexID;
    }

    public void setVBOIndexID(final int id) {
        _vboIndexID = id;
    }

    public void write(final Ardor3DExporter e) throws IOException {
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(_useVBOVertex, "useVBOVertex", false);
        capsule.write(_useVBOTexture, "useVBOTexture", false);
        capsule.write(_useVBOColor, "useVBOColor", false);
        capsule.write(_useVBONormal, "useVBONormal", false);
        capsule.write(_useVBOFogCoords, "useVBOFogCoords", false);
        capsule.write(_useVBOIndex, "useVBOIndex", false);
    }

    public void read(final Ardor3DImporter e) throws IOException {
        final InputCapsule capsule = e.getCapsule(this);
        _useVBOVertex = capsule.readBoolean("useVBOVertex", false);
        _useVBOTexture = capsule.readBoolean("useVBOTexture", false);
        _useVBOColor = capsule.readBoolean("useVBOColor", false);
        _useVBONormal = capsule.readBoolean("useVBONormal", false);
        _useVBOFogCoords = capsule.readBoolean("useVBOFogCoords", false);
        _useVBOIndex = capsule.readBoolean("useVBOIndex", false);
    }

    public Class<? extends VBOInfo> getClassTag() {
        return this.getClass();
    }
}
