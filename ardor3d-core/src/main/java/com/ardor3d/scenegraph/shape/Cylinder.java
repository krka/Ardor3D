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

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.TexCoords;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

/**
 * <code>Cylinder</code> provides an extension of <code>Mesh</code>. A <code>Cylinder</code> is defined by a height and
 * radius. The center of the Cylinder is the origin.
 */
public class Cylinder extends Mesh {

    private static final long serialVersionUID = 1L;

    private int axisSamples;

    private int radialSamples;

    private double radius;
    private double radius2;

    private double height;
    private boolean closed;
    private boolean inverted;

    public Cylinder() {}

    /**
     * Creates a new Cylinder. By default its center is the origin. Usually, a higher sample number creates a better
     * looking cylinder, but at the cost of more vertex information.
     * 
     * @param name
     *            The name of this Cylinder.
     * @param axisSamples
     *            Number of triangle samples along the axis.
     * @param radialSamples
     *            Number of triangle samples along the radial.
     * @param radius
     *            The radius of the cylinder.
     * @param height
     *            The cylinder's height.
     */
    public Cylinder(final String name, final int axisSamples, final int radialSamples, final double radius,
            final double height) {
        this(name, axisSamples, radialSamples, radius, height, false);
    }

    /**
     * Creates a new Cylinder. By default its center is the origin. Usually, a higher sample number creates a better
     * looking cylinder, but at the cost of more vertex information. <br>
     * If the cylinder is closed the texture is split into axisSamples parts: top most and bottom most part is used for
     * top and bottom of the cylinder, rest of the texture for the cylinder wall. The middle of the top is mapped to
     * texture coordinates (0.5, 1), bottom to (0.5, 0). Thus you need a suited distorted texture.
     * 
     * @param name
     *            The name of this Cylinder.
     * @param axisSamples
     *            Number of triangle samples along the axis.
     * @param radialSamples
     *            Number of triangle samples along the radial.
     * @param radius
     *            The radius of the cylinder.
     * @param height
     *            The cylinder's height.
     * @param closed
     *            true to create a cylinder with top and bottom surface
     */
    public Cylinder(final String name, final int axisSamples, final int radialSamples, final double radius,
            final double height, final boolean closed) {
        this(name, axisSamples, radialSamples, radius, height, closed, false);
    }

    /**
     * Creates a new Cylinder. By default its center is the origin. Usually, a higher sample number creates a better
     * looking cylinder, but at the cost of more vertex information. <br>
     * If the cylinder is closed the texture is split into axisSamples parts: top most and bottom most part is used for
     * top and bottom of the cylinder, rest of the texture for the cylinder wall. The middle of the top is mapped to
     * texture coordinates (0.5, 1), bottom to (0.5, 0). Thus you need a suited distorted texture.
     * 
     * @param name
     *            The name of this Cylinder.
     * @param axisSamples
     *            Number of triangle samples along the axis.
     * @param radialSamples
     *            Number of triangle samples along the radial.
     * @param radius
     *            The radius of the cylinder.
     * @param height
     *            The cylinder's height.
     * @param closed
     *            true to create a cylinder with top and bottom surface
     * @param inverted
     *            true to create a cylinder that is meant to be viewed from the interior.
     */
    public Cylinder(final String name, final int axisSamples, final int radialSamples, final double radius,
            final double height, final boolean closed, final boolean inverted) {

        super(name);

        this.axisSamples = axisSamples + (closed ? 2 : 0);
        this.radialSamples = radialSamples;
        setRadius(radius);
        this.height = height;
        this.closed = closed;
        this.inverted = inverted;

        allocateVertices();
    }

    /**
     * @return Returns the height.
     */
    public double getHeight() {
        return height;
    }

    /**
     * @param height
     *            The height to set.
     */
    public void setHeight(final double height) {
        this.height = height;
        allocateVertices();
    }

    /**
     * @return Returns the radius.
     */
    public double getRadius() {
        return radius;
    }

    /**
     * Change the radius of this cylinder. This resets any second radius.
     * 
     * @param radius
     *            The radius to set.
     */
    public void setRadius(final double radius) {
        this.radius = radius;
        radius2 = radius;
        allocateVertices();
    }

    /**
     * Set the top radius of the 'cylinder' to differ from the bottom radius.
     * 
     * @param radius
     *            The first radius to set.
     * @see com.ardor3d.extension.shape.Cone
     */
    public void setRadius1(final double radius) {
        this.radius = radius;
        allocateVertices();
    }

    /**
     * Set the bottom radius of the 'cylinder' to differ from the top radius. This makes the Mesh be a frustum of
     * pyramid, or if set to 0, a cone.
     * 
     * @param radius
     *            The second radius to set.
     * @see com.ardor3d.extension.shape.Cone
     */
    public void setRadius2(final double radius) {
        radius2 = radius;
        allocateVertices();
    }

    /**
     * @return the number of samples along the cylinder axis
     */
    public int getAxisSamples() {
        return axisSamples;
    }

    /**
     * @return true if end caps are used.
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * @return true if normals and uvs are created for interior use
     */
    public boolean isInverted() {
        return inverted;
    }

    /**
     * @return number of samples around cylinder
     */
    public int getRadialSamples() {
        return radialSamples;
    }

    private void allocateVertices() {
        // allocate vertices
        final int verts = axisSamples * (radialSamples + 1) + (closed ? 2 : 0);
        _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(_meshData.getVertexBuffer(), verts));

        // allocate normals if requested
        _meshData.setNormalBuffer(BufferUtils.createVector3Buffer(_meshData.getNormalBuffer(), verts));

        // allocate texture coordinates
        _meshData.setTextureCoords(new TexCoords(BufferUtils.createVector2Buffer(verts)), 0);

        final int count = ((closed ? 2 : 0) + 2 * (axisSamples - 1)) * radialSamples;
        _meshData.setIndexBuffer(BufferUtils.createIntBuffer(_meshData.getIndexBuffer(), 3 * count));

        setGeometryData();
        setIndexData();
    }

    private void setGeometryData() {
        // generate geometry
        final double inverseRadial = 1.0 / radialSamples;
        final double inverseAxisLess = 1.0 / (closed ? axisSamples - 3 : axisSamples - 1);
        final double inverseAxisLessTexture = 1.0 / (axisSamples - 1);
        final double halfHeight = 0.5 * height;

        // Generate points on the unit circle to be used in computing the mesh
        // points on a cylinder slice.
        final double[] sin = new double[radialSamples + 1];
        final double[] cos = new double[radialSamples + 1];

        for (int radialCount = 0; radialCount < radialSamples; radialCount++) {
            final double angle = MathUtils.TWO_PI * inverseRadial * radialCount;
            cos[radialCount] = MathUtils.cos(angle);
            sin[radialCount] = MathUtils.sin(angle);
        }
        sin[radialSamples] = sin[0];
        cos[radialSamples] = cos[0];

        // generate the cylinder itself
        final Vector3 tempNormal = new Vector3();
        for (int axisCount = 0, i = 0; axisCount < axisSamples; axisCount++) {
            double axisFraction;
            double axisFractionTexture;
            int topBottom = 0;
            if (!closed) {
                axisFraction = axisCount * inverseAxisLess; // in [0,1]
                axisFractionTexture = axisFraction;
            } else {
                if (axisCount == 0) {
                    topBottom = -1; // bottom
                    axisFraction = 0;
                    axisFractionTexture = inverseAxisLessTexture;
                } else if (axisCount == axisSamples - 1) {
                    topBottom = 1; // top
                    axisFraction = 1;
                    axisFractionTexture = 1 - inverseAxisLessTexture;
                } else {
                    axisFraction = (axisCount - 1) * inverseAxisLess;
                    axisFractionTexture = axisCount * inverseAxisLessTexture;
                }
            }
            final double z = -halfHeight + height * axisFraction;

            // compute center of slice
            final Vector3 sliceCenter = new Vector3(0, 0, z);

            // compute slice vertices with duplication at end point
            final int save = i;
            for (int radialCount = 0; radialCount < radialSamples; radialCount++) {
                final double radialFraction = radialCount * inverseRadial; // in [0,1)
                tempNormal.set(cos[radialCount], sin[radialCount], 0);
                if (topBottom == 0) {
                    if (!inverted) {
                        _meshData.getNormalBuffer().put(tempNormal.getXf()).put(tempNormal.getYf()).put(
                                tempNormal.getZf());
                    } else {
                        _meshData.getNormalBuffer().put(-tempNormal.getXf()).put(-tempNormal.getYf()).put(
                                -tempNormal.getZf());
                    }
                } else {
                    _meshData.getNormalBuffer().put(0).put(0).put(topBottom * (inverted ? -1 : 1));
                }

                tempNormal.multiplyLocal((radius - radius2) * axisFraction + radius2).addLocal(sliceCenter);
                _meshData.getVertexBuffer().put(tempNormal.getXf()).put(tempNormal.getYf()).put(tempNormal.getZf());

                _meshData.getTextureCoords(0).coords.put((float) (inverted ? 1 - radialFraction : radialFraction)).put(
                        (float) axisFractionTexture);
                i++;
            }

            BufferUtils.copyInternalVector3(_meshData.getVertexBuffer(), save, i);
            BufferUtils.copyInternalVector3(_meshData.getNormalBuffer(), save, i);

            _meshData.getTextureCoords(0).coords.put((inverted ? 0.0f : 1.0f)).put((float) axisFractionTexture);

            i++;
        }

        if (closed) {
            _meshData.getVertexBuffer().put(0).put(0).put((float) -halfHeight); // bottom center
            _meshData.getNormalBuffer().put(0).put(0).put(-1 * (inverted ? -1 : 1));
            _meshData.getTextureCoords(0).coords.put(0.5f).put(0);
            _meshData.getVertexBuffer().put(0).put(0).put((float) halfHeight); // top center
            _meshData.getNormalBuffer().put(0).put(0).put(1 * (inverted ? -1 : 1));
            _meshData.getTextureCoords(0).coords.put(0.5f).put(1);
        }
    }

    private void setIndexData() {
        // generate connectivity
        for (int axisCount = 0, axisStart = 0; axisCount < axisSamples - 1; axisCount++) {
            int i0 = axisStart;
            int i1 = i0 + 1;
            axisStart += radialSamples + 1;
            int i2 = axisStart;
            int i3 = i2 + 1;
            for (int i = 0; i < radialSamples; i++) {
                if (closed && axisCount == 0) {
                    if (!inverted) {
                        _meshData.getIndexBuffer().put(i0++);
                        _meshData.getIndexBuffer().put(_meshData.getVertexCount() - 2);
                        _meshData.getIndexBuffer().put(i1++);
                    } else {
                        _meshData.getIndexBuffer().put(i0++);
                        _meshData.getIndexBuffer().put(i1++);
                        _meshData.getIndexBuffer().put(_meshData.getVertexCount() - 2);
                    }
                } else if (closed && axisCount == axisSamples - 2) {
                    if (!inverted) {
                        _meshData.getIndexBuffer().put(i2++);
                        _meshData.getIndexBuffer().put(i3++);
                        _meshData.getIndexBuffer().put(_meshData.getVertexCount() - 1);
                    } else {
                        _meshData.getIndexBuffer().put(i2++);
                        _meshData.getIndexBuffer().put(_meshData.getVertexCount() - 1);
                        _meshData.getIndexBuffer().put(i3++);
                    }
                } else {
                    if (!inverted) {
                        _meshData.getIndexBuffer().put(i0++);
                        _meshData.getIndexBuffer().put(i1);
                        _meshData.getIndexBuffer().put(i2);
                        _meshData.getIndexBuffer().put(i1++);
                        _meshData.getIndexBuffer().put(i3++);
                        _meshData.getIndexBuffer().put(i2++);
                    } else {
                        _meshData.getIndexBuffer().put(i0++);
                        _meshData.getIndexBuffer().put(i2);
                        _meshData.getIndexBuffer().put(i1);
                        _meshData.getIndexBuffer().put(i1++);
                        _meshData.getIndexBuffer().put(i2++);
                        _meshData.getIndexBuffer().put(i3++);
                    }
                }
            }
        }
    }

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(axisSamples, "axisSamples", 0);
        capsule.write(radialSamples, "radialSamples", 0);
        capsule.write(radius, "radius", 0);
        capsule.write(radius2, "radius2", 0);
        capsule.write(height, "height", 0);
        capsule.write(closed, "closed", false);
        capsule.write(inverted, "inverted", false);
    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);
        axisSamples = capsule.readInt("axisSamples", 0);
        radialSamples = capsule.readInt("radialSamples", 0);
        radius = capsule.readDouble("radius", 0);
        radius2 = capsule.readDouble("radius2", 0);
        height = capsule.readDouble("height", 0);
        closed = capsule.readBoolean("closed", false);
        inverted = capsule.readBoolean("inverted", false);
    }
}
