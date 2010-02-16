/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.geom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.FloatBufferDataUtil;
import com.ardor3d.scenegraph.Mesh;

/**
 * Note: Does not work with geometry using texcoords other than 2d coords.
 */
public abstract class GeometryTool {
    private static final Logger logger = Logger.getLogger(GeometryTool.class.getName());

    public static final int MV_SAME_NORMALS = 1;
    public static final int MV_SAME_TEXS = 2;
    public static final int MV_SAME_COLORS = 4;

    @SuppressWarnings("unchecked")
    public static VertMap minimizeVerts(final Mesh mesh, final int options) {
        int vertCount = -1;
        final int oldCount = mesh.getMeshData().getVertexCount();
        int newCount = 0;

        final VertMap result = new VertMap(mesh);

        while (vertCount != newCount) {
            vertCount = mesh.getMeshData().getVertexCount();
            // go through each vert...
            final Vector3[] verts = BufferUtils.getVector3Array(mesh.getMeshData().getVertexBuffer());
            Vector3[] norms = null;
            if (mesh.getMeshData().getNormalBuffer() != null) {
                norms = BufferUtils.getVector3Array(mesh.getMeshData().getNormalBuffer());
            }

            ColorRGBA[] colors = null;
            if (mesh.getMeshData().getColorBuffer() != null) {
                colors = BufferUtils.getColorArray(mesh.getMeshData().getColorBuffer());
            }

            final Vector2[][] tex = new Vector2[mesh.getMeshData().getNumberOfUnits()][];
            for (int x = 0; x < tex.length; x++) {
                if (mesh.getMeshData().getTextureCoords(x) != null) {
                    tex[x] = BufferUtils.getVector2Array(mesh.getMeshData().getTextureCoords(x).getBuffer());
                }
            }

            final int[] inds = BufferUtils.getIntArray(mesh.getMeshData().getIndices());

            final HashMap<VertKey, Integer> store = new HashMap<VertKey, Integer>();
            int good = 0;
            for (int x = 0, max = verts.length; x < max; x++) {
                final VertKey vkey = new VertKey(verts[x], norms != null ? norms[x] : null, colors != null ? colors[x]
                        : null, getTexs(tex, x), options);
                // if we've already seen it, mark it for deletion and repoint
                // the corresponding index
                if (store.containsKey(vkey)) {
                    final int newInd = store.get(vkey);
                    result.replaceIndex(x, newInd);
                    findReplace(x, newInd, inds);
                    verts[x] = null;
                    if (norms != null) {
                        norms[newInd].addLocal(norms[x].normalizeLocal());
                    }
                    if (colors != null) {
                        colors[x] = null;
                    }
                } else {
                    store.put(vkey, x);
                    good++;
                }
            }

            final List<Vector3> newVects = new ArrayList<Vector3>(good);
            final List<Vector3> newNorms = new ArrayList<Vector3>(good);
            final List<ColorRGBA> newColors = new ArrayList<ColorRGBA>(good);
            final List[] newTexs = new ArrayList[mesh.getMeshData().getNumberOfUnits()];
            for (int x = 0; x < newTexs.length; x++) {
                if (mesh.getMeshData().getTextureCoords(x) != null) {
                    newTexs[x] = new ArrayList<Vector2>(good);
                }
            }

            // go through each vert
            // add non-duped verts, texs, normals to new buffers
            // and set into mesh.
            int off = 0;
            for (int x = 0, max = verts.length; x < max; x++) {
                if (verts[x] == null) {
                    // shift indices above this down a notch.
                    decrementIndices(x - off, inds);
                    result.decrementIndices(x - off);
                    off++;
                } else {
                    newVects.add(verts[x]);
                    if (norms != null) {
                        newNorms.add(norms[x].normalizeLocal());
                    }
                    if (colors != null) {
                        newColors.add(colors[x]);
                    }
                    for (int y = 0; y < newTexs.length; y++) {
                        if (mesh.getMeshData().getTextureCoords(y) != null) {
                            newTexs[y].add(tex[y][x]);
                        }
                    }
                }
            }

            mesh.getMeshData().setVertexBuffer(BufferUtils.createFloatBuffer(newVects.toArray(new Vector3[0])));
            if (norms != null) {
                mesh.getMeshData().setNormalBuffer(BufferUtils.createFloatBuffer(newNorms.toArray(new Vector3[0])));
            }
            if (colors != null) {
                mesh.getMeshData().setColorBuffer(BufferUtils.createFloatBuffer(newColors.toArray(new ColorRGBA[0])));
            }

            for (int x = 0; x < newTexs.length; x++) {
                if (mesh.getMeshData().getTextureCoords(x) != null) {
                    mesh.getMeshData().setTextureCoords(
                            FloatBufferDataUtil.makeNew((Vector2[]) newTexs[x].toArray(new Vector2[0])), x);
                }
            }

            mesh.getMeshData().getIndexBuffer().clear();
            for (final int i : inds) {
                mesh.getMeshData().getIndices().put(i);
            }
            newCount = mesh.getMeshData().getVertexCount();
        }
        logger.info("mesh: " + mesh + " old: " + oldCount + " new: " + newCount);

        return result;
    }

    private static Vector2[] getTexs(final Vector2[][] tex, final int i) {
        final Vector2[] res = new Vector2[tex.length];
        for (int x = 0; x < tex.length; x++) {
            if (tex[x] != null) {
                res[x] = tex[x][i];
            }
        }
        return res;
    }

    private static void findReplace(final int oldI, final int newI, final int[] indices) {
        for (int x = indices.length; --x >= 0;) {
            if (indices[x] == oldI) {
                indices[x] = newI;
            }
        }
    }

    private static void decrementIndices(final int above, final int[] inds) {
        for (int x = inds.length; --x >= 0;) {
            if (inds[x] >= above) {
                inds[x]--;
            }
        }
    }

}
