/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.shape;

import java.io.IOException;
import java.nio.IntBuffer;

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
 * PQTorus generates the geometry of a parameterized torus, also known as a pq torus.
 */
public class PQTorus extends Mesh {

    private static final long serialVersionUID = 1L;

    private double p, q;

    private double radius, width;

    private int steps, radialSamples;

    public PQTorus() {}

    /**
     * Creates a parameterized torus. Steps and radialSamples are both degree of accuracy values.
     * 
     * @param name
     *            The name of the torus.
     * @param p
     *            The x/z oscillation.
     * @param q
     *            The y oscillation.
     * @param radius
     *            The radius of the PQTorus.
     * @param width
     *            The width of the torus.
     * @param steps
     *            The steps along the torus.
     * @param radialSamples
     *            Radial samples for the torus.
     */
    public PQTorus(final String name, final double p, final double q, final double radius, final double width,
            final int steps, final int radialSamples) {
        super(name);

        this.p = p;
        this.q = q;
        this.radius = radius;
        this.width = width;
        this.steps = steps;
        this.radialSamples = radialSamples;

        setGeometryData();
        setIndexData();
    }

    private void setGeometryData() {
        final double THETA_STEP = (MathUtils.TWO_PI / steps);
        final double BETA_STEP = (MathUtils.TWO_PI / radialSamples);

        final Vector3[] toruspoints = new Vector3[steps];
        // allocate vertices
        final int verts = radialSamples * steps;
        _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(verts));

        // allocate normals if requested
        _meshData.setNormalBuffer(BufferUtils.createVector3Buffer(verts));

        // allocate texture coordinates
        _meshData.setTextureCoords(new TexCoords(BufferUtils.createVector2Buffer(verts)), 0);

        final Vector3 pointB = new Vector3();
        final Vector3 T = new Vector3(), N = new Vector3(), B = new Vector3();
        final Vector3 tempNorm = new Vector3();
        double r, x, y, z, theta = 0.0f, beta = 0.0f;
        int nvertex = 0;

        // Move along the length of the pq torus
        for (int i = 0; i < steps; i++) {
            theta += THETA_STEP;
            final double circleFraction = ((double) i) / (double) steps;

            // Find the point on the torus
            r = (0.5f * (2.0f + MathUtils.sin(q * theta)) * radius);
            x = (r * MathUtils.cos(p * theta) * radius);
            y = (r * MathUtils.sin(p * theta) * radius);
            z = (r * MathUtils.cos(q * theta) * radius);
            toruspoints[i] = new Vector3(x, y, z);

            // Now find a point slightly farther along the torus
            r = (0.5f * (2.0f + MathUtils.sin(q * (theta + 0.01f))) * radius);
            x = (r * MathUtils.cos(p * (theta + 0.01f)) * radius);
            y = (r * MathUtils.sin(p * (theta + 0.01f)) * radius);
            z = (r * MathUtils.cos(q * (theta + 0.01f)) * radius);
            pointB.set(x, y, z);

            // Approximate the Frenet Frame
            pointB.subtract(toruspoints[i], T);
            toruspoints[i].add(pointB, N);
            T.cross(N, B);
            B.cross(T, N);

            // Normalize the two vectors before use
            N.normalizeLocal();
            B.normalizeLocal();

            // Create a circle oriented by these new vectors
            beta = 0.0f;
            for (int j = 0; j < radialSamples; j++) {
                beta += BETA_STEP;
                final double cx = MathUtils.cos(beta) * width;
                final double cy = MathUtils.sin(beta) * width;
                final double radialFraction = ((double) j) / radialSamples;
                tempNorm.setX((cx * N.getX() + cy * B.getX()));
                tempNorm.setY((cx * N.getY() + cy * B.getY()));
                tempNorm.setZ((cx * N.getZ() + cy * B.getZ()));

                _meshData.getNormalBuffer().put((float) tempNorm.getX()).put((float) tempNorm.getY()).put(
                        (float) tempNorm.getZ());

                tempNorm.addLocal(toruspoints[i]);
                _meshData.getVertexBuffer().put((float) tempNorm.getX()).put((float) tempNorm.getY()).put(
                        (float) tempNorm.getZ());

                _meshData.getTextureCoords(0).coords.put((float) radialFraction).put((float) circleFraction);

                nvertex++;
            }
        }
    }

    private void setIndexData() {
        final IntBuffer indices = BufferUtils.createIntBuffer(6 * _meshData.getVertexCount());

        for (int i = 0; i < _meshData.getVertexCount(); i++) {
            indices.put(i);
            indices.put(i - radialSamples);
            indices.put(i + 1);

            indices.put(i + 1);
            indices.put(i - radialSamples);
            indices.put(i - radialSamples + 1);
        }

        for (int i = 0, len = indices.capacity(); i < len; i++) {
            int ind = indices.get(i);
            if (ind < 0) {
                ind += _meshData.getVertexCount();
                indices.put(i, ind);
            }
            if (ind >= _meshData.getVertexCount()) {
                ind -= _meshData.getVertexCount();
                indices.put(i, ind);
            }
        }
        indices.rewind();

        _meshData.setIndexBuffer(indices);
    }

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(p, "p", 0);
        capsule.write(q, "q", 0);
        capsule.write(radius, "radius", 0);
        capsule.write(width, "width", 0);
        capsule.write(steps, "steps", 0);
        capsule.write(radialSamples, "radialSamples", 0);

    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);
        p = capsule.readDouble("p", 0);
        q = capsule.readDouble("q", 0);
        radius = capsule.readDouble("radius", 0);
        width = capsule.readDouble("width", 0);
        steps = capsule.readInt("steps", 0);
        radialSamples = capsule.readInt("radialSamples", 0);

    }
}