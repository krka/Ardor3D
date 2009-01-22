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
    private boolean useVBOVertex = false;
    private boolean useVBOTexture = false;
    private boolean useVBOColor = false;
    private boolean useVBONormal = false;
    private boolean useVBOFogCoords = false;
    private boolean useVBOIndex = false;
    private int vboVertexID = -1;
    private int vboColorID = -1;
    private int vboNormalID = -1;
    private int vboFogCoordsID = -1;
    private int[] vboTextureIDs = null;
    private int vboIndexID = -1;

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
        useVBOColor = defaultVBO;
        useVBOTexture = defaultVBO;
        useVBOVertex = defaultVBO;
        useVBONormal = defaultVBO;
        useVBOFogCoords = defaultVBO;
        useVBOIndex = false;

        vboTextureIDs = new int[2];
    }

    /**
     * Creates a copy of this VBOInfo. Does not copy any IDs.
     * 
     * @return a copy of this VBOInfo instance
     */
    public VBOInfo copy() {
        final VBOInfo copy = new VBOInfo();
        copy.useVBOVertex = useVBOVertex;
        copy.useVBOTexture = useVBOTexture;
        copy.useVBOColor = useVBOColor;
        copy.useVBONormal = useVBONormal;
        copy.useVBOIndex = useVBOIndex;
        copy.useVBOFogCoords = useVBOFogCoords;
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
        if (vboTextureIDs.length == size || size == 0) {
            return;
        }

        final int[] newIDArray = new int[size];
        for (int x = Math.min(size, vboTextureIDs.length); --x >= 0;) {
            newIDArray[x] = vboTextureIDs[x];
        }

        vboTextureIDs = newIDArray;
    }

    /**
     * Returns true if VBO (Vertex Buffer) is enabled for vertex information. This is used during rendering.
     * 
     * @return If VBO is enabled for vertexes.
     */
    public boolean isVBOVertexEnabled() {
        return useVBOVertex;
    }

    /**
     * Returns true if VBO (Vertex Buffer) is enabled for fog coords information. This is used during rendering.
     * 
     * @return If VBO is enabled for fog coords.
     */
    public boolean isVBOFogCoordsEnabled() {
        return useVBOFogCoords;
    }

    /**
     * Returns true if VBO (Vertex Buffer) is enabled for indices information. This is used during rendering.
     * 
     * @return If VBO is enabled for indices.
     */
    public boolean isVBOIndexEnabled() {
        return useVBOIndex;
    }

    /**
     * Returns true if VBO (Vertex Buffer) is enabled for texture information. This is used during rendering.
     * 
     * @return If VBO is enabled for textures.
     */
    public boolean isVBOTextureEnabled() {
        return useVBOTexture;
    }

    /**
     * Returns true if VBO (Vertex Buffer) is enabled for normal information. This is used during rendering.
     * 
     * @return If VBO is enabled for normals.
     */
    public boolean isVBONormalEnabled() {
        return useVBONormal;
    }

    /**
     * Returns true if VBO (Vertex Buffer) is enabled for color information. This is used during rendering.
     * 
     * @return If VBO is enabled for colors.
     */
    public boolean isVBOColorEnabled() {
        return useVBOColor;
    }

    /**
     * Enables or disables Vertex Buffer Objects for vertex information.
     * 
     * @param enabled
     *            If true, VBO enabled for vertexes.
     */
    public void setVBOVertexEnabled(final boolean enabled) {
        useVBOVertex = enabled;
    }

    /**
     * Enables or disables Vertex Buffer Objects for fog coords information.
     * 
     * @param enabled
     *            If true, VBO enabled for fog coords.
     */
    public void setVBOFogCoordsEnabled(final boolean enabled) {
        useVBOFogCoords = enabled;
    }

    /**
     * Enables or disables Vertex Buffer Objects for indices information.
     * 
     * @param enabled
     *            If true, VBO enabled for indices.
     */
    public void setVBOIndexEnabled(final boolean enabled) {
        useVBOIndex = enabled;
    }

    /**
     * Enables or disables Vertex Buffer Objects for texture coordinate information.
     * 
     * @param enabled
     *            If true, VBO enabled for texture coordinates.
     */
    public void setVBOTextureEnabled(final boolean enabled) {
        useVBOTexture = enabled;
    }

    /**
     * Enables or disables Vertex Buffer Objects for normal information.
     * 
     * @param enabled
     *            If true, VBO enabled for normals
     */
    public void setVBONormalEnabled(final boolean enabled) {
        useVBONormal = enabled;
    }

    /**
     * Enables or disables Vertex Buffer Objects for color information.
     * 
     * @param enabled
     *            If true, VBO enabled for colors
     */
    public void setVBOColorEnabled(final boolean enabled) {
        useVBOColor = enabled;
    }

    public int getVBOVertexID() {
        return vboVertexID;
    }

    public int getVBOTextureID(final int index) {
        if (index >= vboTextureIDs.length) {
            return -1;
        }
        return vboTextureIDs[index];
    }

    public int getVBONormalID() {
        return vboNormalID;
    }

    public int getVBOFogCoordsID() {
        return vboFogCoordsID;
    }

    public int getVBOColorID() {
        return vboColorID;
    }

    public void setVBOVertexID(final int id) {
        vboVertexID = id;
    }

    public void setVBOTextureID(final int index, final int id) {
        if (index >= vboTextureIDs.length) {
            resizeTextureIds(index + 1);
        }
        vboTextureIDs[index] = id;
    }

    public void setVBONormalID(final int id) {
        vboNormalID = id;
    }

    public void setVBOFogCoordsID(final int id) {
        vboFogCoordsID = id;
    }

    public void setVBOColorID(final int id) {
        vboColorID = id;
    }

    public int getVBOIndexID() {
        return vboIndexID;
    }

    public void setVBOIndexID(final int id) {
        vboIndexID = id;
    }

    public void write(final Ardor3DExporter e) throws IOException {
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(useVBOVertex, "useVBOVertex", false);
        capsule.write(useVBOTexture, "useVBOTexture", false);
        capsule.write(useVBOColor, "useVBOColor", false);
        capsule.write(useVBONormal, "useVBONormal", false);
        capsule.write(useVBOFogCoords, "useVBOFogCoords", false);
        capsule.write(useVBOIndex, "useVBOIndex", false);
    }

    public void read(final Ardor3DImporter e) throws IOException {
        final InputCapsule capsule = e.getCapsule(this);
        useVBOVertex = capsule.readBoolean("useVBOVertex", false);
        useVBOTexture = capsule.readBoolean("useVBOTexture", false);
        useVBOColor = capsule.readBoolean("useVBOColor", false);
        useVBONormal = capsule.readBoolean("useVBONormal", false);
        useVBOFogCoords = capsule.readBoolean("useVBOFogCoords", false);
        useVBOIndex = capsule.readBoolean("useVBOIndex", false);
    }

    public Class<? extends VBOInfo> getClassTag() {
        return this.getClass();
    }
}
