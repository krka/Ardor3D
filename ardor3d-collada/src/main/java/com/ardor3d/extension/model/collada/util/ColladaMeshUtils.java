/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.util;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.logging.Logger;

import com.ardor3d.extension.model.collada.binding.core.Collada;
import com.ardor3d.extension.model.collada.binding.core.DaeGeometry;
import com.ardor3d.extension.model.collada.binding.core.DaeInputShared;
import com.ardor3d.extension.model.collada.binding.core.DaeMesh;
import com.ardor3d.extension.model.collada.binding.core.DaePolygons;
import com.ardor3d.extension.model.collada.binding.core.DaePolylist;
import com.ardor3d.extension.model.collada.binding.core.DaeSimpleIntegerArray;
import com.ardor3d.extension.model.collada.binding.core.DaeSource;
import com.ardor3d.extension.model.collada.binding.core.DaeTriangles;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Point;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.geom.BufferUtils;

public class ColladaMeshUtils {
    private static final Logger logger = Logger.getLogger(ColladaMeshUtils.class.getName());

    public static Spatial getGeometryMesh(final String id, final Collada collada) {
        final DaeGeometry geometry = (DaeGeometry) Collada.findLibraryEntry(id, collada.getLibraryGeometries());

        if (geometry != null && geometry.getMesh() != null) {
            return buildMesh(geometry);
        }
        return null;
    }

    public static Node buildMesh(final DaeGeometry colladaGeometry) {
        if (colladaGeometry.getMesh() != null) {
            final DaeMesh cMesh = colladaGeometry.getMesh();
            final Node meshNode = new Node(colladaGeometry.getName());

            // Grab all mesh types (polygons, triangles, etc.)
            // Create each as an Ardor3D Mesh, and attach to node
            boolean hasChild = false;
            if (cMesh.getPolygons() != null) {
                for (final DaePolygons p : cMesh.getPolygons()) {
                    final Spatial child = buildMesh(p);
                    if (child.getName() == null) {
                        child.setName(meshNode.getName() + "_polygons");
                    }
                    if (child != null) {
                        meshNode.attachChild(child);
                        hasChild = true;
                    }
                }
            }
            if (cMesh.getPolylist() != null) {
                for (final DaePolylist p : cMesh.getPolylist()) {
                    final Spatial child = buildMesh(p);
                    if (child.getName() == null) {
                        child.setName(meshNode.getName() + "_polylist");
                    }
                    if (child != null) {
                        meshNode.attachChild(child);
                        hasChild = true;
                    }
                }
            }
            if (cMesh.getTriangles() != null) {
                for (final DaeTriangles t : cMesh.getTriangles()) {
                    final Spatial child = buildMesh(t);
                    if (child.getName() == null) {
                        child.setName(meshNode.getName() + "_triangles");
                    }
                    if (child != null) {
                        meshNode.attachChild(child);
                        hasChild = true;
                    }
                }
            }
            if (cMesh.getLines() != null) {
                logger.warning("<line> not currently supported.");
                // TODO: Add support
            }
            if (cMesh.getLinestrips() != null) {
                logger.warning("<linestrip> not currently supported.");
                // TODO: Add support
            }
            if (cMesh.getTrifans() != null) {
                logger.warning("<trifan> not currently supported.");
                // TODO: Add support
            }
            if (cMesh.getTristrips() != null) {
                logger.warning("<tristrip> not currently supported.");
                // TODO: Add support
            }

            // If we did not find a valid child, the spec says to add verts as a "cloud of points"
            if (!hasChild) {
                final Point points = buildPoints(cMesh);
                if (points.getName() == null) {
                    points.setName(meshNode.getName() + "_points");
                }
                if (points != null) {
                    meshNode.attachChild(points);
                }
            }

            return meshNode;
        }
        return null;
    }

    private static Point buildPoints(final DaeMesh mesh) {
        if (mesh == null || mesh.getVertices() == null || mesh.getVertices().getInputs() == null
                || mesh.getVertices().getInputs().size() == 0) {
            return null;
        }
        final Point points = new Point();
        points.setName(mesh.getName());

        // Find POSITION vertices source
        final DaeSource source = ColladaInputPipe.getPositionSource(mesh.getVertices(), mesh.getRootNode());
        if (source == null) {
            return null;
        }

        if (source.getFloatArray() != null) {
            // Turn into Floatbuffer if we have float array data
            final FloatBuffer vertices = BufferUtils.createFloatBuffer(source.getFloatArray().getData());
            // Add to points
            points.getMeshData().setVertexBuffer(vertices);
        } else if (source.getIntArray() != null) {
            // Turn into Floatbuffer if we have int array data
            final int[] data = source.getIntArray().getData();
            final FloatBuffer vertices = BufferUtils.createFloatBuffer(data.length);
            for (final int i : data) {
                vertices.put(i);
            }
            // Add to points
            points.getMeshData().setVertexBuffer(vertices);
        }

        // Update bound
        points.updateModelBound();

        // return
        return points;
    }

    public static Mesh buildMesh(final DaePolygons polys) {
        if (polys == null || polys.getInputs() == null || polys.getInputs().size() == 0) {
            return null;
        }
        final Mesh polyMesh = new Mesh(polys.getName());
        polyMesh.getMeshData().setIndexMode(IndexMode.Triangles);

        // Build and set RenderStates for our material
        ColladaMaterialUtils.applyMaterial(polys.getMaterial(), polys.getRootNode(), polyMesh);

        // Pull inputs out... what is max offset? what values will we be using?
        int maxOffset = 0;
        final LinkedList<ColladaInputPipe> pipes = new LinkedList<ColladaInputPipe>();
        for (final DaeInputShared i : polys.getInputs()) {
            // Construct an input pipe...
            final ColladaInputPipe pipe = new ColladaInputPipe(i, polys.getRootNode());
            pipes.add(pipe);
            maxOffset = Math.max(maxOffset, i.getOffset());
        }
        final int interval = maxOffset + 1;

        // use interval & sum of sizes of p entries to determine buffer sizes.
        int numEntries = 0;
        int indices = 0;
        for (final DaeSimpleIntegerArray vals : polys.getPEntries()) {
            final int length = vals.getData().length;
            numEntries += length;
            indices += ((length / interval) - 2) * 3;
        }
        numEntries /= interval;

        // Construct nio buffers for specified inputs.
        for (final ColladaInputPipe pipe : pipes) {
            pipe.setupBuffer(numEntries, polyMesh.getMeshData());
        }

        // Prepare indices buffer
        final IntBuffer meshIndices = BufferUtils.createIntBuffer(indices);
        polyMesh.getMeshData().setIndexBuffer(meshIndices);

        // go through the polygon entries
        int firstIndex = 0;
        final int[] currentVal = new int[interval];
        for (final DaeSimpleIntegerArray dia : polys.getPEntries()) {
            // for each p, iterate using max offset
            final int[] vals = dia.getData();

            final int first = firstIndex + 0;
            System.arraycopy(vals, 0, currentVal, 0, interval);
            ColladaInputPipe.processPipes(pipes, currentVal);

            int prev = firstIndex + 1;
            System.arraycopy(vals, interval, currentVal, 0, interval);
            ColladaInputPipe.processPipes(pipes, currentVal);

            // first add the first two entries to the buffers.

            // Now go through remaining entries and create a polygon as a triangle fan.
            for (int j = 2, max = vals.length / interval; j < max; j++) {
                // add first as index
                meshIndices.put(first);
                // add prev as index
                meshIndices.put(prev);

                // set prev to current
                prev = firstIndex + j;
                // add current to buffers
                System.arraycopy(vals, j * interval, currentVal, 0, interval);
                ColladaInputPipe.processPipes(pipes, currentVal);
                // add current as index
                meshIndices.put(prev);
            }
            firstIndex += vals.length / interval;
        }

        // update bounds
        polyMesh.updateModelBound();

        // return
        return polyMesh;
    }

    public static Mesh buildMesh(final DaePolylist polys) {
        if (polys == null || polys.getInputs() == null || polys.getInputs().size() == 0) {
            return null;
        }
        final Mesh polyMesh = new Mesh(polys.getName());
        polyMesh.getMeshData().setIndexMode(IndexMode.Triangles);

        // Build and set RenderStates for our material
        ColladaMaterialUtils.applyMaterial(polys.getMaterial(), polys.getRootNode(), polyMesh);

        // Pull inputs out... what is max offset? what values will we be using?
        int maxOffset = 0;
        final LinkedList<ColladaInputPipe> pipes = new LinkedList<ColladaInputPipe>();
        for (final DaeInputShared i : polys.getInputs()) {
            // Construct an input pipe...
            final ColladaInputPipe pipe = new ColladaInputPipe(i, polys.getRootNode());
            pipes.add(pipe);
            maxOffset = Math.max(maxOffset, i.getOffset());
        }
        final int interval = maxOffset + 1;

        // use interval & sum of sizes of vcount to determine buffer sizes.
        int numEntries = 0;
        int indices = 0;
        for (final int length : polys.getVCount().getData()) {
            numEntries += length;
            indices += ((length) - 2) * 3;
        }

        // Construct nio buffers for specified inputs.
        for (final ColladaInputPipe pipe : pipes) {
            pipe.setupBuffer(numEntries, polyMesh.getMeshData());
        }

        // Prepare indices buffer
        final IntBuffer meshIndices = BufferUtils.createIntBuffer(indices);
        polyMesh.getMeshData().setIndexBuffer(meshIndices);

        // go through the polygon entries
        int firstIndex = 0;
        int offset = 0;
        final int[] vals = polys.getPEntry().getData();
        for (final int length : polys.getVCount().getData()) {
            final int[] currentVal = new int[interval];

            // first add the first two entries to the buffers.
            final int first = firstIndex + 0;
            System.arraycopy(vals, ((offset + 0) * interval), currentVal, 0, interval);
            ColladaInputPipe.processPipes(pipes, currentVal);

            int prev = firstIndex + 1;
            System.arraycopy(vals, ((offset + 1) * interval), currentVal, 0, interval);
            ColladaInputPipe.processPipes(pipes, currentVal);

            // Now go through remaining entries and create a polygon as a triangle fan.
            for (int j = 2, max = length; j < max; j++) {
                // add first as index
                meshIndices.put(first);
                // add prev as index
                meshIndices.put(prev);

                // set prev to current
                prev = firstIndex + j;
                // add current to buffers
                System.arraycopy(vals, ((offset + j) * interval), currentVal, 0, interval);
                ColladaInputPipe.processPipes(pipes, currentVal);
                // add current as index
                meshIndices.put(prev);
            }
            firstIndex += length;

            offset += length;
        }

        // update bounds
        polyMesh.updateModelBound();

        // return
        return polyMesh;
    }

    public static Mesh buildMesh(final DaeTriangles tris) {
        if (tris == null || tris.getInputs() == null || tris.getInputs().size() == 0 || tris.getPEntry() == null) {
            return null;
        }
        final Mesh triMesh = new Mesh(tris.getName());
        triMesh.getMeshData().setIndexMode(IndexMode.Triangles);

        // Build and set RenderStates for our material
        ColladaMaterialUtils.applyMaterial(tris.getMaterial(), tris.getRootNode(), triMesh);

        // Pull inputs out... what is max offset? what values will we be using?
        int maxOffset = 0;
        final LinkedList<ColladaInputPipe> pipes = new LinkedList<ColladaInputPipe>();
        for (final DaeInputShared i : tris.getInputs()) {
            // Construct an input pipe...
            final ColladaInputPipe pipe = new ColladaInputPipe(i, tris.getRootNode());
            pipes.add(pipe);
            maxOffset = Math.max(maxOffset, i.getOffset());
        }
        final int interval = maxOffset + 1;

        // use interval & size of p array to determine buffer sizes.
        final int numEntries = tris.getPEntry().getData().length / interval;

        // Construct nio buffers for specified inputs.
        for (final ColladaInputPipe pipe : pipes) {
            pipe.setupBuffer(numEntries, triMesh.getMeshData());
        }

        // go through the p entry
        int firstIndex = 0;
        // for each p, iterate using max offset
        final int[] vals = tris.getPEntry().getData();
        final int[] currentVal = new int[interval];

        // Go through entries and add to buffers.
        for (int j = 0, max = vals.length / interval; j < max; j++) {
            // add entry to buffers
            System.arraycopy(vals, j * interval, currentVal, 0, interval);
            ColladaInputPipe.processPipes(pipes, currentVal);
        }
        firstIndex += vals.length / interval;

        triMesh.updateModelBound();

        return triMesh;
    }
}
