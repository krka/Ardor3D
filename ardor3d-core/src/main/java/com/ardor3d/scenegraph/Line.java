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
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

public class Line extends Mesh {

    private static final long serialVersionUID = 1L;

    private float lineWidth = 1.0f;
    private short stipplePattern = (short) 0xFFFF;
    private int stippleFactor = 1;
    private boolean antialiased = false;

    public Line() {

    }

    /**
     * Constructs a new line with the given name. By default, the line has no information.
     * 
     * @param name
     *            The name of the line.
     */
    public Line(final String name) {
        super(name);

        _meshData.setIndexMode(IndexMode.Lines);
    }

    /**
     * Constructor instantiates a new <code>Line</code> object with a given set of data. Any data can be null except for
     * the vertex list. If vertices are null an exception will be thrown.
     * 
     * @param name
     *            the name of the scene element. This is required for identification and comparision purposes.
     * @param vertex
     *            the vertices that make up the lines.
     * @param normal
     *            the normals of the lines.
     * @param color
     *            the color of each point of the lines.
     * @param coords
     *            the texture coordinates of the lines.
     */
    public Line(final String name, final FloatBuffer vertex, final FloatBuffer normal, final FloatBuffer color,
            final TexCoords coords) {
        super(name);
        reconstruct(vertex, normal, color, coords);
        _meshData.setIndexMode(IndexMode.Lines);
    }

    /**
     * Constructor instantiates a new <code>Line</code> object with a given set of data. Any data can be null except for
     * the vertex list. If vertices are null an exception will be thrown.
     * 
     * @param name
     *            the name of the scene element. This is required for identification and comparision purposes.
     * @param vertex
     *            the vertices that make up the lines.
     * @param normal
     *            the normals of the lines.
     * @param color
     *            the color of each point of the lines.
     * @param texture
     *            the texture coordinates of the lines.
     */
    public Line(final String name, final Vector3[] vertex, final Vector3[] normal, final ColorRGBA[] color,
            final Vector2[] texture) {
        super(name);
        reconstruct(BufferUtils.createFloatBuffer(vertex), BufferUtils.createFloatBuffer(normal), BufferUtils
                .createFloatBuffer(color), TexCoords.makeNew(texture));
        _meshData.setIndexMode(IndexMode.Lines);
    }

    @Override
    public void reconstruct(final FloatBuffer vertices, final FloatBuffer normals, final FloatBuffer colors,
            final TexCoords coords) {
        super.reconstruct(vertices, normals, colors, coords);
        generateIndices();
    }

    public void generateIndices() {
        if (_meshData.getIndexBuffer() == null || _meshData.getIndexBuffer().limit() != _meshData.getVertexCount()) {
            _meshData.setIndexBuffer(BufferUtils.createIntBuffer(_meshData.getVertexCount()));
        } else {
            _meshData.getIndexBuffer().rewind();
        }

        for (int x = 0; x < _meshData.getVertexCount(); x++) {
            _meshData.getIndexBuffer().put(x);
        }
    }

    /**
     * Puts a circle into vertex and normal buffer at the current buffer position. The buffers are enlarged and copied
     * if they are too small.
     * 
     * @param radius
     *            radius of the circle
     * @param x
     *            x coordinate of circle center
     * @param y
     *            y coordinate of circle center
     * @param segments
     *            number of line segments the circle is built from
     * @param insideOut
     *            false for normal winding (ccw), true for clockwise winding
     */
    public void appendCircle(final double radius, final double x, final double y, final int segments,
            final boolean insideOut) {
        final int requiredFloats = segments * 2 * 3;
        final FloatBuffer verts = BufferUtils.ensureLargeEnough(_meshData.getVertexBuffer(), requiredFloats);
        _meshData.setVertexBuffer(verts);
        final FloatBuffer normals = BufferUtils.ensureLargeEnough(_meshData.getNormalBuffer(), requiredFloats);
        _meshData.setNormalBuffer(normals);
        double angle = 0;
        final double step = MathUtils.PI * 2 / segments;
        for (int i = 0; i < segments; i++) {
            final double dx = MathUtils.cos(insideOut ? -angle : angle) * radius;
            final double dy = MathUtils.sin(insideOut ? -angle : angle) * radius;
            if (i > 0) {
                verts.put((float) (dx + x)).put((float) (dy + y)).put(0);
                normals.put((float) dx).put((float) dy).put(0);
            }
            verts.put((float) (dx + x)).put((float) (dy + y)).put(0);
            normals.put((float) dx).put((float) dy).put(0);
            angle += step;
        }
        verts.put((float) (radius + x)).put((float) y).put(0);
        normals.put((float) radius).put(0).put(0);
        generateIndices();
    }

    /**
     * @return true if points are to be drawn antialiased
     */
    public boolean isAntialiased() {
        return antialiased;
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
        this.antialiased = antialiased;
    }

    /**
     * @return the width of this line.
     */
    public float getLineWidth() {
        return lineWidth;
    }

    /**
     * Sets the width of the line when drawn. Non anti-aliased line widths are rounded to the nearest whole number by
     * opengl.
     * 
     * @param lineWidth
     *            The lineWidth to set.
     */
    public void setLineWidth(final float lineWidth) {
        this.lineWidth = lineWidth;
    }

    /**
     * @return the set stipplePattern. 0xFFFF means no stipple.
     */
    public short getStipplePattern() {
        return stipplePattern;
    }

    /**
     * The stipple or pattern to use when drawing this line. 0xFFFF is a solid line.
     * 
     * @param stipplePattern
     *            a 16bit short whose bits describe the pattern to use when drawing this line
     */
    public void setStipplePattern(final short stipplePattern) {
        this.stipplePattern = stipplePattern;
    }

    /**
     * @return the set stippleFactor.
     */
    public int getStippleFactor() {
        return stippleFactor;
    }

    /**
     * @param stippleFactor
     *            magnification factor to apply to the stipple pattern.
     */
    public void setStippleFactor(final int stippleFactor) {
        this.stippleFactor = stippleFactor;
    }

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(lineWidth, "lineWidth", 1);
        capsule.write(stipplePattern, "stipplePattern", (short) 0xFFFF);
        capsule.write(antialiased, "antialiased", false);
    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);
        lineWidth = capsule.readFloat("lineWidth", 1);
        stipplePattern = capsule.readShort("stipplePattern", (short) 0xFFFF);
        antialiased = capsule.readBoolean("antialiased", false);
    }

    @Override
    public void render(final Renderer renderer) {
        renderer.setupLineParameters(getLineWidth(), getStippleFactor(), getStipplePattern(), isAntialiased());

        super.render(renderer);
    }

}
