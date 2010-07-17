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

import java.nio.FloatBuffer;
import java.util.List;

import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.IndexBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.visitor.Visitor;
import com.google.common.collect.Lists;

/**
 * Utility for combining multiple Meshes into a single Mesh. Note that you generally will want to combine Mesh objects
 * that have the same render states.
 */
public class MeshCombiner {
    public static final float[] DEFAULT_COLOR = { 1f, 1f, 1f, 1f };
    public static final float[] DEFAULT_NORMAL = { 0f, 1f, 0f };
    public static final float[] DEFAULT_TEXCOORD = { 0 };

    /**
     * <p>
     * Combine all mesh objects that fall under the scene graph the given source node. All Mesh objects must have
     * vertices and texcoords that have the same tuple width. It is possible to merge Mesh objects together that have
     * mismatched normals/colors/etc. (eg. one with colors and one without.)
     * </p>
     * 
     * @param source
     *            our source node
     * @return the combined Mesh.
     */
    public final static Mesh combine(final Node source) {
        final List<Mesh> sources = Lists.newArrayList();
        source.acceptVisitor(new Visitor() {
            @Override
            public void visit(final Spatial spatial) {
                if (spatial instanceof Mesh) {
                    sources.add((Mesh) spatial);
                }
            }
        }, true);

        return combine(sources);
    }

    /**
     * Combine the given List of Mesh objects into a single Mesh. All Mesh objects must have vertices and texcoords that
     * have the same tuple width. It is possible to merge Mesh objects together that have mismatched normals/colors/etc.
     * (eg. one with colors and one without.)
     * 
     * @param sources
     *            our list of Mesh objects to combine.
     * @return the combined Mesh.
     */
    public final static Mesh combine(final List<Mesh> sources) {
        if (sources == null || sources.isEmpty()) {
            return null;
        }

        // go through each MeshData to see what buffers we need and validate sizes.
        boolean useIndices = false, useNormals = false, useTextures = false, useColors = false;
        int maxTextures = 0, totalVertices = 0, totalIndices = 0, texCoords = 0;
        final int vertCoords = sources.get(0).getMeshData().getVertexCoords().getValuesPerTuple();
        MeshData md;
        for (final Mesh mesh : sources) {
            md = mesh.getMeshData();
            if (vertCoords != md.getVertexCoords().getValuesPerTuple()) {
                throw new IllegalArgumentException("all MeshData vertex coords must use same tuple size.");
            }

            // update total vertices
            totalVertices += md.getVertexCount();

            // check for indices
            if (useIndices || md.getIndexBuffer() != null) {
                useIndices = true;
                if (md.getIndexBuffer() != null) {
                    totalIndices += md.getIndices().capacity();
                } else {
                    totalIndices += md.getVertexCount();
                }
            }

            // check for normals
            if (!useNormals && md.getNormalBuffer() != null) {
                useNormals = true;
            }

            // check for colors
            if (!useColors && md.getColorBuffer() != null) {
                useColors = true;
            }

            // check for texcoord usage
            if (md.getNumberOfUnits() > 0) {
                if (!useTextures) {
                    useTextures = true;
                    texCoords = md.getTextureCoords(0).getValuesPerTuple();
                } else if (texCoords != md.getTextureCoords(0).getValuesPerTuple()) {
                    throw new IllegalArgumentException("all MeshData objects with texcoords must use same tuple size.");
                }
                maxTextures = Math.max(maxTextures, md.getNumberOfUnits());
            }
        }

        // Instantiate the return Mesh and MeshData
        final Mesh result = new Mesh("combined");
        final MeshData data = new MeshData();
        result.setMeshData(data);

        // Generate our buffers based on the information collected above and populate MeshData
        final FloatBufferData vertices = new FloatBufferData(totalVertices * vertCoords, vertCoords);
        data.setVertexCoords(vertices);

        final FloatBufferData colors = useColors ? new FloatBufferData(totalVertices * 4, 4) : null;
        data.setColorCoords(colors);

        final FloatBufferData normals = useNormals ? new FloatBufferData(totalVertices * 3, 3) : null;
        data.setNormalCoords(normals);

        final List<FloatBufferData> texCoordsList = Lists.newArrayListWithCapacity(maxTextures);
        for (int i = 0; i < maxTextures; i++) {
            texCoordsList.add(new FloatBufferData(totalVertices * texCoords, texCoords));
        }
        data.setTextureCoords(useTextures ? texCoordsList : null);

        final IndexBufferData<?> indices = useIndices ? BufferUtils.createIndexBufferData(totalIndices,
                totalVertices - 1) : null;
        data.setIndices(indices);

        // Walk through our source meshes and populate return MeshData buffers.
        int vertexOffset = 0;
        for (final Mesh mesh : sources) {
            md = mesh.getMeshData();

            // Vertices
            md.getVertexBuffer().rewind();
            vertices.getBuffer().put(mesh.getWorldVectors(null));

            // Normals
            if (useNormals) {
                final FloatBuffer nb = md.getNormalBuffer();
                if (nb != null) {
                    nb.rewind();
                    normals.getBuffer().put(mesh.getWorldNormals(null));
                } else {
                    for (int i = 0; i < md.getVertexCount(); i++) {
                        normals.getBuffer().put(DEFAULT_NORMAL);
                    }
                }
            }

            // Colors
            if (useColors) {
                final FloatBuffer cb = md.getColorBuffer();
                if (cb != null) {
                    cb.rewind();
                    colors.getBuffer().put(cb);
                } else {
                    for (int i = 0; i < md.getVertexCount(); i++) {
                        colors.getBuffer().put(DEFAULT_COLOR);
                    }
                }
            }

            // Tex Coords
            if (useTextures) {
                for (int i = 0; i < maxTextures; i++) {
                    final FloatBuffer dest = texCoordsList.get(i).getBuffer();
                    final FloatBuffer tb = md.getTextureBuffer(i);
                    if (tb != null) {
                        tb.rewind();
                        dest.put(tb);
                    } else {
                        for (int j = 0; j < md.getVertexCount() * texCoords; j++) {
                            dest.put(DEFAULT_TEXCOORD);
                        }
                    }
                }
            }

            // Indices
            if (useIndices) {
                final IndexBufferData<?> ib = md.getIndices();
                if (ib != null) {
                    ib.rewind();
                    for (int i = 0, max = ib.capacity(); i < max; i++) {
                        indices.put(ib.get(i) + vertexOffset);
                    }
                } else {
                    for (int i = 0; i < md.getVertexCount(); i++) {
                        indices.put(i + vertexOffset);
                    }
                }
                vertexOffset += md.getVertexCount();
            }
        }

        // set our bounding volume using the volume type of our first source.
        result.setModelBound(sources.get(0).getModelBound(null));

        // return our mesh
        return result;
    }
}