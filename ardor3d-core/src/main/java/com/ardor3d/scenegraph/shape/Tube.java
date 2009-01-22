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
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.TexCoords;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.geom.BufferUtils;

public class Tube extends Mesh implements Savable {

    private static final long serialVersionUID = 1L;

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    private int axisSamples;
    private int radialSamples;

    private double outerRadius;
    private double innerRadius;
    private double height;

    /**
     * Constructor meant for Savable use only.
     */
    public Tube() {}

    public Tube(final String name, final double outerRadius, final double innerRadius, final double height,
            final int axisSamples, final int radialSamples) {
        super(name);
        this.outerRadius = outerRadius;
        this.innerRadius = innerRadius;
        this.height = height;
        this.axisSamples = axisSamples;
        this.radialSamples = radialSamples;
        allocateVertices();
    }

    public Tube(final String name, final double outerRadius, final double innerRadius, final double height) {
        this(name, outerRadius, innerRadius, height, 2, 20);
    }

    private void allocateVertices() {
        final int verts = (2 * (axisSamples + 1) * (radialSamples + 1) + radialSamples * 4);
        _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(_meshData.getVertexBuffer(), verts));
        _meshData.setNormalBuffer(BufferUtils.createVector3Buffer(_meshData.getNormalBuffer(), verts));
        _meshData.setTextureCoords(new TexCoords(BufferUtils.createVector2Buffer(verts)), 0);

        final int tris = (4 * radialSamples * (1 + axisSamples));
        _meshData.setIndexBuffer(BufferUtils.createIntBuffer(_meshData.getIndexBuffer(), 3 * tris));

        setGeometryData();
        setIndexData();
    }

    public int getAxisSamples() {
        return axisSamples;
    }

    public void setAxisSamples(final int axisSamples) {
        this.axisSamples = axisSamples;
        allocateVertices();
    }

    public int getRadialSamples() {
        return radialSamples;
    }

    public void setRadialSamples(final int radialSamples) {
        this.radialSamples = radialSamples;
        allocateVertices();
    }

    public double getOuterRadius() {
        return outerRadius;
    }

    public void setOuterRadius(final double outerRadius) {
        this.outerRadius = outerRadius;
        allocateVertices();
    }

    public double getInnerRadius() {
        return innerRadius;
    }

    public void setInnerRadius(final double innerRadius) {
        this.innerRadius = innerRadius;
        allocateVertices();
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(final double height) {
        this.height = height;
        allocateVertices();
    }

    private void setGeometryData() {
        final double inverseRadial = 1.0 / radialSamples;
        final double axisStep = height / axisSamples;
        final double axisTextureStep = 1.0 / axisSamples;
        final double halfHeight = 0.5 * height;
        final double innerOuterRatio = innerRadius / outerRadius;
        final double[] sin = new double[radialSamples];
        final double[] cos = new double[radialSamples];

        for (int radialCount = 0; radialCount < radialSamples; radialCount++) {
            final double angle = MathUtils.TWO_PI * inverseRadial * radialCount;
            cos[radialCount] = MathUtils.cos(angle);
            sin[radialCount] = MathUtils.sin(angle);
        }

        // outer cylinder
        for (int radialCount = 0; radialCount < radialSamples + 1; radialCount++) {
            for (int axisCount = 0; axisCount < axisSamples + 1; axisCount++) {
                _meshData.getVertexBuffer().put((float) (cos[radialCount % radialSamples] * outerRadius)).put(
                        (float) (axisStep * axisCount - halfHeight)).put(
                        (float) (sin[radialCount % radialSamples] * outerRadius));
                _meshData.getNormalBuffer().put((float) cos[radialCount % radialSamples]).put(0).put(
                        (float) sin[radialCount % radialSamples]);
                _meshData.getTextureCoords(0).coords.put((float) (radialCount * inverseRadial)).put(
                        (float) (axisTextureStep * axisCount));
            }
        }
        // inner cylinder
        for (int radialCount = 0; radialCount < radialSamples + 1; radialCount++) {
            for (int axisCount = 0; axisCount < axisSamples + 1; axisCount++) {
                _meshData.getVertexBuffer().put((float) (cos[radialCount % radialSamples] * innerRadius)).put(
                        (float) (axisStep * axisCount - halfHeight)).put(
                        (float) (sin[radialCount % radialSamples] * innerRadius));
                _meshData.getNormalBuffer().put((float) -cos[radialCount % radialSamples]).put(0).put(
                        (float) -sin[radialCount % radialSamples]);
                _meshData.getTextureCoords(0).coords.put((float) (radialCount * inverseRadial)).put(
                        (float) (axisTextureStep * axisCount));
            }
        }
        // bottom edge
        for (int radialCount = 0; radialCount < radialSamples; radialCount++) {
            _meshData.getVertexBuffer().put((float) (cos[radialCount] * outerRadius)).put((float) -halfHeight).put(
                    (float) (sin[radialCount] * outerRadius));
            _meshData.getVertexBuffer().put((float) (cos[radialCount] * innerRadius)).put((float) -halfHeight).put(
                    (float) (sin[radialCount] * innerRadius));
            _meshData.getNormalBuffer().put(0).put(-1).put(0);
            _meshData.getNormalBuffer().put(0).put(-1).put(0);
            _meshData.getTextureCoords(0).coords.put((float) (0.5 + 0.5 * cos[radialCount])).put(
                    (float) (0.5 + 0.5 * sin[radialCount]));
            _meshData.getTextureCoords(0).coords.put((float) (0.5 + innerOuterRatio * 0.5 * cos[radialCount])).put(
                    (float) (0.5 + innerOuterRatio * 0.5 * sin[radialCount]));
        }
        // top edge
        for (int radialCount = 0; radialCount < radialSamples; radialCount++) {
            _meshData.getVertexBuffer().put((float) (cos[radialCount] * outerRadius)).put((float) halfHeight).put(
                    (float) (sin[radialCount] * outerRadius));
            _meshData.getVertexBuffer().put((float) (cos[radialCount] * innerRadius)).put((float) halfHeight).put(
                    (float) (sin[radialCount] * innerRadius));
            _meshData.getNormalBuffer().put(0).put(1).put(0);
            _meshData.getNormalBuffer().put(0).put(1).put(0);
            _meshData.getTextureCoords(0).coords.put((float) (0.5 + 0.5 * cos[radialCount])).put(
                    (float) (0.5 + 0.5 * sin[radialCount]));
            _meshData.getTextureCoords(0).coords.put((float) (0.5 + innerOuterRatio * 0.5 * cos[radialCount])).put(
                    (float) (0.5 + innerOuterRatio * 0.5 * sin[radialCount]));
        }

    }

    private void setIndexData() {
        final int innerCylinder = (axisSamples + 1) * (radialSamples + 1);
        final int bottomEdge = 2 * innerCylinder;
        final int topEdge = bottomEdge + 2 * radialSamples;
        // outer cylinder
        for (int radialCount = 0; radialCount < radialSamples; radialCount++) {
            for (int axisCount = 0; axisCount < axisSamples; axisCount++) {
                final int index0 = axisCount + (axisSamples + 1) * radialCount;
                final int index1 = index0 + 1;
                final int index2 = index0 + (axisSamples + 1);
                final int index3 = index2 + 1;
                _meshData.getIndexBuffer().put(index0).put(index1).put(index2);
                _meshData.getIndexBuffer().put(index1).put(index3).put(index2);
            }
        }

        // inner cylinder
        for (int radialCount = 0; radialCount < radialSamples; radialCount++) {
            for (int axisCount = 0; axisCount < axisSamples; axisCount++) {
                final int index0 = innerCylinder + axisCount + (axisSamples + 1) * radialCount;
                final int index1 = index0 + 1;
                final int index2 = index0 + (axisSamples + 1);
                final int index3 = index2 + 1;
                _meshData.getIndexBuffer().put(index0).put(index2).put(index1);
                _meshData.getIndexBuffer().put(index1).put(index2).put(index3);
            }
        }

        // bottom edge
        for (int radialCount = 0; radialCount < radialSamples; radialCount++) {
            final int index0 = bottomEdge + 2 * radialCount;
            final int index1 = index0 + 1;
            final int index2 = bottomEdge + 2 * ((radialCount + 1) % radialSamples);
            final int index3 = index2 + 1;
            _meshData.getIndexBuffer().put(index0).put(index2).put(index1);
            _meshData.getIndexBuffer().put(index1).put(index2).put(index3);
        }

        // top edge
        for (int radialCount = 0; radialCount < radialSamples; radialCount++) {
            final int index0 = topEdge + 2 * radialCount;
            final int index1 = index0 + 1;
            final int index2 = topEdge + 2 * ((radialCount + 1) % radialSamples);
            final int index3 = index2 + 1;
            _meshData.getIndexBuffer().put(index0).put(index1).put(index2);
            _meshData.getIndexBuffer().put(index1).put(index3).put(index2);
        }
    }

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(getAxisSamples(), "axisSamples", 0);
        capsule.write(getRadialSamples(), "radialSamples", 0);
        capsule.write(getOuterRadius(), "outerRadius", 0);
        capsule.write(getInnerRadius(), "innerRadius", 0);
        capsule.write(getHeight(), "height", 0);
    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);
        setAxisSamples(capsule.readInt("axisSamples", 0));
        setRadialSamples(capsule.readInt("radialSamples", 0));
        setOuterRadius(capsule.readDouble("outerRadius", 0));
        setInnerRadius(capsule.readDouble("innerRadius", 0));
        setHeight(capsule.readDouble("height", 0));
    }
}