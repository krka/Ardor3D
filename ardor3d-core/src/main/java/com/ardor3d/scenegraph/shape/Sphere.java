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
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.TexCoords;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

/**
 * Sphere represents a 3D object with all points equi-distance from a center point.
 */
public class Sphere extends Mesh {
    private static final long serialVersionUID = 1L;

    public static final int TEX_ORIGINAL = 0;

    // Spherical projection mode, donated by Ogli
    public static final int TEX_PROJECTED = 1;

    protected int _zSamples;

    protected int _radialSamples;

    /** the distance from the center point each point falls on */
    public double _radius;
    /** the center of the sphere */
    public final Vector3 _center = new Vector3();

    protected int _textureMode = TEX_ORIGINAL;

    public Sphere() {}

    /**
     * Constructs a sphere. By default the Sphere has not geometry data or center.
     * 
     * @param name
     *            The name of the sphere.
     */
    public Sphere(final String name) {
        super(name);
    }

    /**
     * Constructs a sphere with center at the origin. For details, see the other constructor.
     * 
     * @param name
     *            Name of sphere.
     * @param zSamples
     *            The samples along the Z.
     * @param radialSamples
     *            The samples along the radial.
     * @param radius
     *            Radius of the sphere.
     * @see #Sphere(java.lang.String, com.ardor3d.math.Vector3, int, int, double)
     */
    public Sphere(final String name, final int zSamples, final int radialSamples, final double radius) {
        this(name, new Vector3(0, 0, 0), zSamples, radialSamples, radius);
    }

    /**
     * Constructs a sphere. All geometry data buffers are updated automatically. Both zSamples and radialSamples
     * increase the quality of the generated sphere.
     * 
     * @param name
     *            Name of the sphere.
     * @param center
     *            Center of the sphere.
     * @param zSamples
     *            The number of samples along the Z.
     * @param radialSamples
     *            The number of samples along the radial.
     * @param radius
     *            The radius of the sphere.
     */
    public Sphere(final String name, final Vector3 center, final int zSamples, final int radialSamples,
            final double radius) {
        super(name);
        setData(center, zSamples, radialSamples, radius);
    }

    /**
     * Changes the information of the sphere into the given values.
     * 
     * @param center
     *            The new center of the sphere.
     * @param zSamples
     *            The new number of zSamples of the sphere.
     * @param radialSamples
     *            The new number of radial samples of the sphere.
     * @param radius
     *            The new radius of the sphere.
     */
    public void setData(final ReadOnlyVector3 center, final int zSamples, final int radialSamples, final double radius) {
        _center.set(center);
        _zSamples = zSamples;
        _radialSamples = radialSamples;
        _radius = radius;

        setGeometryData();
        setIndexData();
    }

    /**
     * builds the vertices based on the radius, center and radial and zSamples.
     */
    private void setGeometryData() {
        // allocate vertices
        final int verts = (_zSamples - 2) * (_radialSamples + 1) + 2;
        _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(_meshData.getVertexBuffer(), verts));

        // allocate normals if requested
        _meshData.setNormalBuffer(BufferUtils.createVector3Buffer(_meshData.getNormalBuffer(), verts));

        // allocate texture coordinates
        _meshData.setTextureCoords(new TexCoords(BufferUtils.createVector2Buffer(verts)), 0);

        // generate geometry
        final double fInvRS = 1.0 / _radialSamples;
        final double fZFactor = 2.0 / (_zSamples - 1);

        // Generate points on the unit circle to be used in computing the mesh
        // points on a sphere slice.
        final double[] afSin = new double[(_radialSamples + 1)];
        final double[] afCos = new double[(_radialSamples + 1)];
        for (int iR = 0; iR < _radialSamples; iR++) {
            final double fAngle = MathUtils.TWO_PI * fInvRS * iR;
            afCos[iR] = MathUtils.cos(fAngle);
            afSin[iR] = MathUtils.sin(fAngle);
        }
        afSin[_radialSamples] = afSin[0];
        afCos[_radialSamples] = afCos[0];

        // generate the sphere itself
        int i = 0;
        final Vector3 tempVa = Vector3.fetchTempInstance();
        final Vector3 tempVb = Vector3.fetchTempInstance();
        final Vector3 tempVc = Vector3.fetchTempInstance();
        for (int iZ = 1; iZ < (_zSamples - 1); iZ++) {
            final double fZFraction = -1.0 + fZFactor * iZ; // in (-1,1)
            final double fZ = _radius * fZFraction;

            // compute center of slice
            final Vector3 kSliceCenter = tempVb.set(_center);
            kSliceCenter.setZ(kSliceCenter.getZ() + fZ);

            // compute radius of slice
            final double fSliceRadius = Math.sqrt(Math.abs(_radius * _radius - fZ * fZ));

            // compute slice vertices with duplication at end point
            Vector3 kNormal;
            final int iSave = i;
            for (int iR = 0; iR < _radialSamples; iR++) {
                final double fRadialFraction = iR * fInvRS; // in [0,1)
                final Vector3 kRadial = tempVc.set(afCos[iR], afSin[iR], 0);
                kRadial.multiply(fSliceRadius, tempVa);
                _meshData.getVertexBuffer().put((float) (kSliceCenter.getX() + tempVa.getX())).put(
                        (float) (kSliceCenter.getY() + tempVa.getY())).put(
                        (float) (kSliceCenter.getZ() + tempVa.getZ()));

                BufferUtils.populateFromBuffer(tempVa, _meshData.getVertexBuffer(), i);
                kNormal = tempVa.subtractLocal(_center);
                kNormal.normalizeLocal();
                if (true) {
                    _meshData.getNormalBuffer().put(kNormal.getXf()).put(kNormal.getYf()).put(kNormal.getZf());
                } else {
                    _meshData.getNormalBuffer().put(-kNormal.getXf()).put(-kNormal.getYf()).put(-kNormal.getZf());
                }

                if (_textureMode == TEX_ORIGINAL) {
                    _meshData.getTextureCoords(0).coords.put((float) fRadialFraction).put(
                            (float) (0.5 * (fZFraction + 1.0)));
                } else if (_textureMode == TEX_PROJECTED) {
                    _meshData.getTextureCoords(0).coords.put((float) fRadialFraction).put(
                            (float) (MathUtils.INV_PI * (MathUtils.HALF_PI + Math.asin(fZFraction))));
                }

                i++;
            }

            BufferUtils.copyInternalVector3(_meshData.getVertexBuffer(), iSave, i);
            BufferUtils.copyInternalVector3(_meshData.getNormalBuffer(), iSave, i);

            if (_textureMode == TEX_ORIGINAL) {
                _meshData.getTextureCoords(0).coords.put(1.0f).put((float) (0.5 * (fZFraction + 1.0)));
            } else if (_textureMode == TEX_PROJECTED) {
                _meshData.getTextureCoords(0).coords.put(1.0f).put(
                        (float) (MathUtils.INV_PI * (MathUtils.HALF_PI + Math.asin(fZFraction))));
            }

            i++;
        }

        // south pole
        _meshData.getVertexBuffer().position(i * 3);
        _meshData.getVertexBuffer().put(_center.getXf()).put(_center.getYf()).put((float) (_center.getZ() - _radius));

        _meshData.getNormalBuffer().position(i * 3);
        if (true) {
            // TODO: allow for inner texture orientation later.
            _meshData.getNormalBuffer().put(0).put(0).put(-1);
        } else {
            _meshData.getNormalBuffer().put(0).put(0).put(1);
        }

        _meshData.getTextureCoords(0).coords.position(i * 2);
        _meshData.getTextureCoords(0).coords.put(0.5f).put(0);

        i++;

        // north pole
        _meshData.getVertexBuffer().put(_center.getXf()).put(_center.getYf()).put((float) (_center.getZ() + _radius));

        if (true) {
            _meshData.getNormalBuffer().put(0).put(0).put(1);
        } else {
            _meshData.getNormalBuffer().put(0).put(0).put(-1);
        }

        _meshData.getTextureCoords(0).coords.put(0.5f).put(1);
        Vector3.releaseTempInstance(tempVa);
        Vector3.releaseTempInstance(tempVb);
        Vector3.releaseTempInstance(tempVc);
    }

    /**
     * sets the indices for rendering the sphere.
     */
    private void setIndexData() {
        // allocate connectivity
        final int tris = 2 * (_zSamples - 2) * _radialSamples;
        _meshData.setIndexBuffer(BufferUtils.createIntBuffer(3 * tris));

        // generate connectivity
        int index = 0;
        for (int iZ = 0, iZStart = 0; iZ < (_zSamples - 3); iZ++) {
            int i0 = iZStart;
            int i1 = i0 + 1;
            iZStart += (_radialSamples + 1);
            int i2 = iZStart;
            int i3 = i2 + 1;
            for (int i = 0; i < _radialSamples; i++, index += 6) {
                if (true) {
                    _meshData.getIndexBuffer().put(i0++);
                    _meshData.getIndexBuffer().put(i1);
                    _meshData.getIndexBuffer().put(i2);
                    _meshData.getIndexBuffer().put(i1++);
                    _meshData.getIndexBuffer().put(i3++);
                    _meshData.getIndexBuffer().put(i2++);
                } else // inside view
                {
                    _meshData.getIndexBuffer().put(i0++);
                    _meshData.getIndexBuffer().put(i2);
                    _meshData.getIndexBuffer().put(i1);
                    _meshData.getIndexBuffer().put(i1++);
                    _meshData.getIndexBuffer().put(i2++);
                    _meshData.getIndexBuffer().put(i3++);
                }
            }
        }

        // south pole triangles
        for (int i = 0; i < _radialSamples; i++, index += 3) {
            if (true) {
                _meshData.getIndexBuffer().put(i);
                _meshData.getIndexBuffer().put(_meshData.getVertexCount() - 2);
                _meshData.getIndexBuffer().put(i + 1);
            } else // inside view
            {
                _meshData.getIndexBuffer().put(i);
                _meshData.getIndexBuffer().put(i + 1);
                _meshData.getIndexBuffer().put(_meshData.getVertexCount() - 2);
            }
        }

        // north pole triangles
        final int iOffset = (_zSamples - 3) * (_radialSamples + 1);
        for (int i = 0; i < _radialSamples; i++, index += 3) {
            if (true) {
                _meshData.getIndexBuffer().put(i + iOffset);
                _meshData.getIndexBuffer().put(i + 1 + iOffset);
                _meshData.getIndexBuffer().put(_meshData.getVertexCount() - 1);
            } else // inside view
            {
                _meshData.getIndexBuffer().put(i + iOffset);
                _meshData.getIndexBuffer().put(_meshData.getVertexCount() - 1);
                _meshData.getIndexBuffer().put(i + 1 + iOffset);
            }
        }
    }

    /**
     * Returns the center of this sphere.
     * 
     * @return The sphere's center.
     */
    public Vector3 getCenter() {
        return _center;
    }

    /**
     * Sets the center of this sphere. Note that other information (such as geometry buffers and actual vertex
     * information) is not changed. In most cases, you'll want to use setData()
     * 
     * @param aCenter
     *            The new center.
     * @see #setData
     */
    public void setCenter(final Vector3 aCenter) {
        _center.set(aCenter);
    }

    /**
     * @return Returns the textureMode.
     */
    public int getTextureMode() {
        return _textureMode;
    }

    /**
     * @param textureMode
     *            The textureMode to set.
     */
    public void setTextureMode(final int textureMode) {
        _textureMode = textureMode;
        setGeometryData();
    }

    public double getRadius() {
        return _radius;
    }

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(_zSamples, "zSamples", 0);
        capsule.write(_radialSamples, "radialSamples", 0);
        capsule.write(_radius, "radius", 0);
        capsule.write(_center, "center", new Vector3(Vector3.ZERO));
        capsule.write(_textureMode, "textureMode", TEX_ORIGINAL);
    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);
        _zSamples = capsule.readInt("zSamples", 0);
        _radialSamples = capsule.readInt("radialSamples", 0);
        _radius = capsule.readDouble("radius", 0);
        _center.set((Vector3) capsule.readSavable("center", new Vector3(Vector3.ZERO)));
        _textureMode = capsule.readInt("textureMode", TEX_ORIGINAL);
    }
}