/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.jdom;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.jdom.DataConversionException;
import org.jdom.Element;

import com.ardor3d.extension.model.collada.jdom.data.GlobalData;
import com.ardor3d.extension.model.collada.jdom.data.MeshVertPairs;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Point;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.geom.BufferUtils;

public class ColladaMeshUtils {
    private static final Logger logger = Logger.getLogger(ColladaMeshUtils.class.getName());

    /**
     * Builds geometry from an instance_geometry element.
     * 
     * @param instance_geometry
     * @return our Spatial
     */
    public static Spatial getGeometryMesh(final Element instance_geometry) {
        final Element geometry = ColladaDOMUtil.findTargetWithId(instance_geometry.getAttributeValue("url"));

        if (geometry != null) {
            return ColladaMeshUtils.buildMesh(geometry);
        }
        return null;
    }

    /**
     * Builds a mesh from a Collada geometry element. Currently supported mesh types: mesh, polygons, polylist,
     * triangles, lines. Not supported yet: linestrips, trifans, tristrips. If no meshtype is found, a pointcloud is
     * built.
     * 
     * @param colladaGeometry
     * @return a Node containing all of the Ardor3D meshes we've parsed from this geometry element.
     */
    @SuppressWarnings("unchecked")
    public static Node buildMesh(final Element colladaGeometry) {
        if (colladaGeometry.getChild("mesh") != null) {
            final Element cMesh = colladaGeometry.getChild("mesh");
            final Node meshNode = new Node(colladaGeometry.getAttributeValue("name", colladaGeometry.getName()));

            // Grab all mesh types (polygons, triangles, etc.)
            // Create each as an Ardor3D Mesh, and attach to node
            boolean hasChild = false;
            if (cMesh.getChild("polygons") != null) {
                for (final Element p : (List<Element>) cMesh.getChildren("polygons")) {
                    final Mesh child = ColladaMeshUtils.buildMeshPolygons(colladaGeometry, p);
                    if (child.getName() == null) {
                        child.setName(meshNode.getName() + "_polygons");
                    }
                    if (child != null) {
                        meshNode.attachChild(child);
                        hasChild = true;
                    }
                }
            }
            if (cMesh.getChild("polylist") != null) {
                for (final Element p : (List<Element>) cMesh.getChildren("polylist")) {
                    final Mesh child = ColladaMeshUtils.buildMeshPolylist(colladaGeometry, p);
                    if (child.getName() == null) {
                        child.setName(meshNode.getName() + "_polylist");
                    }
                    if (child != null) {
                        meshNode.attachChild(child);
                        hasChild = true;
                    }
                }
            }
            if (cMesh.getChild("triangles") != null) {
                for (final Element t : (List<Element>) cMesh.getChildren("triangles")) {
                    final Mesh child = ColladaMeshUtils.buildMeshTriangles(colladaGeometry, t);
                    if (child.getName() == null) {
                        child.setName(meshNode.getName() + "_triangles");
                    }
                    if (child != null) {
                        meshNode.attachChild(child);
                        hasChild = true;
                    }
                }
            }
            if (cMesh.getChild("lines") != null) {
                for (final Element l : (List<Element>) cMesh.getChildren("lines")) {
                    final Line child = ColladaMeshUtils.buildMeshLines(colladaGeometry, l);
                    if (child.getName() == null) {
                        child.setName(meshNode.getName() + "_lines");
                    }
                    if (child != null) {
                        meshNode.attachChild(child);
                        hasChild = true;
                    }
                }
            }
            if (cMesh.getChild("linestrips") != null) {
                ColladaMeshUtils.logger.warning("<linestrips> not currently supported.");
                hasChild = true;
                // TODO: Add support
            }
            if (cMesh.getChild("trifans") != null) {
                ColladaMeshUtils.logger.warning("<trifan> not currently supported.");
                hasChild = true;
                // TODO: Add support
            }
            if (cMesh.getChild("tristrips") != null) {
                ColladaMeshUtils.logger.warning("<tristrip> not currently supported.");
                hasChild = true;
                // TODO: Add support
            }

            // If we did not find a valid child, the spec says to add verts as a "cloud of points"
            if (!hasChild) {
                ColladaMeshUtils.logger.warning("No valid child found, creating 'cloud of points'");
                final Point points = ColladaMeshUtils.buildPoints(colladaGeometry, cMesh);
                if (points != null) {
                    if (points.getName() == null) {
                        points.setName(meshNode.getName() + "_points");
                    }
                    meshNode.attachChild(points);
                }
            }

            return meshNode;
        }
        return null;
    }

    private static Point buildPoints(final Element colladaGeometry, final Element mesh) {
        if (mesh == null || mesh.getChild("vertices") == null || mesh.getChild("vertices").getChild("input") == null) {
            return null;
        }
        final Point points = new Point();
        points.setName(mesh.getAttributeValue("name", mesh.getName()));

        // Find POSITION vertices source
        final Element source = ColladaInputPipe.getPositionSource(mesh.getChild("vertices"));
        if (source == null) {
            return null;
        }

        if (source.getChild("float_array") != null) {
            // Turn into Floatbuffer if we have float array data
            final Element floatArray = source.getChild("float_array");
            if ("0".equals(floatArray.getAttributeValue("count"))) {
                return null;
            }
            final FloatBuffer vertices = BufferUtils.createFloatBuffer(ColladaDOMUtil.parseFloatArray(floatArray));
            // Add to points
            points.getMeshData().setVertexBuffer(vertices);
        } else if (source.getChild("int_array") != null) {
            // Turn into Floatbuffer if we have int array data
            final Element intArray = source.getChild("int_array");
            if ("0".equals(intArray.getAttributeValue("count"))) {
                return null;
            }
            final int[] data = ColladaDOMUtil.parseIntArray(intArray);
            final FloatBuffer vertices = BufferUtils.createFloatBuffer(data.length);
            for (final int i : data) {
                vertices.put(i);
            }
            // Add to points
            points.getMeshData().setVertexBuffer(vertices);
        }

        // Add to vert mapping
        final int[] indices = new int[points.getMeshData().getVertexCount()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }
        final MeshVertPairs mvp = new MeshVertPairs(points, indices);
        GlobalData.getInstance().getVertMappings().put(mesh, mvp);

        // Update bound
        points.updateModelBound();

        // return
        return points;
    }

    @SuppressWarnings("unchecked")
    public static Mesh buildMeshPolygons(final Element colladaGeometry, final Element polys) {
        if (polys == null || polys.getChild("input") == null) {
            return null;
        }
        final Mesh polyMesh = new Mesh(polys.getAttributeValue("name"));
        polyMesh.getMeshData().setIndexMode(IndexMode.Triangles);

        // Build and set RenderStates for our material
        ColladaMaterialUtils.applyMaterial(polys.getAttributeValue("material"), polyMesh);

        // Pull inputs out... what is max offset? what values will we be using?
        int maxOffset = 0;
        final LinkedList<ColladaInputPipe> pipes = new LinkedList<ColladaInputPipe>();
        for (final Element i : (List<Element>) polys.getChildren("input")) {
            // Construct an input pipe...
            final ColladaInputPipe pipe = new ColladaInputPipe(i);
            pipes.add(pipe);
            try {
                maxOffset = Math.max(maxOffset, i.getAttribute("offset").getIntValue());
            } catch (final DataConversionException e) {
                e.printStackTrace();
            }
        }
        final int interval = maxOffset + 1;

        // use interval & sum of sizes of p entries to determine buffer sizes.
        int numEntries = 0;
        int numIndices = 0;
        for (final Element vals : (List<Element>) polys.getChildren("p")) {
            final int length = ColladaDOMUtil.parseIntArray(vals).length;
            numEntries += length;
            numIndices += (length / interval - 2) * 3;
        }
        numEntries /= interval;

        // Construct nio buffers for specified inputs.
        for (final ColladaInputPipe pipe : pipes) {
            pipe.setupBuffer(numEntries, polyMesh.getMeshData());
        }

        // Add to vert mapping
        final int[] indices = new int[numEntries];
        final MeshVertPairs mvp = new MeshVertPairs(polyMesh, indices);
        GlobalData.getInstance().getVertMappings().put(colladaGeometry, mvp);

        // Prepare indices buffer
        final IntBuffer meshIndices = BufferUtils.createIntBuffer(numIndices);
        polyMesh.getMeshData().setIndexBuffer(meshIndices);

        // go through the polygon entries
        int firstIndex = 0, vecIndex;
        final int[] currentVal = new int[interval];
        for (final Element dia : (List<Element>) polys.getChildren("p")) {
            // for each p, iterate using max offset
            final int[] vals = ColladaDOMUtil.parseIntArray(dia);

            final int first = firstIndex + 0;
            System.arraycopy(vals, 0, currentVal, 0, interval);
            vecIndex = ColladaInputPipe.processPipes(pipes, currentVal);
            if (vecIndex != Integer.MIN_VALUE) {
                indices[firstIndex + 0] = vecIndex;
            }

            int prev = firstIndex + 1;
            System.arraycopy(vals, interval, currentVal, 0, interval);
            vecIndex = ColladaInputPipe.processPipes(pipes, currentVal);
            if (vecIndex != Integer.MIN_VALUE) {
                indices[firstIndex + 1] = vecIndex;
            }

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
                vecIndex = ColladaInputPipe.processPipes(pipes, currentVal);
                if (vecIndex != Integer.MIN_VALUE) {
                    indices[firstIndex + j] = vecIndex;
                }
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

    @SuppressWarnings("unchecked")
    public static Mesh buildMeshPolylist(final Element colladaGeometry, final Element polys) {
        if (polys == null || polys.getChild("input") == null) {
            return null;
        }
        final Mesh polyMesh = new Mesh(polys.getAttributeValue("name"));
        polyMesh.getMeshData().setIndexMode(IndexMode.Triangles);

        // Build and set RenderStates for our material
        ColladaMaterialUtils.applyMaterial(polys.getAttributeValue("material"), polyMesh);

        // Pull inputs out... what is max offset? what values will we be using?
        int maxOffset = 0;
        final LinkedList<ColladaInputPipe> pipes = new LinkedList<ColladaInputPipe>();
        for (final Element i : (List<Element>) polys.getChildren("input")) {
            // Construct an input pipe...
            final ColladaInputPipe pipe = new ColladaInputPipe(i);
            pipes.add(pipe);
            try {
                maxOffset = Math.max(maxOffset, i.getAttribute("offset").getIntValue());
            } catch (final DataConversionException e) {
                e.printStackTrace();
            }
        }
        final int interval = maxOffset + 1;

        // use interval & sum of sizes of vcount to determine buffer sizes.
        int numEntries = 0;
        int numIndices = 0;
        for (final int length : ColladaDOMUtil.parseIntArray(polys.getChild("vcount"))) {
            numEntries += length;
            numIndices += (length - 2) * 3;
        }

        // Construct nio buffers for specified inputs.
        for (final ColladaInputPipe pipe : pipes) {
            pipe.setupBuffer(numEntries, polyMesh.getMeshData());
        }

        // Add to vert mapping
        final int[] indices = new int[numEntries];
        final MeshVertPairs mvp = new MeshVertPairs(polyMesh, indices);
        GlobalData.getInstance().getVertMappings().put(colladaGeometry, mvp);

        // Prepare indices buffer
        final IntBuffer meshIndices = BufferUtils.createIntBuffer(numIndices);
        polyMesh.getMeshData().setIndexBuffer(meshIndices);

        // go through the polygon entries
        int firstIndex = 0;
        int vecIndex;
        final int[] vals = ColladaDOMUtil.parseIntArray(polys.getChild("p"));
        for (final int length : ColladaDOMUtil.parseIntArray(polys.getChild("vcount"))) {
            final int[] currentVal = new int[interval];

            // first add the first two entries to the buffers.
            final int first = firstIndex + 0;
            System.arraycopy(vals, (first * interval), currentVal, 0, interval);
            vecIndex = ColladaInputPipe.processPipes(pipes, currentVal);
            if (vecIndex != Integer.MIN_VALUE) {
                indices[firstIndex + 0] = vecIndex;
            }

            int prev = firstIndex + 1;
            System.arraycopy(vals, (prev * interval), currentVal, 0, interval);
            vecIndex = ColladaInputPipe.processPipes(pipes, currentVal);
            if (vecIndex != Integer.MIN_VALUE) {
                indices[firstIndex + 1] = vecIndex;
            }

            // Now go through remaining entries and create a polygon as a triangle fan.
            for (int j = 2, max = length; j < max; j++) {
                // add first as index
                meshIndices.put(first);
                // add prev as index
                meshIndices.put(prev);

                // set prev to current
                prev = firstIndex + j;
                // add current to buffers
                System.arraycopy(vals, (prev * interval), currentVal, 0, interval);
                vecIndex = ColladaInputPipe.processPipes(pipes, currentVal);
                if (vecIndex != Integer.MIN_VALUE) {
                    indices[firstIndex + j] = vecIndex;
                }
                // add current as index
                meshIndices.put(prev);
            }
            firstIndex += length;
        }

        // update bounds
        polyMesh.updateModelBound();

        // return
        return polyMesh;
    }

    @SuppressWarnings("unchecked")
    public static Mesh buildMeshTriangles(final Element colladaGeometry, final Element tris) {
        if (tris == null || tris.getChild("input") == null || tris.getChild("p") == null) {
            return null;
        }
        final Mesh triMesh = new Mesh(tris.getAttributeValue("name"));
        triMesh.getMeshData().setIndexMode(IndexMode.Triangles);

        // Build and set RenderStates for our material
        ColladaMaterialUtils.applyMaterial(tris.getAttributeValue("material"), triMesh);

        // Pull inputs out... what is max offset? what values will we be using?
        int maxOffset = 0;
        final LinkedList<ColladaInputPipe> pipes = new LinkedList<ColladaInputPipe>();
        for (final Element i : (List<Element>) tris.getChildren("input")) {
            // Construct an input pipe...
            final ColladaInputPipe pipe = new ColladaInputPipe(i);
            pipes.add(pipe);
            try {
                maxOffset = Math.max(maxOffset, i.getAttribute("offset").getIntValue());
            } catch (final DataConversionException e) {
                e.printStackTrace();
            }
        }
        final int interval = maxOffset + 1;

        // use interval & size of p array to determine buffer sizes.
        final int[] vals = ColladaDOMUtil.parseIntArray(tris.getChild("p"));
        final int numEntries = vals.length / interval;

        // Construct nio buffers for specified inputs.
        for (final ColladaInputPipe pipe : pipes) {
            pipe.setupBuffer(numEntries, triMesh.getMeshData());
        }

        // Add to vert mapping
        final int[] indices = new int[numEntries];
        final MeshVertPairs mvp = new MeshVertPairs(triMesh, indices);
        GlobalData.getInstance().getVertMappings().put(colladaGeometry, mvp);

        // go through the p entry
        // for each p, iterate using max offset
        final int[] currentVal = new int[interval];

        // Go through entries and add to buffers.
        for (int j = 0, max = numEntries; j < max; j++) {
            // add entry to buffers
            System.arraycopy(vals, j * interval, currentVal, 0, interval);
            final int rVal = ColladaInputPipe.processPipes(pipes, currentVal);
            if (rVal != Integer.MIN_VALUE) {
                indices[j] = rVal;
            }
        }

        triMesh.updateModelBound();

        return triMesh;
    }

    @SuppressWarnings("unchecked")
    public static Line buildMeshLines(final Element colladaGeometry, final Element lines) {
        if (lines == null || lines.getChild("input") == null || lines.getChild("p") == null) {
            return null;
        }
        final Line lineMesh = new Line(lines.getAttributeValue("name"));

        // Build and set RenderStates for our material
        ColladaMaterialUtils.applyMaterial(lines.getAttributeValue("material"), lineMesh);

        // Pull inputs out... what is max offset? what values will we be using?
        int maxOffset = 0;
        final LinkedList<ColladaInputPipe> pipes = new LinkedList<ColladaInputPipe>();
        for (final Element i : (List<Element>) lines.getChildren("input")) {
            // Construct an input pipe...
            final ColladaInputPipe pipe = new ColladaInputPipe(i);
            pipes.add(pipe);
            try {
                maxOffset = Math.max(maxOffset, i.getAttribute("offset").getIntValue());
            } catch (final DataConversionException e) {
                e.printStackTrace();
            }
        }
        final int interval = maxOffset + 1;

        // use interval & size of p array to determine buffer sizes.
        final int[] vals = ColladaDOMUtil.parseIntArray(lines.getChild("p"));
        final int numEntries = vals.length / interval;

        // Construct nio buffers for specified inputs.
        for (final ColladaInputPipe pipe : pipes) {
            pipe.setupBuffer(numEntries, lineMesh.getMeshData());
        }

        // Add to vert mapping
        final int[] indices = new int[numEntries];
        final MeshVertPairs mvp = new MeshVertPairs(lineMesh, indices);
        GlobalData.getInstance().getVertMappings().put(colladaGeometry, mvp);

        // go through the p entry
        // for each p, iterate using max offset
        final int[] currentVal = new int[interval];

        // Go through entries and add to buffers.
        for (int j = 0, max = numEntries; j < max; j++) {
            // add entry to buffers
            System.arraycopy(vals, j * interval, currentVal, 0, interval);
            final int rVal = ColladaInputPipe.processPipes(pipes, currentVal);
            if (rVal != Integer.MIN_VALUE) {
                indices[j] = rVal;
            }
        }

        lineMesh.updateModelBound();

        return lineMesh;
    }
}
