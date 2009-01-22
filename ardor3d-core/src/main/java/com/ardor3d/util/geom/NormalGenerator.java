/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.geom;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.TexCoords;

/**
 * A utility class to generate normals for Meshes.<br />
 * <br />
 * The generator generates the normals using a given crease angle up to which the transitions between two triangles are
 * to be smoothed. Normals will be generated for the mesh, as long as it uses the default triangle mode (
 * <code>TRIANGLE</code>) - the modes <code>TRIANGLE_FAN</code> and <code>TRIANGLE_STRIP</code> are not supported.<br />
 * <br />
 * The generator currently only supports a single set of texture coordinates in a mesh. If more than one set of texture
 * coordinates is specified in a mesh, all sets after the first one will be deleted.<br />
 * <br />
 * <strong>Please note:</strong> The mesh must be <cite>manifold</cite>, i.e. only 2 triangles may be connected by one
 * edge, and the mesh has to be connected by means of edges, not vertices. Otherwise, the normal generation might fail,
 * with undefined results.
 */
public class NormalGenerator {

    private static final Logger logger = Logger.getLogger(NormalGenerator.class.getName());

    // The angle up to which edges between triangles are smoothed
    private float creaseAngle;

    // Source data
    private Vector3[] sourceVerts;
    private ColorRGBA[] sourceColors;
    private Vector2[] sourceTexCoords;
    private int[] sourceInds;
    private LinkedList<Triangle> triangles;

    // Computed (destination) data for one mesh split
    private List<Vector3> destVerts;
    private List<ColorRGBA> destColors;
    private List<Vector2> destTexCoords;
    private LinkedList<Triangle> destTris;
    private LinkedList<Edge> edges;

    // The lists to store the split data for the final mesh
    private LinkedList<LinkedList<Triangle>> splitMeshes;
    private LinkedList<LinkedList<Edge>> splitMeshBorders;
    private Vector3[] splitVerts;
    private ColorRGBA[] splitColors;
    private Vector2[] splitTexCoords;
    private Vector3[] splitNormals;
    private int[] splitIndices;

    // The data used to compute the final mesh
    private boolean[] borderIndices;

    // Temporary data used for computation
    private final Vector3 compVect0 = new Vector3();
    private final Vector3 compVect1 = new Vector3();

    /**
     * Generates the normals for one Mesh, using the specified crease angle.
     * 
     * @param mesh
     *            The Mesh to generate the normals for
     * @param creaseAngle
     *            The angle between two triangles up to which the normal between the two triangles will be interpolated,
     *            creating a smooth transition
     */
    public void generateNormals(final Mesh mesh, final float creaseAngle) {
        if (mesh != null) {
            this.creaseAngle = creaseAngle;
            generateNormals(mesh);
            cleanup();
        }
    }

    /**
     * Generates the normals for one Mesh, using the crease angle stored in the field <code>creaseAngle</code>
     * 
     * @param mesh
     *            The Mesh to generate the normals for
     */
    private void generateNormals(final Mesh mesh) {
        if (mesh.getMeshData().getIndexMode() != IndexMode.Triangles) {
            logger.info("Invalid triangles mode in " + mesh);
            return;
        }

        // Get the data of the mesh as arrays
        sourceInds = BufferUtils.getIntArray(mesh.getMeshData().getIndexBuffer());
        sourceVerts = BufferUtils.getVector3Array(mesh.getMeshData().getVertexBuffer());
        if (mesh.getMeshData().getColorBuffer() != null) {
            sourceColors = BufferUtils.getColorArray(mesh.getMeshData().getColorBuffer());
        } else {
            sourceColors = null;
        }
        if (mesh.getMeshData().getTextureCoords(0) != null) {
            sourceTexCoords = BufferUtils.getVector2Array(mesh.getMeshData().getTextureCoords(0).coords);
        } else {
            sourceTexCoords = null;
        }

        // Initialize the lists needed to generate the normals for the mesh
        initialize();

        // Process all triangles in the mesh. Create one connected mesh for
        // every set of triangles that are connected by their vertex indices
        // with an angle not greater than the creaseAngle.
        while (!triangles.isEmpty()) {
            createMeshSplit();
        }

        // Duplicate all vertices that are shared by different split meshes
        if (!splitMeshes.isEmpty()) {
            borderIndices = new boolean[sourceVerts.length];
            fillBorderIndices();
            duplicateCreaseVertices();
        }

        // Set up the arrays for reconstructing the mesh: Vertices,
        // texture coordinates, colors, normals and indices
        splitVerts = destVerts.toArray(new Vector3[destVerts.size()]);
        if (destColors != null) {
            splitColors = destColors.toArray(new ColorRGBA[destColors.size()]);
        } else {
            splitColors = null;
        }
        if (destTexCoords != null) {
            splitTexCoords = destTexCoords.toArray(new Vector2[destTexCoords.size()]);
        } else {
            splitTexCoords = null;
        }
        splitNormals = new Vector3[destVerts.size()];
        for (int j = 0; j < splitNormals.length; j++) {
            splitNormals[j] = new Vector3();
        }
        int numTris = 0;
        for (final LinkedList<Triangle> tris : splitMeshes) {
            numTris += tris.size();
        }
        splitIndices = new int[numTris * 3];

        // For each of the split meshes, create the interpolated normals
        // between its triangles and set up its index array in the process
        computeNormalsAndIndices();

        // Set up the buffers for the mesh

        // Vertex buffer:
        FloatBuffer vertices = mesh.getMeshData().getVertexBuffer();
        if (vertices.capacity() < splitVerts.length * 3) {
            vertices = BufferUtils.createFloatBuffer(splitVerts);
        } else {
            vertices.clear();
            for (final Vector3 vertex : splitVerts) {
                vertices.put((float) vertex.getX()).put((float) vertex.getY()).put((float) vertex.getZ());
            }
            vertices.flip();
        }

        // Normal buffer:
        FloatBuffer normals = mesh.getMeshData().getNormalBuffer();
        if (normals == null || normals.capacity() < splitNormals.length * 3) {
            normals = BufferUtils.createFloatBuffer(splitNormals);
        } else {
            normals.clear();
            for (final Vector3 normal : splitNormals) {
                normals.put((float) normal.getX()).put((float) normal.getY()).put((float) normal.getZ());
            }
            normals.flip();
        }

        // Color buffer:
        FloatBuffer colors = null;
        if (splitColors != null) {
            colors = mesh.getMeshData().getColorBuffer();
            if (colors.capacity() < splitColors.length * 4) {
                colors = BufferUtils.createFloatBuffer(splitColors);
            } else {
                colors.clear();
                for (final ColorRGBA color : splitColors) {
                    colors.put(color.getRed()).put(color.getGreen()).put(color.getBlue()).put(color.getAlpha());
                }
                colors.flip();
            }
        }

        // Tex coord buffer:
        FloatBuffer texCoords = null;
        if (splitTexCoords != null) {
            texCoords = mesh.getMeshData().getTextureCoords(0).coords;
            if (texCoords.capacity() < splitTexCoords.length * 2) {
                texCoords = BufferUtils.createFloatBuffer(splitTexCoords);
            } else {
                texCoords.clear();
                for (final Vector2 texCoord : splitTexCoords) {
                    texCoords.put((float) texCoord.getX()).put((float) texCoord.getY());
                }
                texCoords.flip();
            }
        }

        // Index buffer:
        IntBuffer indices = mesh.getMeshData().getIndexBuffer();
        if (indices.capacity() < splitIndices.length) {
            indices = BufferUtils.createIntBuffer(splitIndices);
        } else {
            indices.clear();
            indices.put(splitIndices);
            indices.flip();
        }

        // Apply the buffers to the mesh
        mesh.getMeshData().setVertexBuffer(vertices);
        mesh.getMeshData().setNormalBuffer(normals);
        mesh.getMeshData().setColorBuffer(colors);
        mesh.getMeshData().getTextureCoords().clear();
        mesh.getMeshData().setTextureCoords(new TexCoords(texCoords), 0);
        mesh.getMeshData().setIndexBuffer(indices);
    }

    /**
     * Sets up the lists to get the data for normal generation from.
     */
    private void initialize() {
        // Copy the source vertices as a base for the normal generation
        destVerts = new ArrayList<Vector3>(sourceVerts.length);
        for (int i = 0; i < sourceVerts.length; i++) {
            destVerts.add(sourceVerts[i]);
        }
        if (sourceColors != null) {
            destColors = new ArrayList<ColorRGBA>(sourceColors.length);
            for (int i = 0; i < sourceColors.length; i++) {
                destColors.add(sourceColors[i]);
            }
        } else {
            destColors = null;
        }
        if (sourceTexCoords != null) {
            destTexCoords = new ArrayList<Vector2>(sourceTexCoords.length);
            for (int i = 0; i < sourceTexCoords.length; i++) {
                destTexCoords.add(sourceTexCoords[i]);
            }
        } else {
            destTexCoords = null;
        }

        // Set up the base triangles of the mesh and their face normals
        triangles = new LinkedList<Triangle>();
        for (int i = 0; i * 3 < sourceInds.length; i++) {
            final Triangle tri = new Triangle(sourceInds[i * 3 + 0], sourceInds[i * 3 + 1], sourceInds[i * 3 + 2]);
            tri.computeNormal(sourceVerts);
            triangles.add(tri);
        }

        // Set up the lists to store the created mesh split data
        if (splitMeshes == null) {
            splitMeshes = new LinkedList<LinkedList<Triangle>>();
        } else {
            splitMeshes.clear();
        }
        if (splitMeshBorders == null) {
            splitMeshBorders = new LinkedList<LinkedList<Edge>>();
        } else {
            splitMeshBorders.clear();
        }
    }

    /**
     * Assembles one set of triangles ("split mesh") that are connected and do not contain a transition with an angle
     * greater than the creaseAngle. The Triangles of the split mesh are stored in splitMeshes, and the Edges along the
     * border of the split mesh in splitMeshBorders.
     */
    private void createMeshSplit() {
        destTris = new LinkedList<Triangle>();
        edges = new LinkedList<Edge>();
        final Triangle tri = triangles.removeFirst();
        destTris.addLast(tri);
        edges.addLast(tri.edges[0]);
        edges.addLast(tri.edges[1]);
        edges.addLast(tri.edges[2]);

        Triangle newTri;
        do {
            newTri = insertTriangle();
        } while (newTri != null);

        splitMeshes.addLast(destTris);
        splitMeshBorders.addLast(edges);
    }

    /**
     * Finds one triangle connected to the split mesh currently being assembled over an edge whose angle does not exceed
     * the creaseAngle. The Triangle is inserted into destTris and the list edges is updated with the edges of the
     * triangle accordingly.
     * 
     * @return The triangle, if one was found, or <code>null</code> otherwise
     */
    private Triangle insertTriangle() {
        final ListIterator<Triangle> triIt = triangles.listIterator();
        ListIterator<Edge> edgeIt = null;

        // Find a triangle that is connected to the border of the currently
        // assembled mesh split and whose angle to the connected border triangle
        // is less than or equal to the creaseAngle
        Triangle result = null;
        int connected = -1;
        Edge borderEdge = null;
        while (result == null && triIt.hasNext()) {
            final Triangle tri = triIt.next();
            edgeIt = edges.listIterator();
            while (result == null && edgeIt.hasNext()) {
                borderEdge = edgeIt.next();
                for (int i = 0; i < tri.edges.length && result == null; i++) {
                    if (borderEdge.isConnectedTo(tri.edges[i]) && checkAngle(tri, borderEdge.parent)) {
                        connected = i;
                        result = tri;
                    }
                }
            }
        }

        // If a triangle has been found, remove it from the list of remaining
        // triangles and add it to the current split mesh, including its borders
        if (result != null) {
            // Connect the triangle to the split mesh
            triIt.remove();
            destTris.addLast(result);
            borderEdge.connected = result;
            final Edge resultEdge = result.edges[connected];
            resultEdge.connected = borderEdge.parent;
            edgeIt.remove();
            edgeIt.add(result.edges[(connected + 1) % 3]);
            edgeIt.add(result.edges[(connected + 2) % 3]);

            // If the connected edge had cloned vertices, use them for the new
            // triangle too
            if (borderEdge.newI0 > -1) {
                resultEdge.newI1 = borderEdge.newI0;
                result.edges[(connected + 1) % 3].newI0 = borderEdge.newI0;
            }
            if (borderEdge.newI1 > -1) {
                resultEdge.newI0 = borderEdge.newI1;
                result.edges[(connected + 2) % 3].newI1 = borderEdge.newI1;
            }

            // Check if the triangle is connected to other edges along the
            // border of the current split mesh
            for (int i = connected + 1; i < connected + 3; i++) {
                connectEdge(result, i % 3);
            }
        }

        return result;
    }

    /**
     * Connects the remaining edges of the given triangle to the split mesh currently being assembled, if possible. The
     * respective edges are removed from the border, and if the crease angle at this additional connection is exceeded,
     * the vertices at this link are duplicated.
     * 
     * @param triangle
     *            The triangle being connected to the split mesh
     * @param i
     *            The index of the edge in the triangle that is already connected to the split mesh
     */
    private void connectEdge(final Triangle triangle, final int i) {
        final Edge edge = triangle.edges[i];
        final ListIterator<Edge> edgeIt = edges.listIterator();
        boolean found = false;
        while (!found && edgeIt.hasNext()) {
            final Edge borderEdge = edgeIt.next();
            if (borderEdge.isConnectedTo(edge)) {
                // Connected => remove the connected edges from the
                // border
                found = true;
                edgeIt.remove();
                edges.remove(edge);
                if (!checkAngle(triangle, borderEdge.parent)) {
                    // Crease angle exceeded => duplicate the vertices, colors
                    // and texCoords
                    duplicateValues(edge.i0);
                    edge.newI0 = destVerts.size() - 1;
                    triangle.edges[(i + 2) % 3].newI1 = edge.newI0;
                    duplicateValues(edge.i1);
                    edge.newI1 = destVerts.size() - 1;
                    triangle.edges[(i + 1) % 3].newI0 = edge.newI1;
                } else {
                    // Crease angle okay => share duplicate vertices, if
                    // any
                    if (borderEdge.newI0 > -1) {
                        edge.newI1 = borderEdge.newI0;
                        triangle.edges[(i + 1) % 3].newI0 = borderEdge.newI0;
                    }
                    if (borderEdge.newI1 > -1) {
                        edge.newI0 = borderEdge.newI1;
                        triangle.edges[(i + 2) % 3].newI1 = borderEdge.newI1;
                    }
                }
            }
        }
    }

    /**
     * Checks if the transition between the tqo given triangles should be smooth, according to the creaseAngle.
     * 
     * @param tri1
     *            The first triangle
     * @param tri2
     *            The second triangle
     * @return <code>true</code>, if the angle between the two triangles is less than or equal to the creaseAngle;
     *         otherwise <code>false</code>
     */
    private boolean checkAngle(final Triangle tri1, final Triangle tri2) {
        return (tri1.normal.smallestAngleBetween(tri2.normal) <= creaseAngle + MathUtils.ZERO_TOLERANCE);
    }

    /**
     * Copies the vertex, color and texCoord at the given index in each of the source lists (if not null) and adds it to
     * the end of the list.
     * 
     * @param index
     *            The index to copy the value in each list from
     */
    private void duplicateValues(final int index) {
        destVerts.add(destVerts.get(index));
        if (destColors != null) {
            destColors.add(destColors.get(index));
        }
        if (destTexCoords != null) {
            destTexCoords.add(destTexCoords.get(index));
        }
    }

    /**
     * Fills the borderIndices array with the all indices contained in the first set of border edges stored in
     * splitMeshBorders. All values not set in the process are set to -1.
     */
    private void fillBorderIndices() {
        Arrays.fill(borderIndices, false);
        final LinkedList<Edge> edges0 = splitMeshBorders.getFirst();
        for (final Edge edge : edges0) {
            borderIndices[edge.i0] = true;
            borderIndices[edge.i1] = true;
        }
    }

    /**
     * Finds all vertices that are used by several split meshes and copies them (including updating the indices in the
     * corresponding triangles).
     */
    private void duplicateCreaseVertices() {
        if (splitMeshBorders.size() < 2) {
            return;
        }

        final int[] replacementIndices = new int[sourceVerts.length];

        // Check the borders of all split meshes, starting with the second one
        final ListIterator<LinkedList<Edge>> borderIt = splitMeshBorders.listIterator();
        borderIt.next();
        final ListIterator<LinkedList<Triangle>> meshIt = splitMeshes.listIterator();
        meshIt.next();
        while (borderIt.hasNext()) {
            Arrays.fill(replacementIndices, -1);
            final LinkedList<Edge> edges0 = borderIt.next(); // Border of the split
            final LinkedList<Triangle> destTris0 = meshIt.next(); // Triangles of the
            // split

            // Check every border edge of the split mesh. If its indices are
            // already set in borderIndices, the corresponding vertices are
            // already used by another split and have to be duplicated
            final ListIterator<Edge> edgeIt = edges0.listIterator();
            while (edgeIt.hasNext()) {
                final Edge edge = edgeIt.next();

                if (edge.newI0 == -1) {
                    if (borderIndices[edge.i0]) {
                        if (replacementIndices[edge.i0] == -1) {
                            duplicateValues(edge.i0);
                            replacementIndices[edge.i0] = destVerts.size() - 1;
                        }
                    } else {
                        replacementIndices[edge.i0] = edge.i0;
                    }
                }

                if (edge.newI1 == -1) {
                    if (borderIndices[edge.i1]) {
                        if (replacementIndices[edge.i1] == -1) {
                            duplicateValues(edge.i1);
                            replacementIndices[edge.i1] = destVerts.size() - 1;
                        }
                    } else {
                        replacementIndices[edge.i1] = edge.i1;
                    }
                }
            }

            // Replace all indices in the split mesh whose vertices have been
            // duplicated
            for (int i = 0; i < borderIndices.length; i++) {
                if (borderIndices[i]) {
                    for (final Triangle tri : destTris0) {
                        replaceIndex(tri, i, replacementIndices[i]);
                    }
                } else if (replacementIndices[i] > -1) {
                    borderIndices[i] = true;
                }
            }
        }
    }

    /**
     * If the triangle contains the given index, it is replaced with the replacement index, unless it is already
     * overridden with a newIndex (newI0, newI1).
     * 
     * @param tri
     *            The triangle
     * @param index
     *            The index to replace
     * @param replacement
     *            The replacement index
     */
    private void replaceIndex(final Triangle tri, final int index, final int replacement) {
        for (int i = 0; i < 3; i++) {
            final Edge edge = tri.edges[i];
            if (edge.newI0 == -1 && edge.i0 == index) {
                edge.newI0 = replacement;
            }
            if (edge.newI1 == -1 && edge.i1 == index) {
                edge.newI1 = replacement;
            }
        }
    }

    /**
     * Sets up the normals and indices for all split meshes.
     */
    private void computeNormalsAndIndices() {

        // First, sum up the normals of the adjacent triangles for each vertex.
        // Store the triangle indices in the process.
        int count = 0;
        for (final LinkedList<Triangle> tris : splitMeshes) {
            for (final Triangle tri : tris) {
                for (int i = 0; i < 3; i++) {
                    if (tri.edges[i].newI0 > -1) {
                        splitNormals[tri.edges[i].newI0].addLocal(tri.normal);
                        splitIndices[count++] = tri.edges[i].newI0;
                    } else {
                        splitNormals[tri.edges[i].i0].addLocal(tri.normal);
                        splitIndices[count++] = tri.edges[i].i0;
                    }

                }
            }
        }

        // Normalize all normals
        for (int i = 0; i < splitNormals.length; i++) {
            if (splitNormals[i].distanceSquared(Vector3.ZERO) > MathUtils.ZERO_TOLERANCE) {
                splitNormals[i].normalizeLocal();
            }
        }
    }

    /**
     * Clears and nulls all used arrays and lists, so the garbage collector can clean them up.
     */
    private void cleanup() {
        creaseAngle = 0;
        Arrays.fill(sourceVerts, null);
        sourceVerts = null;
        if (sourceColors != null) {
            Arrays.fill(sourceColors, null);
            sourceColors = null;
        }
        if (sourceTexCoords != null) {
            Arrays.fill(sourceTexCoords, null);
            sourceTexCoords = null;
        }
        sourceInds = null;

        if (triangles != null) {
            triangles.clear();
            triangles = null;
        }
        if (destVerts != null) {
            destVerts.clear();
            destVerts = null;
        }
        if (destColors != null) {
            destColors.clear();
            destColors = null;
        }
        if (destTexCoords != null) {
            destTexCoords.clear();
            destTexCoords = null;
        }
        if (destTris != null) {
            destTris.clear();
            destTris = null;
        }
        if (edges != null) {
            edges.clear();
            edges = null;
        }

        if (splitMeshes != null) {
            for (final LinkedList<Triangle> tris : splitMeshes) {
                tris.clear();
            }
            splitMeshes.clear();
            splitMeshes = null;
        }
        if (splitMeshBorders != null) {
            for (final LinkedList<Edge> edges : splitMeshBorders) {
                edges.clear();
            }
            splitMeshBorders.clear();
            splitMeshBorders = null;
        }

        splitVerts = null;
        splitNormals = null;
        splitColors = null;
        splitTexCoords = null;
        splitIndices = null;
        borderIndices = null;
    }

    /**
     * A helper class for the normal generator. Stores one triangle, consisting of 3 edges, and the normal for the
     * triangle.
     * 
     * @author M. Sattler
     */
    private class Triangle {

        public Edge[] edges = new Edge[3];

        public Vector3 normal = new Vector3(0, 0, 0);

        public Triangle() {}

        /**
         * Creates the triangle.
         * 
         * @param i0
         *            The index of vertex 0 in the triangle
         * @param i1
         *            The index of vertex 1 in the triangle
         * @param i2
         *            The index of vertex 2 in the triangle
         */
        public Triangle(final int i0, final int i1, final int i2) {
            edges[0] = new Edge(this, i0, i1);
            edges[1] = new Edge(this, i1, i2);
            edges[2] = new Edge(this, i2, i0);
        }

        /**
         * Computes the normal from the three vertices in the given array that are indexed by the edges.
         * 
         * @param verts
         *            The array containing the vertices
         */
        public void computeNormal(final Vector3[] verts) {
            final int i0 = edges[0].i0;
            final int i1 = edges[1].i0;
            final int i2 = edges[2].i0;
            verts[i2].subtract(verts[i1], compVect0);
            verts[i0].subtract(verts[i1], compVect1);
            normal.set(compVect0.crossLocal(compVect1)).normalizeLocal();
        }

        /**
         * @param edge
         *            An Edge to get the index of
         * @return The index of the edge in the triangle, or -1, if it is not contained in the triangle
         */
        public int indexOf(final Edge edge) {
            for (int i = 0; i < 3; i++) {
                if (edges[i] == edge) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public String toString() {
            String result = "Triangle (";
            for (int i = 0; i < 3; i++) {
                final Edge edge = edges[i];
                if (edge == null) {
                    result += "?";
                } else {
                    if (edge.newI0 > -1) {
                        result += edge.newI0;
                    } else {
                        result += edge.i0;
                    }
                }
                if (i < 2) {
                    result += ", ";
                }
            }
            result += ")";
            return result;
        }
    }

    /**
     * Another helper class for the normal generator. Stores one edge in the mesh, consisting of two vertex indices, the
     * triangle the edge belongs to, and, if applicable, another triangle the edge is connected to.
     * 
     * @author M. Sattler
     */
    private class Edge {

        // The indices of the vertices in the mesh
        public int i0;
        public int i1;

        // The indices of duplicated vertices, if > -1
        public int newI0 = -1;
        public int newI1 = -1;

        // The Triangle containing this Edge
        public Triangle parent;

        // A Triangle this Edge is connected to, or null, if it is not connected
        public Triangle connected;

        public Edge() {}

        /**
         * Creates this edge.
         * 
         * @param parent
         *            The Triangle containing this Edge
         * @param i0
         *            The index of the first vertex of this edge
         * @param i1
         *            The index of the second vertex of this edge
         */
        public Edge(final Triangle parent, final int i0, final int i1) {
            this.parent = parent;
            this.i0 = i0;
            this.i1 = i1;
        }

        /**
         * Checks if this edge is connected to another one.
         * 
         * @param other
         *            The other edge
         * @return <code>true</code>, if the indices in this edge and the other one are identical, but in inverse order
         */
        public boolean isConnectedTo(final Edge other) {
            return (i0 == other.i1 && i1 == other.i0);
        }

        @Override
        public String toString() {
            String result = "Edge (";
            if (newI0 > -1) {
                result += newI0;
            } else {
                result += i0;
            }
            result += ", ";
            if (newI1 > -1) {
                result += newI1;
            } else {
                result += i1;
            }
            result += ")";
            return result;
        }
    }
}