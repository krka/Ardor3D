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
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.InterleavedFormat;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.geom.BufferUtils;

/**
 * MeshData contains all the commonly used buffers for rendering a mesh.
 */
public class MeshData implements Cloneable, Savable {

    /** The Constant logger. */
    private static final Logger logger = Logger.getLogger(MeshData.class.getName());

    /** Number of vertices represented by this data. */
    protected int _vertexCount;

    /** Number of primitives represented by this data. */
    protected int[] _primitiveCounts = new int[1];

    /** Buffer data holding buffers and number of coordinates per vertex */
    protected transient FloatBufferData _vertexCoords;
    protected transient FloatBufferData _normalCoords;
    protected transient FloatBufferData _colorCoords;
    protected transient FloatBufferData _fogCoords;
    protected transient FloatBufferData _tangentCoords;
    protected transient List<FloatBufferData> _textureCoords = new ArrayList<FloatBufferData>(1);

    /** Interleaved data. */
    protected transient FloatBuffer _interleavedBuffer;
    protected transient InterleavedFormat _interleavedFormat = InterleavedFormat.GL_V3F;

    /** Index data. */
    protected transient IntBuffer _indexBuffer;
    protected transient int[] _indexLengths;
    protected transient IndexMode[] _indexModes = new IndexMode[] { IndexMode.Triangles };

    /**
     * Gets the vertex count.
     * 
     * @return the vertex count
     */
    public int getVertexCount() {
        return _vertexCount;
    }

    /**
     * Gets the vertex buffer.
     * 
     * @return the vertex buffer
     */
    public FloatBuffer getVertexBuffer() {
        if (_vertexCoords == null) {
            return null;
        }
        return _vertexCoords.getBuffer();
    }

    /**
     * Sets the vertex buffer.
     * 
     * @param vertexBuffer
     *            the new vertex buffer
     */
    public void setVertexBuffer(final FloatBuffer vertexBuffer) {
        if (vertexBuffer == null) {
            _vertexCoords = null;
        } else {
            _vertexCoords = new FloatBufferData(vertexBuffer, 3);
        }
        updateVertexCount();
    }

    /**
     * Gets the vertex coords.
     * 
     * @return the vertex coords
     */
    public FloatBufferData getVertexCoords() {
        return _vertexCoords;
    }

    /**
     * Sets the vertex coords.
     * 
     * @param bufferData
     *            the new vertex coords
     */
    public void setVertexCoords(final FloatBufferData bufferData) {
        _vertexCoords = bufferData;
    }

    /**
     * Gets the normal buffer.
     * 
     * @return the normal buffer
     */
    public FloatBuffer getNormalBuffer() {
        if (_normalCoords == null) {
            return null;
        }
        return _normalCoords.getBuffer();
    }

    /**
     * Sets the normal buffer.
     * 
     * @param normalBuffer
     *            the new normal buffer
     */
    public void setNormalBuffer(final FloatBuffer normalBuffer) {
        if (normalBuffer == null) {
            _normalCoords = null;
        } else {
            _normalCoords = new FloatBufferData(normalBuffer, 3);
        }
    }

    /**
     * Gets the normal coords.
     * 
     * @return the normal coords
     */
    public FloatBufferData getNormalCoords() {
        return _normalCoords;
    }

    /**
     * Sets the normal coords.
     * 
     * @param bufferData
     *            the new normal coords
     */
    public void setNormalCoords(final FloatBufferData bufferData) {
        _normalCoords = bufferData;
    }

    /**
     * Gets the color buffer.
     * 
     * @return the color buffer
     */
    public FloatBuffer getColorBuffer() {
        if (_colorCoords == null) {
            return null;
        }
        return _colorCoords.getBuffer();
    }

    /**
     * Sets the color buffer.
     * 
     * @param colorBuffer
     *            the new color buffer
     */
    public void setColorBuffer(final FloatBuffer colorBuffer) {
        if (colorBuffer == null) {
            _colorCoords = null;
        } else {
            _colorCoords = new FloatBufferData(colorBuffer, 4);
        }
    }

    /**
     * Gets the color coords.
     * 
     * @return the color coords
     */
    public FloatBufferData getColorCoords() {
        return _colorCoords;
    }

    /**
     * Sets the color coords.
     * 
     * @param bufferData
     *            the new color coords
     */
    public void setColorCoords(final FloatBufferData bufferData) {
        _colorCoords = bufferData;
    }

    /**
     * Gets the fog buffer.
     * 
     * @return the fog buffer
     */
    public FloatBuffer getFogBuffer() {
        if (_fogCoords == null) {
            return null;
        }
        return _fogCoords.getBuffer();
    }

    /**
     * Sets the fog buffer.
     * 
     * @param fogBuffer
     *            the new fog buffer
     */
    public void setFogBuffer(final FloatBuffer fogBuffer) {
        if (fogBuffer == null) {
            _fogCoords = null;
        } else {
            _fogCoords = new FloatBufferData(fogBuffer, 3);
        }
    }

    /**
     * Gets the fog coords.
     * 
     * @return the fog coords
     */
    public FloatBufferData getFogCoords() {
        return _fogCoords;
    }

    /**
     * Sets the fog coords.
     * 
     * @param bufferData
     *            the new fog coords
     */
    public void setFogCoords(final FloatBufferData bufferData) {
        _fogCoords = bufferData;
    }

    /**
     * Gets the tangent buffer.
     * 
     * @return the tangent buffer
     */
    public FloatBuffer getTangentBuffer() {
        if (_tangentCoords == null) {
            return null;
        }
        return _tangentCoords.getBuffer();
    }

    /**
     * Sets the tagent buffer.
     * 
     * @param tangentBuffer
     *            the new tagent buffer
     */
    public void setTagentBuffer(final FloatBuffer tangentBuffer) {
        if (tangentBuffer == null) {
            _tangentCoords = null;
        } else {
            _tangentCoords = new FloatBufferData(tangentBuffer, 3);
        }
    }

    /**
     * Gets the tangent coords.
     * 
     * @return the tangent coords
     */
    public FloatBufferData getTangentCoords() {
        return _tangentCoords;
    }

    /**
     * Sets the tangent coords.
     * 
     * @param bufferData
     *            the new tangent coords
     */
    public void setTangentCoords(final FloatBufferData bufferData) {
        _tangentCoords = bufferData;
    }

    /**
     * Gets the texture buffer.
     * 
     * @param index
     *            the index
     * 
     * @return the texture buffer
     */
    public FloatBuffer getTextureBuffer(final int index) {
        if (_textureCoords.size() <= index) {
            return null;
        }
        return _textureCoords.get(index).getBuffer();
    }

    /**
     * Sets the texture buffer.
     * 
     * @param textureBuffer
     *            the texture buffer
     * @param index
     *            the index
     */
    public void setTextureBuffer(final FloatBuffer textureBuffer, final int index) {
        while (_textureCoords.size() <= index) {
            _textureCoords.add(null);
        }
        _textureCoords.set(index, new FloatBufferData(textureBuffer, 2));
    }

    /**
     * Gets the texture coords.
     * 
     * @return the texture coords
     */
    public List<FloatBufferData> getTextureCoords() {
        return _textureCoords;
    }

    /**
     * Gets the texture coords.
     * 
     * @param index
     *            the index
     * 
     * @return the texture coords
     */
    public FloatBufferData getTextureCoords(final int index) {
        if (_textureCoords.size() <= index) {
            return null;
        }
        return _textureCoords.get(index);
    }

    /**
     * Sets the texture coords.
     * 
     * @param textureCoords
     *            the new texture coords
     */
    public void setTextureCoords(final List<FloatBufferData> textureCoords) {
        _textureCoords = textureCoords;
    }

    /**
     * Sets the texture coords.
     * 
     * @param textureCoords
     *            the texture coords
     * @param index
     *            the index
     */
    public void setTextureCoords(final FloatBufferData textureCoords, final int index) {
        while (_textureCoords.size() <= index) {
            _textureCoords.add(null);
        }
        _textureCoords.set(index, textureCoords);
    }

    /**
     * Retrieves the interleaved buffer, if set or created through packInterleaved.
     * 
     * @return the interleaved buffer
     */
    public FloatBuffer getInterleavedBuffer() {
        return _interleavedBuffer;
    }

    /**
     * Sets the interleaved buffer.
     * 
     * @param interleavedBuffer
     *            the interleaved buffer
     */
    public void setInterleavedBuffer(final FloatBuffer interleavedBuffer) {
        _interleavedBuffer = interleavedBuffer;
    }

    /**
     * Gets the interleaved format.
     * 
     * @return the interleaved format
     */
    public InterleavedFormat getInterleavedFormat() {
        return _interleavedFormat;
    }

    /**
     * Sets the interleaved format.
     * 
     * @param interleavedFormat
     *            the new interleaved format
     */
    public void setInterleavedFormat(final InterleavedFormat interleavedFormat) {
        _interleavedFormat = interleavedFormat;
    }

    /**
     * Update the vertex count based on the current limit of the vertex buffer.
     */
    public void updateVertexCount() {
        if (_vertexCoords == null) {
            _vertexCount = 0;
        } else {
            _vertexCount = _vertexCoords.getCount();
        }
        // update primitive count if we are using arrays
        if (_indexBuffer == null) {
            updatePrimitiveCounts();
        }
    }

    /**
     * <code>copyTextureCoords</code> copys the texture coordinates of a given texture unit to another location. If the
     * texture unit is not valid, then the coordinates are ignored. Coords are multiplied by the given factor.
     * 
     * @param fromIndex
     *            the coordinates to copy.
     * @param toIndex
     *            the texture unit to set them to.
     * @param factor
     *            a multiple to apply when copying
     */
    public void copyTextureCoordinates(final int fromIndex, final int toIndex, final float factor) {
        if (_textureCoords == null) {
            return;
        }

        if (fromIndex < 0 || fromIndex >= _textureCoords.size() || _textureCoords.get(fromIndex) == null) {
            return;
        }

        if (toIndex < 0 || toIndex == fromIndex) {
            return;
        }

        // make sure we are big enough
        while (toIndex >= _textureCoords.size()) {
            _textureCoords.add(null);
        }

        FloatBufferData dest = _textureCoords.get(toIndex);
        final FloatBufferData src = _textureCoords.get(fromIndex);
        if (dest == null || dest.getBuffer().capacity() != src.getBuffer().limit()) {
            dest = new FloatBufferData(BufferUtils.createFloatBuffer(src.getBuffer().capacity()), src
                    .getCoordsPerVertex());
            _textureCoords.set(toIndex, dest);
        }
        dest.getBuffer().clear();
        final int oldLimit = src.getBuffer().limit();
        src.getBuffer().clear();
        for (int i = 0, len = dest.getBuffer().capacity(); i < len; i++) {
            dest.getBuffer().put(factor * src.getBuffer().get());
        }
        src.getBuffer().limit(oldLimit);
        dest.getBuffer().limit(oldLimit);
    }

    /**
     * <code>copyTextureCoords</code> copys the texture coordinates of a given texture unit to another location. If the
     * texture unit is not valid, then the coordinates are ignored. Coords are multiplied by the given factor.
     * 
     * @param fromIndex
     *            the coordinates to copy.
     * @param toIndex
     *            the texture unit to set them to.
     * @param factorS
     *            a multiple to apply to the S channel when copying
     * @param factorT
     *            a multiple to apply to the T channel when copying
     */
    public void copyTextureCoordinates(final int fromIndex, final int toIndex, final float factorS, final float factorT) {
        if (_textureCoords == null) {
            return;
        }

        if (fromIndex < 0 || fromIndex >= _textureCoords.size() || _textureCoords.get(fromIndex) == null) {
            return;
        }

        if (toIndex < 0 || toIndex == fromIndex) {
            return;
        }

        // make sure we are big enough
        while (toIndex >= _textureCoords.size()) {
            _textureCoords.add(null);
        }

        FloatBufferData dest = _textureCoords.get(toIndex);
        final FloatBufferData src = _textureCoords.get(fromIndex);
        if (dest == null || dest.getBuffer().capacity() != src.getBuffer().limit()) {
            dest = new FloatBufferData(BufferUtils.createFloatBuffer(src.getBuffer().capacity()), src
                    .getCoordsPerVertex());
            _textureCoords.set(toIndex, dest);
        }
        dest.getBuffer().clear();
        final int oldLimit = src.getBuffer().limit();
        src.getBuffer().clear();
        for (int i = 0, len = dest.getBuffer().capacity(); i < len; i++) {
            if (i % 2 == 0) {
                dest.getBuffer().put(factorS * src.getBuffer().get());
            } else {
                dest.getBuffer().put(factorT * src.getBuffer().get());
            }
        }
        src.getBuffer().limit(oldLimit);
        dest.getBuffer().limit(oldLimit);
    }

    /**
     * <code>getNumberOfUnits</code> returns the number of texture units this geometry is currently using.
     * 
     * @return the number of texture units in use.
     */
    public int getNumberOfUnits() {
        if (_textureCoords == null) {
            return 0;
        }
        return _textureCoords.size();
    }

    /**
     * Gets the index buffer.
     * 
     * @return the index buffer
     */
    public IntBuffer getIndexBuffer() {
        return _indexBuffer;
    }

    /**
     * Sets the index buffer.
     * 
     * @param indices
     *            the new index buffer
     */
    public void setIndexBuffer(final IntBuffer indices) {
        _indexBuffer = indices;
        updatePrimitiveCounts();
    }

    /**
     * Gets the index mode.
     * 
     * @return the IndexMode of the first section of this MeshData.
     */
    public IndexMode getIndexMode() {
        return getIndexMode(0);
    }

    /**
     * Sets the index mode.
     * 
     * @param indexMode
     *            the new IndexMode to use for the first section of this MeshData.
     */
    public void setIndexMode(final IndexMode indexMode) {
        _indexModes[0] = indexMode;
        updatePrimitiveCounts();
    }

    /**
     * Gets the index lengths.
     * 
     * @return the index lengths
     */
    public int[] getIndexLengths() {
        return _indexLengths;
    }

    /**
     * Sets the index lengths.
     * 
     * @param indexLengths
     *            the new index lengths
     */
    public void setIndexLengths(final int[] indexLengths) {
        _indexLengths = indexLengths;
        updatePrimitiveCounts();
    }

    /**
     * Gets the index modes.
     * 
     * @return the index modes
     */
    public IndexMode[] getIndexModes() {
        return _indexModes;
    }

    /**
     * Gets the index mode.
     * 
     * @param sectionIndex
     *            the section index
     * 
     * @return the index mode
     */
    public IndexMode getIndexMode(final int sectionIndex) {
        if (sectionIndex < 0 || sectionIndex >= getSectionCount()) {
            throw new IllegalArgumentException("invalid section index: " + sectionIndex);
        }
        return _indexModes.length > sectionIndex ? _indexModes[sectionIndex] : _indexModes[_indexModes.length - 1];
    }

    /**
     * Note: Also updates primitive counts.
     * 
     * @param indexModes
     *            the index modes to use for this MeshData.
     */
    public void setIndexModes(final IndexMode[] indexModes) {
        _indexModes = indexModes;
        updatePrimitiveCounts();
    }

    /**
     * Gets the section count.
     * 
     * @return the number of sections (lengths, indexModes, etc.) this MeshData contains.
     */
    public int getSectionCount() {
        return _indexLengths != null ? _indexLengths.length : 1;
    }

    /**
     * Gets the total primitive count.
     * 
     * @return the sum of the primitive counts on all sections of this mesh data.
     */
    public int getTotalPrimitiveCount() {
        int count = 0;
        for (int i = 0; i < _primitiveCounts.length; i++) {
            count += _primitiveCounts[i];
        }
        return count;
    }

    /**
     * Gets the primitive count.
     * 
     * @param section
     *            the section
     * 
     * @return the number of primitives (triangles, quads, lines, points, etc.) on a given section of this mesh data.
     */
    public int getPrimitiveCount(final int section) {
        return _primitiveCounts[section];
    }

    /**
     * Returns the vertex indices of a specified primitive.
     * 
     * @param primitiveIndex
     *            which triangle, quad, etc
     * @param section
     *            which section to pull from (corresponds to array position in indexmodes and lengths)
     * @param store
     *            an int array to store the results in. if null, or the length < the size of the primitive, a new array
     *            is created and returned.
     * 
     * @return the primitive's vertex indices as an array
     * 
     * @throws IndexOutOfBoundsException
     *             if primitiveIndex is outside of range [0, count-1] where count is the number of primitives in the
     *             given section.
     * @throws ArrayIndexOutOfBoundsException
     *             if section is out of range [0, N-1] where N is the number of sections in this MeshData object.
     */
    public int[] getPrimitive(final int primitiveIndex, final int section, final int[] store) {
        final int count = getPrimitiveCount(section);
        if (primitiveIndex >= count || primitiveIndex < 0) {
            throw new IndexOutOfBoundsException("Invalid primitiveIndex '" + primitiveIndex + "'.  Count is " + count);
        }

        final IndexMode mode = getIndexMode(section);
        final int rSize = mode.getVertexCount();

        int[] result = store;
        if (result == null || result.length < rSize) {
            result = new int[rSize];
        }

        for (int i = 0; i < rSize; i++) {
            if (getIndexBuffer() != null) {
                result[i] = getIndexBuffer().get(getVertexIndex(primitiveIndex, i, section));
            } else {
                result[i] = getVertexIndex(primitiveIndex, i, section);
            }
        }

        return result;
    }

    /**
     * Gets the primitive.
     * 
     * @param primitiveIndex
     *            the primitive index
     * @param section
     *            the section
     * @param store
     *            the store
     * 
     * @return the primitive
     */
    public Vector3[] getPrimitive(final int primitiveIndex, final int section, final Vector3[] store) {
        final int count = getPrimitiveCount(section);
        if (primitiveIndex >= count || primitiveIndex < 0) {
            throw new IndexOutOfBoundsException("Invalid primitiveIndex '" + primitiveIndex + "'.  Count is " + count);
        }

        final IndexMode mode = getIndexMode(section);
        final int rSize = mode.getVertexCount();
        Vector3[] result = store;
        if (result == null || result.length < rSize) {
            result = new Vector3[rSize];
        }

        for (int i = 0; i < rSize; i++) {
            if (result[i] == null) {
                result[i] = new Vector3();
            }
            if (getIndexBuffer() != null) {
                // indexed geometry
                BufferUtils.populateFromBuffer(result[i], getVertexBuffer(), getIndexBuffer().get(
                        getVertexIndex(primitiveIndex, i, section)));
            } else {
                // non-indexed geometry
                BufferUtils
                        .populateFromBuffer(result[i], getVertexBuffer(), getVertexIndex(primitiveIndex, i, section));
            }
        }

        return result;
    }

    /**
     * Gets the vertex index.
     * 
     * @param primitiveIndex
     *            which triangle, quad, etc.
     * @param point
     *            which point on the triangle, quad, etc. (triangle has three points, so this would be 0-2, etc.)
     * @param section
     *            which section to pull from (corresponds to array position in indexmodes and lengths)
     * 
     * @return the position you would expect to find the given point in the index buffer
     */
    public int getVertexIndex(final int primitiveIndex, final int point, final int section) {
        int index = 0;
        // move our offset up to the beginning of our section
        for (int i = 0; i < section; i++) {
            index += _indexLengths[i];
        }

        // Ok, now pull primitive index based on indexmode.
        switch (getIndexMode(section)) {
            case Triangles:
                index += (primitiveIndex * 3) + point;
                break;
            case TriangleStrip:
                // XXX: Do we need to flip point 0 and 1 on odd primitiveIndex values?
                // if (point < 2 && primitiveIndex % 2 == 1) {
                // index += primitiveIndex + (point == 0 ? 1 : 0);
                // } else {
                index += primitiveIndex + point;
                // }
                break;
            case TriangleFan:
                if (point == 0) {
                    index += 0;
                } else {
                    index += primitiveIndex + point;
                }
                break;
            case Quads:
                index += (primitiveIndex * 4) + point;
                break;
            case QuadStrip:
                index += (primitiveIndex * 2) + point;
                break;
            case Points:
                index += primitiveIndex;
                break;
            case Lines:
                index += (primitiveIndex * 2) + point;
                break;
            case LineStrip:
            case LineLoop:
                index += primitiveIndex + point;
                break;
            default:
                logger.warning("unimplemented index mode: " + getIndexMode(0));
                return -1;
        }
        return index;
    }

    /**
     * Random vertex.
     * 
     * @param store
     *            the vector object to store the result in. if null, a new one is created.
     * 
     * @return a random vertex from the vertices stored in this MeshData. null is returned if there are no vertices.
     */
    public Vector3 randomVertex(final Vector3 store) {
        if (_vertexCoords == null) {
            return null;
        }

        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }

        final int i = MathUtils.nextRandomInt(0, getVertexCount() - 1);
        BufferUtils.populateFromBuffer(result, _vertexCoords.getBuffer(), i);

        return result;
    }

    /**
     * Random point on primitives.
     * 
     * @param store
     *            the vector object to store the result in. if null, a new one is created.
     * 
     * @return a random point from the surface of a primitive stored in this MeshData. null is returned if there are no
     *         vertices or indices.
     */
    public Vector3 randomPointOnPrimitives(final Vector3 store) {
        if (_vertexCoords == null || _indexBuffer == null) {
            return null;
        }

        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }

        // randomly pick a section (if there are more than 1)
        final int section = MathUtils.nextRandomInt(0, getSectionCount() - 1);

        // randomly pick a primitive in that section
        final int primitiveIndex = MathUtils.nextRandomInt(0, getPrimitiveCount(section) - 1);

        // Now, based on IndexMode, pick a point on that primitive
        final IndexMode mode = getIndexMode(section);
        switch (mode) {
            case Triangles:
            case TriangleFan:
            case TriangleStrip:
            case Quads:
            case QuadStrip: {
                final int pntA = getIndexBuffer().get(getVertexIndex(primitiveIndex, 0, section));
                final int pntB = getIndexBuffer().get(getVertexIndex(primitiveIndex, 1, section));
                final int pntC = getIndexBuffer().get(getVertexIndex(primitiveIndex, 2, section));

                double b = MathUtils.nextRandomDouble();
                double c = MathUtils.nextRandomDouble();

                if (mode != IndexMode.Quads && mode != IndexMode.QuadStrip) {
                    // keep it in the triangle by reflecting it across the center diagonal BC
                    if (b + c > 1) {
                        b = 1 - b;
                        c = 1 - c;
                    }
                }

                final double a = 1 - b - c;

                final Vector3 work = Vector3.fetchTempInstance();
                BufferUtils.populateFromBuffer(work, getVertexBuffer(), pntA);
                work.multiplyLocal(a);
                result.set(work);

                BufferUtils.populateFromBuffer(work, getVertexBuffer(), pntB);
                work.multiplyLocal(b);
                result.addLocal(work);

                BufferUtils.populateFromBuffer(work, getVertexBuffer(), pntC);
                work.multiplyLocal(c);
                result.addLocal(work);
                Vector3.releaseTempInstance(work);
                break;
            }
            case Points: {
                final int pnt = getIndexBuffer().get(getVertexIndex(primitiveIndex, 0, section));
                BufferUtils.populateFromBuffer(result, getVertexBuffer(), pnt);
                break;
            }
            case Lines:
            case LineLoop:
            case LineStrip: {
                final int pntA = getIndexBuffer().get(getVertexIndex(primitiveIndex, 0, section));
                final int pntB = getIndexBuffer().get(getVertexIndex(primitiveIndex, 1, section));

                final Vector3 work = Vector3.fetchTempInstance();
                BufferUtils.populateFromBuffer(result, getVertexBuffer(), pntA);
                BufferUtils.populateFromBuffer(work, getVertexBuffer(), pntB);
                Vector3.lerp(result, work, MathUtils.nextRandomDouble(), result);
                Vector3.releaseTempInstance(work);
                break;
            }
        }

        return result;
    }

    /**
     * Translate points.
     * 
     * @param x
     *            the x
     * @param y
     *            the y
     * @param z
     *            the z
     */
    public void translatePoints(final double x, final double y, final double z) {
        translatePoints(new Vector3(x, y, z));
    }

    /**
     * Translate points.
     * 
     * @param amount
     *            the amount
     */
    public void translatePoints(final Vector3 amount) {
        for (int x = 0; x < _vertexCount; x++) {
            BufferUtils.addInBuffer(amount, _vertexCoords.getBuffer(), x);
        }
    }

    /**
     * Rotate points.
     * 
     * @param rotate
     *            the rotate
     */
    public void rotatePoints(final Quaternion rotate) {
        final Vector3 store = new Vector3();
        for (int x = 0; x < _vertexCount; x++) {
            BufferUtils.populateFromBuffer(store, _vertexCoords.getBuffer(), x);
            rotate.apply(store, store);
            BufferUtils.setInBuffer(store, _vertexCoords.getBuffer(), x);
        }
    }

    /**
     * Rotate normals.
     * 
     * @param rotate
     *            the rotate
     */
    public void rotateNormals(final Quaternion rotate) {
        final Vector3 store = new Vector3();
        for (int x = 0; x < _vertexCount; x++) {
            BufferUtils.populateFromBuffer(store, _normalCoords.getBuffer(), x);
            rotate.apply(store, store);
            BufferUtils.setInBuffer(store, _normalCoords.getBuffer(), x);
        }
    }

    /**
     * Update primitive counts.
     */
    private void updatePrimitiveCounts() {
        final int maxIndex = _indexBuffer != null ? _indexBuffer.limit() : _vertexCount;
        final int maxSection = _indexLengths != null ? _indexLengths.length : 1;
        if (_primitiveCounts.length != maxSection) {
            _primitiveCounts = new int[maxSection];
        }
        for (int i = 0; i < maxSection; i++) {
            final int size = _indexLengths != null ? _indexLengths[i] : maxIndex;
            int count = 0;
            switch (getIndexMode(i)) {
                case Triangles:
                    count = size / 3;
                    break;
                case TriangleFan:
                case TriangleStrip:
                    count = size - 2;
                    break;
                case Quads:
                    count = size / 4;
                    break;
                case QuadStrip:
                    count = size / 2 - 1;
                    break;
                case Lines:
                    count = size / 2;
                    break;
                case LineStrip:
                    count = size - 1;
                    break;
                case LineLoop:
                    count = size;
                    break;
                case Points:
                    count = size;
                    break;
                default:
                    logger.warning("unimplemented index mode: " + getIndexMode(i));
            }

            _primitiveCounts[i] = count;
        }

    }

    // /////////////////
    // Method for Cloneable
    // /////////////////

    @Override
    public MeshData clone() {
        try {
            return (MeshData) super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new AssertionError(); // can not happen
        }
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    public Class<? extends MeshData> getClassTag() {
        return this.getClass();
    }

    public void write(final Ardor3DExporter e) throws IOException {
        final OutputCapsule capsule = e.getCapsule(this);

        capsule.write(_vertexCount, "vertexCount", 0);
        capsule.write(_vertexCoords, "vertexBuffer", null);
        capsule.write(_normalCoords, "normalBuffer", null);
        capsule.write(_colorCoords, "colorBuffer", null);
        capsule.write(_fogCoords, "fogBuffer", null);
        capsule.write(_tangentCoords, "tangentBuffer", null);
        capsule.writeSavableList(_textureCoords, "textureCoords", new ArrayList<FloatBufferData>(1));
        capsule.write(_indexBuffer, "indexBuffer", null);
        capsule.write(_interleavedBuffer, "interleavedBuffer", null);
        capsule.write(_indexLengths, "indexLengths", null);
        capsule.write(_indexModes, "indexModes");
    }

    public void read(final Ardor3DImporter e) throws IOException {
        final InputCapsule capsule = e.getCapsule(this);

        _vertexCount = capsule.readInt("vertexCount", 0);
        _vertexCoords = (FloatBufferData) capsule.readSavable("vertexBuffer", null);
        _normalCoords = (FloatBufferData) capsule.readSavable("normalBuffer", null);
        _colorCoords = (FloatBufferData) capsule.readSavable("colorBuffer", null);
        _fogCoords = (FloatBufferData) capsule.readSavable("fogBuffer", null);
        _tangentCoords = (FloatBufferData) capsule.readSavable("tangentBuffer", null);
        _textureCoords = capsule.readSavableList("textureCoords", new ArrayList<FloatBufferData>(1));
        _indexBuffer = capsule.readIntBuffer("indexBuffer", null);
        _interleavedBuffer = capsule.readFloatBuffer("interleavedBuffer", null);
        _indexLengths = capsule.readIntArray("indexLengths", null);
        _indexModes = capsule.readEnumArray("indexModes", IndexMode.class, new IndexMode[] { IndexMode.Triangles });

        updatePrimitiveCounts();
    }
}
