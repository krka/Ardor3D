/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph;

import java.io.IOException;
import java.nio.FloatBuffer;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

/**
 * <code>Point</code> defines a collection of vertices that are rendered as single points.
 */
public class Point extends Mesh {

    private static final long serialVersionUID = 1L;

    private float _pointSize = 1.0f;
    private boolean _antialiased = false;

    public Point() {
        this("point", null, null, null, (FloatBufferData) null);
    }

    /**
     * Constructor instantiates a new <code>Point</code> object with a given set of data. Any data may be null, except
     * the vertex array. If this is null an exception is thrown.
     * 
     * @param name
     *            the name of the scene element. This is required for identification and comparision purposes.
     * @param vertex
     *            the vertices or points.
     * @param normal
     *            the normals of the points.
     * @param color
     *            the color of the points.
     * @param texture
     *            the texture coordinates of the points.
     */
    public Point(final String name, final Vector3[] vertex, final Vector3[] normal, final ColorRGBA[] color,
            final Vector2[] texture) {
        super(name);
        reconstruct(BufferUtils.createFloatBuffer(vertex), BufferUtils.createFloatBuffer(normal), BufferUtils
                .createFloatBuffer(color), FloatBufferDataUtil.makeNew(texture));
        _meshData.setIndexMode(IndexMode.Points);
    }

    /**
     * Constructor instantiates a new <code>Point</code> object with a given set of data. Any data may be null, except
     * the vertex array. If this is null an exception is thrown.
     * 
     * @param name
     *            the name of the scene element. This is required for identification and comparision purposes.
     * @param vertex
     *            the vertices or points.
     * @param normal
     *            the normals of the points.
     * @param color
     *            the color of the points.
     * @param coords
     *            the texture coordinates of the points.
     */
    public Point(final String name, final FloatBuffer vertex, final FloatBuffer normal, final FloatBuffer color,
            final FloatBufferData coords) {
        super(name);
        reconstruct(vertex, normal, color, coords);
        _meshData.setIndexMode(IndexMode.Points);
    }

    /**
     * @return true if points are to be drawn antialiased
     */
    public boolean isAntialiased() {
        return _antialiased;
    }

    /**
     * Sets whether the point should be antialiased. May decrease performance. If you want to enabled antialiasing, you
     * should also use an alphastate with a source of SourceFunction.SourceAlpha and a destination of
     * DB_ONE_MINUS_SRC_ALPHA or DB_ONE.
     * 
     * @param antialiased
     *            true if the line should be antialiased.
     */
    public void setAntialiased(final boolean antialiased) {
        _antialiased = antialiased;
    }

    /**
     * @return the pixel size of each point.
     */
    public float getPointSize() {
        return _pointSize;
    }

    /**
     * Sets the pixel width of the point when drawn. Non anti-aliased point sizes are rounded to the nearest whole
     * number by opengl.
     * 
     * @param size
     *            The size to set.
     */
    public void setPointSize(final float size) {
        _pointSize = size;
    }

    /**
     * Used with Serialization. Do not call this directly.
     * 
     * @param s
     * @throws IOException
     * @see java.io.Serializable
     */
    private void writeObject(final java.io.ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
    }

    /**
     * Used with Serialization. Do not call this directly.
     * 
     * @param s
     * @throws IOException
     * @throws ClassNotFoundException
     * @see java.io.Serializable
     */
    private void readObject(final java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
    }

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(_pointSize, "pointSize", 1);
        capsule.write(_antialiased, "antialiased", false);
    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);
        _pointSize = capsule.readFloat("pointSize", 1);
        _antialiased = capsule.readBoolean("antialiased", false);
    }

    @Override
    public void render(final Renderer renderer) {
        renderer.setupPointParameters(getPointSize(), isAntialiased());

        super.render(renderer);
    }

}
