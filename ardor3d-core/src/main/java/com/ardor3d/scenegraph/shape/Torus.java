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

public class Torus extends Mesh {
    private static final long serialVersionUID = 1L;

    private int _circleSamples;

    private int _radialSamples;

    private double _innerRadius;

    private double _outerRadius;

    public Torus() {

    }

    /**
     * Constructs a new Torus. Center is the origin, but the Torus may be transformed.
     * 
     * @param name
     *            The name of the Torus.
     * @param circleSamples
     *            The number of samples along the circles.
     * @param radialSamples
     *            The number of samples along the radial.
     * @param innerRadius
     *            The radius of the inner begining of the Torus.
     * @param outerRadius
     *            The radius of the outter end of the Torus.
     */
    public Torus(final String name, final int circleSamples, final int radialSamples, final double innerRadius,
            final double outerRadius) {

        super(name);
        _circleSamples = circleSamples;
        _radialSamples = radialSamples;
        _innerRadius = innerRadius;
        _outerRadius = outerRadius;

        setGeometryData();
        setIndexData();

    }

    private void setGeometryData() {
        // allocate vertices
        final int verts = ((_circleSamples + 1) * (_radialSamples + 1));
        _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(verts));

        // allocate normals if requested
        _meshData.setNormalBuffer(BufferUtils.createVector3Buffer(verts));

        // allocate texture coordinates
        _meshData.setTextureCoords(new TexCoords(BufferUtils.createVector2Buffer(verts)), 0);

        // generate geometry
        final double inverseCircleSamples = 1.0 / _circleSamples;
        final double inverseRadialSamples = 1.0 / _radialSamples;
        int i = 0;
        // generate the cylinder itself
        final Vector3 radialAxis = new Vector3(), torusMiddle = new Vector3(), tempNormal = new Vector3();
        for (int circleCount = 0; circleCount < _circleSamples; circleCount++) {
            // compute center point on torus circle at specified angle
            final double circleFraction = circleCount * inverseCircleSamples;
            final double theta = MathUtils.TWO_PI * circleFraction;
            final double cosTheta = MathUtils.cos(theta);
            final double sinTheta = MathUtils.sin(theta);
            radialAxis.set(cosTheta, sinTheta, 0);
            radialAxis.multiply(_outerRadius, torusMiddle);

            // compute slice vertices with duplication at end point
            final int iSave = i;
            for (int radialCount = 0; radialCount < _radialSamples; radialCount++) {
                final double radialFraction = radialCount * inverseRadialSamples;
                // in [0,1)
                final double phi = MathUtils.TWO_PI * radialFraction;
                final double cosPhi = MathUtils.cos(phi);
                final double sinPhi = MathUtils.sin(phi);
                tempNormal.set(radialAxis).multiplyLocal(cosPhi);
                tempNormal.setZ(tempNormal.getZ() + sinPhi);
                if (true) {
                    _meshData.getNormalBuffer().put((float) tempNormal.getX()).put((float) tempNormal.getY()).put(
                            (float) tempNormal.getZ());
                } else {
                    _meshData.getNormalBuffer().put((float) -tempNormal.getX()).put((float) -tempNormal.getY()).put(
                            (float) -tempNormal.getZ());
                }

                tempNormal.multiplyLocal(_innerRadius).addLocal(torusMiddle);
                _meshData.getVertexBuffer().put((float) tempNormal.getX()).put((float) tempNormal.getY()).put(
                        (float) tempNormal.getZ());

                _meshData.getTextureCoords(0)._coords.put((float) radialFraction).put((float) circleFraction);
                i++;
            }

            BufferUtils.copyInternalVector3(_meshData.getVertexBuffer(), iSave, i);
            BufferUtils.copyInternalVector3(_meshData.getNormalBuffer(), iSave, i);

            _meshData.getTextureCoords(0)._coords.put(1.0f).put((float) circleFraction);

            i++;
        }

        // duplicate the cylinder ends to form a torus
        for (int iR = 0; iR <= _radialSamples; iR++, i++) {
            BufferUtils.copyInternalVector3(_meshData.getVertexBuffer(), iR, i);
            BufferUtils.copyInternalVector3(_meshData.getNormalBuffer(), iR, i);
            BufferUtils.copyInternalVector2(_meshData.getTextureCoords(0)._coords, iR, i);
            _meshData.getTextureCoords(0)._coords.put(i * 2 + 1, 1.0f);
        }
    }

    private void setIndexData() {
        // allocate connectivity
        final int tris = (2 * _circleSamples * _radialSamples);
        _meshData.setIndexBuffer(BufferUtils.createIntBuffer(3 * tris));
        int i;
        // generate connectivity
        int connectionStart = 0;
        int index = 0;
        for (int circleCount = 0; circleCount < _circleSamples; circleCount++) {
            int i0 = connectionStart;
            int i1 = i0 + 1;
            connectionStart += _radialSamples + 1;
            int i2 = connectionStart;
            int i3 = i2 + 1;
            for (i = 0; i < _radialSamples; i++, index += 6) {
                if (true) {
                    _meshData.getIndexBuffer().put(i0++);
                    _meshData.getIndexBuffer().put(i2);
                    _meshData.getIndexBuffer().put(i1);
                    _meshData.getIndexBuffer().put(i1++);
                    _meshData.getIndexBuffer().put(i2++);
                    _meshData.getIndexBuffer().put(i3++);
                } else {
                    _meshData.getIndexBuffer().put(i0++);
                    _meshData.getIndexBuffer().put(i1);
                    _meshData.getIndexBuffer().put(i2);
                    _meshData.getIndexBuffer().put(i1++);
                    _meshData.getIndexBuffer().put(i3++);
                    _meshData.getIndexBuffer().put(i2++);
                }
            }
        }
    }

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(_circleSamples, "circleSamples", 0);
        capsule.write(_radialSamples, "radialSamples", 0);
        capsule.write(_innerRadius, "innerRadius", 0);
        capsule.write(_outerRadius, "outerRadius", 0);
    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);
        _circleSamples = capsule.readInt("circleSamples", 0);
        _radialSamples = capsule.readInt("radialSamples", 0);
        _innerRadius = capsule.readDouble("innerRadius", 0);
        _outerRadius = capsule.readDouble("outerRaidus", 0);
    }

}