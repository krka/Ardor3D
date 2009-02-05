/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.shadow.stencil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import com.ardor3d.light.DirectionalLight;
import com.ardor3d.light.Light;
import com.ardor3d.light.PointLight;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Plane;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyMatrix3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.geom.BufferUtils;

/**
 * <code>MeshShadows</code> A grouping of the ShadowVolumes for a single Mesh.
 */
public class MeshShadows {
    private static final long serialVersionUID = 1L;

    /** the distance to which shadow volumes will be projected */
    protected float _projectionLength = 1000;

    /** The triangles of our occluding mesh (one per triangle in the mesh) */
    protected List<ShadowTriangle> _faces;

    /** A bitset used for storing directional flags. */
    protected BitSet _facing;

    /** The mesh that is the target of this shadow volume */
    protected Mesh _target = null;

    /** The arraylist of shadowvolumes in this grouping */
    protected List<ShadowVolume> _volumes = new ArrayList<ShadowVolume>();

    /** The world rotation of the target at the last mesh construction */
    protected Matrix3 _oldWorldRotation = new Matrix3();

    /** The world translation of the trimesh at the last mesh construction */
    protected Vector3 _oldWorldTranslation = new Vector3();

    /** The world scale of the trimesh at the last mesh construction */
    protected Vector3 _oldWorldScale = new Vector3();

    private int _maxIndex;

    private int _vertCount;

    public static long _throttle = 1000 / 50; // 50 x a sec
    private long _lastTime;
    private boolean _nextTime = true;

    /**
     * Constructor for <code>MeshShadows</code>
     * 
     * @param target
     *            the mesh that will be the target of the shadow volumes held in this grouping
     */
    public MeshShadows(final Mesh target) {
        _target = target;
        recreateFaces();
    }

    /**
     * <code>createGeometry</code> creates or updates the ShadowVolume geometries for the target Mesh - one for each
     * applicable Light in the given LightState. Only Directional and Point lights are currently supported. ShadowVolume
     * geometry is only regen'd when light or occluder aspects change.
     * 
     * XXX: Note that this only supports the first section of a MeshData.
     * 
     * @param lightState
     *            is the current lighting state
     * @param factory
     */
    public void createGeometry(final LightState lightState) {
        if (_target.getMeshData().getPrimitiveCount(0) != _maxIndex
                || _target.getMeshData().getVertexCount() != _vertCount) {
            recreateFaces();
        }

        // Holds a copy of the mesh vertices transformed to world coordinates
        FloatBuffer vertex = null;

        // Ensure that we have some potential lights to cast shadows!
        if (lightState.getNumberOfChildren() != 0) {
            final LightState lights = lightState;

            // Update the cache of lights - if still sane, return
            if (updateCache(lights)) {
                return;
            }

            // Now scan through each light and create the shadow volume
            for (int l = 0; l < lights.getNumberOfChildren(); l++) {
                final Light light = lights.get(l);

                // Make sure we can (or want to) handle this light
                if (!light.isShadowCaster()
                        || (!(light.getType() == Light.Type.Directional) && !(light.getType() == Light.Type.Point))) {
                    continue;
                }

                // Get the volume assoicated with this light
                ShadowVolume lv = getShadowVolume(light);

                // See if this light has not been seen before!
                if (lv == null) {
                    // Create a new light volume
                    lv = new ShadowVolume(light);
                    _volumes.add(lv);
                    lv.setUpdate(true);
                }

                // See if the volume requires updating
                if (lv.isUpdate()) {
                    lv.setUpdate(false);

                    if (!_target.isCastsShadows()) {
                        lv.setCullHint(Spatial.CullHint.Always);
                        continue;
                    }

                    lv.setCullHint(Spatial.CullHint.Dynamic);

                    // Translate the vertex information from the mesh to
                    // world
                    // coordinates if
                    // we are going to do any work
                    if (vertex == null) {
                        vertex = _target.getWorldVectors(null);
                    }

                    // Find out which triangles are facing the light
                    // triangle will be set true for faces towards the light
                    processFaces(vertex, light, _target);

                    // Get the edges that are in shadow
                    final ShadowEdge[] edges = getShadowEdges();

                    // Now we need to develop a mesh based on projecting
                    // these
                    // edges
                    // to infinity in the direction of the light
                    final int length = edges.length;

                    // Create arrays to hold the shadow mesh
                    FloatBuffer shadowVertex = lv.getMeshData().getVertexBuffer();
                    if (shadowVertex == null || shadowVertex.capacity() < length * 12) {
                        shadowVertex = BufferUtils.createVector3Buffer(length * 4);
                    }
                    FloatBuffer shadowNormal = lv.getMeshData().getNormalBuffer();
                    if (shadowNormal == null || shadowNormal.capacity() < length * 12) {
                        shadowNormal = BufferUtils.createVector3Buffer(length * 4);
                    }
                    IntBuffer shadowIndex = lv.getMeshData().getIndexBuffer();
                    if (shadowIndex == null || shadowIndex.capacity() < length * 6) {
                        shadowIndex = BufferUtils.createIntBuffer(length * 6);
                    }

                    shadowVertex.limit(length * 12);
                    shadowNormal.limit(length * 12);
                    shadowIndex.limit(length * 6);

                    // Create quads out of the edge vertices
                    createShadowQuads(vertex, edges, shadowVertex, shadowNormal, shadowIndex, light);

                    // Rebuild the Mesh
                    lv.reconstruct(shadowVertex, shadowNormal, null, null);
                    lv.getMeshData().setIndexBuffer(shadowIndex);
                    shadowVertex.rewind();
                    shadowIndex.rewind();
                    lv.updateModelBound();
                }

            }

        } else {
            // There are no volumes
            _volumes.clear();
        }

    }

    /**
     * void <code>createShadowQuad</code> Creates projected quads from a series of edges and vertices and stores them in
     * the output shadowXXXX arrays
     * 
     * @param vertex
     *            array of world coordinate vertices for the target Mesh
     * @param edges
     *            a collection of edges that will be projected
     * @param shadowVertex
     * @param shadowNormal
     * @param shadowIndex
     * @param light
     *            light casting shadow
     */
    private void createShadowQuads(final FloatBuffer vertex, final ShadowEdge[] edges, final FloatBuffer shadowVertex,
            final FloatBuffer shadowNormal, final IntBuffer shadowIndex, final Light light) {
        final Vector3 p0 = new Vector3();
        Vector3 p1 = new Vector3(), p2 = new Vector3();
        final Vector3 p3 = new Vector3();

        // Setup a flag to indicate which type of light this is
        final boolean directional = (light.getType() == Light.Type.Directional);

        final Vector3 direction = new Vector3();
        final Vector3 location = new Vector3();
        if (directional) {
            direction.set(((DirectionalLight) light).getDirection());
        } else {
            location.set(((PointLight) light).getLocation());
        }

        // Loop for each edge
        for (int e = 0; e < edges.length; e++) {
            // get the two known vertices
            BufferUtils.populateFromBuffer(p0, vertex, edges[e].p0);
            BufferUtils.populateFromBuffer(p3, vertex, edges[e].p1);

            // Calculate the projection of p0
            if (!directional) {
                p0.subtract(location, direction).normalizeLocal();
            }
            // Project the other edges to infinity
            p1 = direction.multiply(_projectionLength, p1).addLocal(p0);
            if (!directional) {
                p3.subtract(location, direction).normalizeLocal();
            }
            p2 = direction.multiply(_projectionLength, p2).addLocal(p3);

            // Now we need to add a quad to the model
            final int vertexOffset = e * 4;
            BufferUtils.setInBuffer(p0, shadowVertex, vertexOffset);
            BufferUtils.setInBuffer(p1, shadowVertex, vertexOffset + 1);
            BufferUtils.setInBuffer(p2, shadowVertex, vertexOffset + 2);
            BufferUtils.setInBuffer(p3, shadowVertex, vertexOffset + 3);

            // Calculate the normal
            final Vector3 n = p1.subtract(p0, Vector3.fetchTempInstance()).normalizeLocal();
            final Vector3 nDiff = p3.subtract(p0, Vector3.fetchTempInstance()).normalizeLocal();

            n.crossLocal(nDiff).normalizeLocal();
            BufferUtils.setInBuffer(n, shadowNormal, vertexOffset);
            BufferUtils.setInBuffer(n, shadowNormal, vertexOffset + 1);
            BufferUtils.setInBuffer(n, shadowNormal, vertexOffset + 2);
            BufferUtils.setInBuffer(n, shadowNormal, vertexOffset + 3);

            // Add the indices
            final int indexOffset = e * 6;
            shadowIndex.put(indexOffset + 0, vertexOffset + 0);
            shadowIndex.put(indexOffset + 1, vertexOffset + 1);
            shadowIndex.put(indexOffset + 2, vertexOffset + 3);
            shadowIndex.put(indexOffset + 3, vertexOffset + 3);
            shadowIndex.put(indexOffset + 4, vertexOffset + 1);
            shadowIndex.put(indexOffset + 5, vertexOffset + 2);
        }
    }

    // Get the intersection of a line segment and a plane in terms of t>=0 t<=1
    // for positions within the segment
    protected double getIntersectTime(final Plane p, final Vector3 p0, final Vector3 v) {
        final ReadOnlyVector3 normal = p.getNormal();
        final double divider = normal.dot(v);
        if (divider == 0) {
            return -Float.MAX_VALUE;
        }
        final Vector3 normalScaled = normal.multiply(p.getConstant(), Vector3.fetchTempInstance());
        final double intersectTime = normal.dot(normalScaled.subtractLocal(p0)) / divider;

        Vector3.releaseTempInstance(normalScaled);
        return intersectTime;

    }

    /**
     * <code>getShadowEdges</code>
     * 
     * @return an array of the edges which are in shadow
     */
    private ShadowEdge[] getShadowEdges() {
        // Create a dynamic structure to contain the vertices
        final List<ShadowEdge> shadowEdges = new ArrayList<ShadowEdge>();
        // Now work through the faces
        for (int t = 0; t < _maxIndex; t++) {
            // Check whether this is a front facing triangle
            if (_facing.get(t)) {
                final ShadowTriangle tri = _faces.get(t);
                // If it is then check if any of the edges are connected to a
                // back facing triangle or are unconnected
                checkAndAdd(tri.edge1, shadowEdges);
                checkAndAdd(tri.edge2, shadowEdges);
                checkAndAdd(tri.edge3, shadowEdges);
            }
        }
        return shadowEdges.toArray(new ShadowEdge[0]);
    }

    private void checkAndAdd(final ShadowEdge edge, final List<ShadowEdge> shadowEdges) {
        // Is the edge connected
        if (edge.triangle == ShadowTriangle.INVALID_TRIANGLE) {
            // if not then add the edge
            shadowEdges.add(edge);

        }
        // check if the connected triangle is back facing
        else if (!_facing.get(edge.triangle)) {
            // if it is then add the edge
            shadowEdges.add(edge);

        }
    }

    /**
     * <code>processFaces</code> Determines whether faces of a Mesh face the light
     * 
     * @param triangle
     *            an array of boolean values that will indicate whether a triangle is front or back facing
     * @param light
     *            the light to use
     * @param target
     *            the Mesh that will be shadowed and holds the triangles for testing
     */
    private void processFaces(final FloatBuffer vertex, final Light light, final Mesh target) {
        if (target.getMeshData().getIndexBuffer() == null) {
            return;
        }

        final Vector3 v0 = Vector3.fetchTempInstance();
        final Vector3 v1 = Vector3.fetchTempInstance();
        final Vector3 compVect = Vector3.fetchTempInstance();
        final Vector3 compVect2 = Vector3.fetchTempInstance();
        final boolean directional = light.getType() == Light.Type.Directional;
        final Vector3 vLight = Vector3.fetchTempInstance();
        final int[] index = BufferUtils.getIntArray(target.getMeshData().getIndexBuffer());

        if (directional) {
            vLight.set(((DirectionalLight) light).getDirection());
        }

        // Loop through each triangle and see if it is back or front facing
        for (int t = 0, tri = 0; t < index.length; tri++, t += 3) {
            // Calculate a normal to the plane
            BufferUtils.populateFromBuffer(compVect, vertex, index[t]);
            BufferUtils.populateFromBuffer(v0, vertex, index[t + 1]);
            BufferUtils.populateFromBuffer(v1, vertex, index[t + 2]);
            v1.subtractLocal(v0).normalizeLocal();
            v0.subtractLocal(compVect).normalizeLocal();
            final Vector3 n = v1.cross(v0, compVect2);

            // Some kind of bodge for a direction to a point light -
            // TODO: improve this
            if (!directional) {
                vLight.set(((PointLight) light).getLocation());
                compVect.subtract(vLight, vLight).normalizeLocal();
            }
            // See if it is back facing
            _facing.set(tri, (n.dot(vLight) >= 0));
        }

        Vector3.releaseTempInstance(v0);
        Vector3.releaseTempInstance(v1);
        Vector3.releaseTempInstance(compVect);
        Vector3.releaseTempInstance(compVect2);
        Vector3.releaseTempInstance(vLight);
    }

    /**
     * <code>updateCache</code> Updates the cache to show which models need rebuilding
     * 
     * @param lights
     *            a LightState containing the lights to check against
     * @return returns <code>true</code> if the cache was not invalidated
     */
    private boolean updateCache(final LightState lights) {
        boolean voidLights = false;
        boolean same = true;

        final float passTime = System.currentTimeMillis() - _lastTime;

        final ReadOnlyVector3 worldTranslation = _target.getWorldTranslation();
        final ReadOnlyVector3 worldScale = _target.getWorldScale();
        final ReadOnlyMatrix3 worldRotation = _target.getWorldRotation();

        if (_nextTime) {
            if (passTime > _throttle) {
                voidLights = true;
                _nextTime = false;
            }
        } else {
            // First see if we need to void all volumes as the target has
            // changed
            if (!worldRotation.equals(_oldWorldRotation)) {
                voidLights = true;
            } else if (!worldScale.equals(_oldWorldScale)) {
                voidLights = true;
            } else if (!worldTranslation.equals(_oldWorldTranslation)) {
                voidLights = true;
            }
        }
        // Configure the current settings
        _oldWorldRotation.set(worldRotation);
        _oldWorldScale.set(worldScale);
        _oldWorldTranslation.set(worldTranslation);

        // See if we need to update all of the volumes
        if (voidLights) {
            for (int v = 0, vSize = _volumes.size(); v < vSize; v++) {
                final ShadowVolume sv = _volumes.get(v);
                sv.setUpdate(true);
            }
            _lastTime = System.currentTimeMillis();
            _nextTime = false;
            return false;
        }

        // Loop through the lights to see if any have changed
        for (int i = lights.getNumberOfChildren(); --i >= 0;) {
            final Light testLight = lights.get(i);
            if (!testLight.isShadowCaster()) {
                continue;
            }
            final ShadowVolume v = getShadowVolume(testLight);
            if (v != null) {
                if (testLight.getType() == Light.Type.Directional) {
                    final DirectionalLight dl = (DirectionalLight) testLight;
                    final ReadOnlyVector3 direction = dl.getDirection();
                    if (!v._direction.equals(direction)) {
                        v.setUpdate(true);
                        v.setDirection(direction);
                        same = false;
                    }
                } else if (testLight.getType() == Light.Type.Point) {
                    final PointLight pl = (PointLight) testLight;
                    final ReadOnlyVector3 loc = pl.getLocation();
                    if (!v._position.equals(loc)) {
                        v.setUpdate(true);
                        v.setPosition(loc);
                        same = false;
                    }

                }
            } else {
                return false;
            }
        }
        return same;
    }

    // Checks whether two edges are connected and sets triangle field if they
    // are.
    private void edgeConnected(final int face, final IntBuffer index, final int index1, final int index2,
            final ShadowEdge edge) {
        edge.p0 = index1;
        edge.p1 = index2;

        index.rewind();

        for (int t = 0; t < _maxIndex; t++) {
            if (t != face) {
                final int offset = t * 3;
                final int t0 = index.get(offset), t1 = index.get(offset + 1), t2 = index.get(offset + 2);
                if ((t0 == index1 && t1 == index2) || (t1 == index1 && t2 == index2) || (t2 == index1 && t0 == index2)
                        || (t0 == index2 && t1 == index1) || (t1 == index2 && t2 == index1)
                        || (t2 == index2 && t0 == index1)) {
                    // Edges are connected
                    edge.triangle = t;
                    return;
                }
            }
        }
    }

    /**
     * <code>recreateFaces</code> creates a triangle array for every triangle in the target occluder mesh and stores it
     * in the faces field. This is only done rarely in general.
     */
    public void recreateFaces() {
        // make a copy of the original indices
        _maxIndex = 0;
        _facing = new BitSet();
        final IntBuffer index = BufferUtils.clone(_target.getMeshData().getIndexBuffer());
        if (index == null) {
            return;
        }
        index.clear();

        // Create a ShadowTriangle object for each face
        _faces = new ArrayList<ShadowTriangle>();

        _maxIndex = index.capacity() / 3;
        _vertCount = _target.getMeshData().getVertexCount();

        // Create a bitset for holding direction flags
        _facing = new BitSet(_maxIndex);

        // Loop through all of the triangles
        for (int t = 0; t < _maxIndex; t++) {
            final ShadowTriangle tri = new ShadowTriangle();
            _faces.add(tri);
            final int offset = t * 3;
            final int t0 = index.get(offset), t1 = index.get(offset + 1), t2 = index.get(offset + 2);
            edgeConnected(t, index, t0, t1, tri.edge1);
            edgeConnected(t, index, t1, t2, tri.edge2);
            edgeConnected(t, index, t2, t0, tri.edge3);
        }
    }

    /**
     * <code>getShadowVolume</code> returns the shadow volume contained in this grouping for a particular light
     * 
     * @param light
     *            the light whose shadow volume should be returned
     * @return a shadow volume for the light or null if one does not exist
     */
    public ShadowVolume getShadowVolume(final Light light) {
        for (int v = 0, vSize = _volumes.size(); v < vSize; v++) {
            final ShadowVolume vol = _volumes.get(v);
            if (vol._light.equals(light)) {
                return vol;
            }
        }
        return null;
    }

    /**
     * @return Returns the projectionLength.
     */
    public float getProjectionLength() {
        return _projectionLength;
    }

    /**
     * @param projectionLength
     *            The projectionLength to set.
     */
    public void setProjectionLength(final float projectionLength) {
        _projectionLength = projectionLength;
        // force update of volumes
        for (int v = 0, vSize = _volumes.size(); v < vSize; v++) {
            _volumes.get(v).setUpdate(true);
        }
    }

    /**
     * @return Returns the volumes.
     */
    public List<ShadowVolume> getVolumes() {
        return _volumes;
    }

}
