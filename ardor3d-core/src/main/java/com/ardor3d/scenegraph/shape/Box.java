/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph.shape;

import java.io.IOException;
import java.nio.FloatBuffer;

import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.TexCoords;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.geom.BufferUtils;

/**
 * <code>Box</code> provides an extension of <code>Mesh</code>. A <code>Box</code> is defined by a minimal point and a
 * maximum point. The eight vertices that make the box are then computed. They are computed in such a way as to generate
 * an axis-aligned box.
 */
public class Box extends Mesh implements Savable {
    private static final long serialVersionUID = 1L;

    public double xExtent, yExtent, zExtent;

    public final Vector3 center = new Vector3(0, 0, 0);

    /**
     * instantiates a new <code>Box</code> object. All information must be applies later. For internal usage only
     */
    public Box() {
        super("temp");
    }

    /**
     * Constructor instantiates a new <code>Box</code> object. Center and vertice information must be supplied later.
     * 
     * @param name
     *            the name of the scene element. This is required for identification and comparision purposes.
     */
    public Box(final String name) {
        super(name);
    }

    /**
     * Constructor instantiates a new <code>Box</code> object. The minimum and maximum point are provided. These two
     * points define the shape and size of the box, but not it's orientation or position. You should use the
     * <code>setTranslation</code> and <code>setLocalRotation</code> for those attributes.
     * 
     * @param name
     *            the name of the scene element. This is required for identification and comparision purposes.
     * @param min
     *            the minimum point that defines the box.
     * @param max
     *            the maximum point that defines the box.
     */
    public Box(final String name, final Vector3 min, final Vector3 max) {
        super(name);
        setData(min, max);
    }

    /**
     * Constructs a new box. The box has the given center and extends in the x, y, and z out from the center (+ and -)
     * by the given amounts. So, for example, a box with extent of .5 would be the unit cube.
     * 
     * @param name
     *            Name of the box.
     * @param center
     *            Center of the box.
     * @param xExtent
     *            x extent of the box, in both directions.
     * @param yExtent
     *            y extent of the box, in both directions.
     * @param zExtent
     *            z extent of the box, in both directions.
     */
    public Box(final String name, final Vector3 center, final double xExtent, final double yExtent, final double zExtent) {
        super(name);
        setData(center, xExtent, yExtent, zExtent);
    }

    /**
     * Changes the data of the box so that the two opposite corners are minPoint and maxPoint. The other corners are
     * created from those two points. If update buffers is flagged as true, the vertex/normal/texture/color/index
     * buffers are updated when the data is changed.
     * 
     * @param minPoint
     *            The new minPoint of the box.
     * @param maxPoint
     *            The new maxPoint of the box.
     */
    public void setData(final Vector3 minPoint, final Vector3 maxPoint) {
        center.set(maxPoint).addLocal(minPoint).multiplyLocal(0.5);

        final double x = maxPoint.getX() - center.getX();
        final double y = maxPoint.getY() - center.getY();
        final double z = maxPoint.getZ() - center.getZ();
        setData(center, x, y, z);
    }

    /**
     * Changes the data of the box so that its center is <code>center</code> and it extends in the x, y, and z
     * directions by the given extent. Note that the actual sides will be 2x the given extent values because the box
     * extends in + & - from the center for each extent.
     * 
     * @param center
     *            The center of the box.
     * @param xExtent
     *            x extent of the box, in both directions.
     * @param yExtent
     *            y extent of the box, in both directions.
     * @param zExtent
     *            z extent of the box, in both directions.
     */
    public void setData(final Vector3 center, final double xExtent, final double yExtent, final double zExtent) {
        if (center != null) {
            this.center.set(center);
        }

        this.xExtent = xExtent;
        this.yExtent = yExtent;
        this.zExtent = zExtent;

        setVertexData();
        setNormalData();
        setTextureData();
        setIndexData();

    }

    /**
     * <code>setVertexData</code> sets the vertex positions that define the box. These eight points are determined from
     * the minimum and maximum point.
     */
    private void setVertexData() {
        _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(_meshData.getVertexBuffer(), 24));
        final Vector3[] vert = computeVertices(); // returns 8

        // Back
        BufferUtils.setInBuffer(vert[0], _meshData.getVertexBuffer(), 0);
        BufferUtils.setInBuffer(vert[1], _meshData.getVertexBuffer(), 1);
        BufferUtils.setInBuffer(vert[2], _meshData.getVertexBuffer(), 2);
        BufferUtils.setInBuffer(vert[3], _meshData.getVertexBuffer(), 3);

        // Right
        BufferUtils.setInBuffer(vert[1], _meshData.getVertexBuffer(), 4);
        BufferUtils.setInBuffer(vert[4], _meshData.getVertexBuffer(), 5);
        BufferUtils.setInBuffer(vert[6], _meshData.getVertexBuffer(), 6);
        BufferUtils.setInBuffer(vert[2], _meshData.getVertexBuffer(), 7);

        // Front
        BufferUtils.setInBuffer(vert[4], _meshData.getVertexBuffer(), 8);
        BufferUtils.setInBuffer(vert[5], _meshData.getVertexBuffer(), 9);
        BufferUtils.setInBuffer(vert[7], _meshData.getVertexBuffer(), 10);
        BufferUtils.setInBuffer(vert[6], _meshData.getVertexBuffer(), 11);

        // Left
        BufferUtils.setInBuffer(vert[5], _meshData.getVertexBuffer(), 12);
        BufferUtils.setInBuffer(vert[0], _meshData.getVertexBuffer(), 13);
        BufferUtils.setInBuffer(vert[3], _meshData.getVertexBuffer(), 14);
        BufferUtils.setInBuffer(vert[7], _meshData.getVertexBuffer(), 15);

        // Top
        BufferUtils.setInBuffer(vert[2], _meshData.getVertexBuffer(), 16);
        BufferUtils.setInBuffer(vert[6], _meshData.getVertexBuffer(), 17);
        BufferUtils.setInBuffer(vert[7], _meshData.getVertexBuffer(), 18);
        BufferUtils.setInBuffer(vert[3], _meshData.getVertexBuffer(), 19);

        // Bottom
        BufferUtils.setInBuffer(vert[0], _meshData.getVertexBuffer(), 20);
        BufferUtils.setInBuffer(vert[5], _meshData.getVertexBuffer(), 21);
        BufferUtils.setInBuffer(vert[4], _meshData.getVertexBuffer(), 22);
        BufferUtils.setInBuffer(vert[1], _meshData.getVertexBuffer(), 23);
    }

    /**
     * <code>setNormalData</code> sets the normals of each of the box's planes.
     */
    private void setNormalData() {
        if (_meshData.getNormalBuffer() == null) {
            _meshData.setNormalBuffer(BufferUtils.createVector3Buffer(24));

            // back
            for (int i = 0; i < 4; i++) {
                _meshData.getNormalBuffer().put(0).put(0).put(-1);
            }

            // right
            for (int i = 0; i < 4; i++) {
                _meshData.getNormalBuffer().put(1).put(0).put(0);
            }

            // front
            for (int i = 0; i < 4; i++) {
                _meshData.getNormalBuffer().put(0).put(0).put(1);
            }

            // left
            for (int i = 0; i < 4; i++) {
                _meshData.getNormalBuffer().put(-1).put(0).put(0);
            }

            // top
            for (int i = 0; i < 4; i++) {
                _meshData.getNormalBuffer().put(0).put(1).put(0);
            }

            // bottom
            for (int i = 0; i < 4; i++) {
                _meshData.getNormalBuffer().put(0).put(-1).put(0);
            }
        }
    }

    /**
     * <code>setTextureData</code> sets the points that define the texture of the box. It's a one-to-one ratio, where
     * each plane of the box has it's own copy of the texture. That is, the texture is repeated one time for each six
     * faces.
     */
    private void setTextureData() {
        if (_meshData.getTextureCoords(0) == null) {
            _meshData.setTextureCoords(new TexCoords(BufferUtils.createVector2Buffer(24)), 0);
            final FloatBuffer tex = _meshData.getTextureCoords(0)._coords;

            for (int i = 0; i < 6; i++) {
                tex.put(1).put(0);
                tex.put(0).put(0);
                tex.put(0).put(1);
                tex.put(1).put(1);
            }
        }
    }

    /**
     * <code>setIndexData</code> sets the indices into the list of vertices, defining all triangles that constitute the
     * box.
     */
    private void setIndexData() {
        if (_meshData.getIndexBuffer() == null) {
            final int[] indices = { 2, 1, 0, 3, 2, 0, 6, 5, 4, 7, 6, 4, 10, 9, 8, 11, 10, 8, 14, 13, 12, 15, 14, 12,
                    18, 17, 16, 19, 18, 16, 22, 21, 20, 23, 22, 20 };
            _meshData.setIndexBuffer(BufferUtils.createIntBuffer(indices));
        }
    }

    /**
     * <code>clone</code> creates a new Box object containing the same data as this one.
     * 
     * @return the new Box
     */
    @Override
    public Box clone() {
        final Box rVal = new Box(getName() + "_clone", center.clone(), xExtent, yExtent, zExtent);
        return rVal;
    }

    /**
     * @return a size 8 array of Vectors representing the 8 points of the box.
     */
    public Vector3[] computeVertices() {

        final Vector3 akEAxis[] = { Vector3.UNIT_X.multiply(xExtent, null), Vector3.UNIT_Y.multiply(yExtent, null),
                Vector3.UNIT_Z.multiply(zExtent, null) };

        final Vector3 rVal[] = new Vector3[8];
        rVal[0] = center.subtract(akEAxis[0], null).subtractLocal(akEAxis[1]).subtractLocal(akEAxis[2]);
        rVal[1] = center.add(akEAxis[0], null).subtractLocal(akEAxis[1]).subtractLocal(akEAxis[2]);
        rVal[2] = center.add(akEAxis[0], null).addLocal(akEAxis[1]).subtractLocal(akEAxis[2]);
        rVal[3] = center.subtract(akEAxis[0], null).addLocal(akEAxis[1]).subtractLocal(akEAxis[2]);
        rVal[4] = center.add(akEAxis[0], null).subtractLocal(akEAxis[1]).addLocal(akEAxis[2]);
        rVal[5] = center.subtract(akEAxis[0], null).subtractLocal(akEAxis[1]).addLocal(akEAxis[2]);
        rVal[6] = center.add(akEAxis[0], null).addLocal(akEAxis[1]).addLocal(akEAxis[2]);
        rVal[7] = center.subtract(akEAxis[0], null).addLocal(akEAxis[1]).addLocal(akEAxis[2]);
        return rVal;
    }

    /**
     * Returns the current center of the box.
     * 
     * @return The box's center.
     */
    public Vector3 getCenter() {
        return center;
    }

    /**
     * Sets the center of the box. Note that even though the center is set, Mesh information is not updated. In most
     * cases, you'll want to use setData()
     * 
     * @param aCenter
     *            The new center.
     */
    public void setCenter(final Vector3 aCenter) {
        center.set(aCenter);
    }

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(xExtent, "xExtent", 0);
        capsule.write(yExtent, "yExtent", 0);
        capsule.write(zExtent, "zExtent", 0);
        capsule.write(center, "center", new Vector3(Vector3.ZERO));

    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);
        xExtent = capsule.readDouble("xExtent", 0);
        yExtent = capsule.readDouble("yExtent", 0);
        zExtent = capsule.readDouble("zExtent", 0);
        center.set((Vector3) capsule.readSavable("center", new Vector3(Vector3.ZERO)));
    }
}