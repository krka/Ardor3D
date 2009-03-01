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
    private static final Logger logger = Logger.getLogger(MeshData.class.getName());

    /** Number of vertices represented by this data */
    protected int _vertexCount;

    /** Number of primitives represented by this data */
    protected int[] _primitiveCounts = new int[1];

    /** Buffer data */
    protected transient FloatBuffer _vertexBuffer;
    protected transient FloatBuffer _normalBuffer;
    protected transient FloatBuffer _colorBuffer;
    protected transient List<TexCoords> _textureCoords = new ArrayList<TexCoords>(1);

    /** Interleaved data */
    protected transient FloatBuffer _interleavedBuffer;
    protected transient InterleavedFormat _interleavedFormat = InterleavedFormat.GL_V3F;

    /** TODO: this should be handled differently */
    protected transient FloatBuffer _tangentBuffer;

    /** Index data */
    protected transient IntBuffer _indexBuffer;
    protected transient int[] _indexLengths;
    protected transient IndexMode[] _indexModes = new IndexMode[] { IndexMode.Triangles };

    /**
     * Retrieves the interleaved buffer, if set or created through packInterleaved.
     * 
     * @return the interleaved buffer
     */
    public FloatBuffer getInterleavedBuffer() {
        return _interleavedBuffer;
    }

    /**
     * 
     * @param interleaved
     */
    public void setInterleavedBuffer(final FloatBuffer interleavedBuffer) {
        _interleavedBuffer = interleavedBuffer;
    }

    public InterleavedFormat getInterleavedFormat() {
        return _interleavedFormat;
    }

    public void setInterleavedFormat(final InterleavedFormat interleavedFormat) {
        _interleavedFormat = interleavedFormat;
    }

    /**
     * 
     * @return
     */
    public int getVertexCount() {
        return _vertexCount;
    }

    /**
     * 
     * @return
     */
    public FloatBuffer getVertexBuffer() {
        return _vertexBuffer;
    }

    /**
     * 
     * @param vertices
     */
    public void setVertexBuffer(final FloatBuffer vertexBuffer) {
        _vertexBuffer = vertexBuffer;
        updateVertexCount();
    }

    /**
     * Update the vertex count based on the current limit of the vertex buffer.
     */
    public void updateVertexCount() {
        if (_vertexBuffer == null) {
            _vertexCount = 0;
        } else {
            _vertexCount = _vertexBuffer.limit() / 3;
        }
        // update primitive count if we are using arrays
        if (_indexBuffer == null) {
            updatePrimitiveCounts();
        }
    }

    /**
     * 
     * @return
     */
    public FloatBuffer getNormalBuffer() {
        return _normalBuffer;
    }

    /**
     * 
     * @param normals
     */
    public void setNormalBuffer(final FloatBuffer normalBuffer) {
        _normalBuffer = normalBuffer;
    }

    /**
     * 
     * @return
     */
    public FloatBuffer getColorBuffer() {
        return _colorBuffer;
    }

    /**
     * 
     * @param colors
     */
    public void setColorBuffer(final FloatBuffer colorBuffer) {
        _colorBuffer = colorBuffer;
    }

    /**
     * 
     * @return
     */
    public FloatBuffer getTangentBuffer() {
        return _tangentBuffer;
    }

    /**
     * 
     * @param vertices
     */
    public void setTangentBuffer(final FloatBuffer tangentBuffer) {
        _tangentBuffer = tangentBuffer;
    }

    /**
     * 
     * @param index
     * @return
     */
    public FloatBuffer getTextureBuffer(final int index) {
        if (_textureCoords.size() <= index) {
            return null;
        }
        return _textureCoords.get(index).coords;
    }

    /**
     * 
     * @param textureBuffer
     * @param index
     */
    public void setTextureBuffer(final FloatBuffer textureBuffer, final int index) {
        while (_textureCoords.size() <= index) {
            _textureCoords.add(null);
        }
        _textureCoords.set(index, new TexCoords(textureBuffer));
    }

    /**
     * 
     * @param index
     * @return
     */
    public List<TexCoords> getTextureCoords() {
        return _textureCoords;
    }

    /**
     * 
     * @param index
     * @return
     */
    public TexCoords getTextureCoords(final int index) {
        if (_textureCoords.size() <= index) {
            return null;
        }
        return _textureCoords.get(index);
    }

    public void setTextureCoords(final List<TexCoords> textureCoords) {
        _textureCoords = textureCoords;
    }

    public void setTextureCoords(final TexCoords textureCoords, final int index) {
        while (_textureCoords.size() <= index) {
            _textureCoords.add(null);
        }
        _textureCoords.set(index, textureCoords);
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

        TexCoords dest = _textureCoords.get(toIndex);
        final TexCoords src = _textureCoords.get(fromIndex);
        if (dest == null || dest.coords.capacity() != src.coords.limit()) {
            dest = new TexCoords(BufferUtils.createFloatBuffer(src.coords.capacity()), src.perVert);
            _textureCoords.set(toIndex, dest);
        }
        dest.coords.clear();
        final int oldLimit = src.coords.limit();
        src.coords.clear();
        for (int i = 0, len = dest.coords.capacity(); i < len; i++) {
            dest.coords.put(factor * src.coords.get());
        }
        src.coords.limit(oldLimit);
        dest.coords.limit(oldLimit);
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
     * 
     * @return
     */
    public IntBuffer getIndexBuffer() {
        return _indexBuffer;
    }

    /**
     * 
     * @param indices
     */
    public void setIndexBuffer(final IntBuffer indices) {
        _indexBuffer = indices;
        updatePrimitiveCounts();
    }

    /**
     * @return the IndexMode of the first section of this MeshData.
     */
    public IndexMode getIndexMode() {
        return getIndexMode(0);
    }

    /**
     * @param indexMode
     *            the new IndexMode to use for the first section of this MeshData.
     */
    public void setIndexMode(final IndexMode indexMode) {
        _indexModes[0] = indexMode;
        updatePrimitiveCounts();
    }

    public int[] getIndexLengths() {
        return _indexLengths;
    }

    public void setIndexLengths(final int[] indexLengths) {
        _indexLengths = indexLengths;
        updatePrimitiveCounts();
    }

    public IndexMode[] getIndexModes() {
        return _indexModes;
    }

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
     * 
     * @return the number of sections (lengths, indexModes, etc.) this MeshData contains.
     */
    public int getSectionCount() {
        return _indexLengths != null ? _indexLengths.length : 1;
    }

    /**
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
     * @return the primitive's vertex indices as an array
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
            result[i] = getIndexBuffer().get(getVertexIndex(primitiveIndex, i, section));
        }

        return result;
    }

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
            BufferUtils.populateFromBuffer(result[i], getVertexBuffer(), getIndexBuffer().get(
                    getVertexIndex(primitiveIndex, i, section)));
        }

        return result;
    }

    /**
     * 
     * @param primitiveIndex
     *            which triangle, quad, etc.
     * @param point
     *            which point on the triangle, quad, etc. (triangle has three points, so this would be 0-2, etc.)
     * @param section
     *            which section to pull from (corresponds to array position in indexmodes and lengths)
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
     * @param store
     *            the vector object to store the result in. if null, a new one is created.
     * @return a random vertex from the vertices stored in this MeshData. null is returned if there are no vertices.
     */
    public Vector3 randomVertex(final Vector3 store) {
        if (_vertexBuffer == null) {
            return null;
        }

        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }

        final int i = MathUtils.nextRandomInt(0, getVertexCount() - 1);
        BufferUtils.populateFromBuffer(result, _vertexBuffer, i);

        return result;
    }

    /**
     * @param store
     *            the vector object to store the result in. if null, a new one is created.
     * @return a random point from the surface of a primitive stored in this MeshData. null is returned if there are no
     *         vertices or indices.
     */
    public Vector3 randomPointOnPrimitives(final Vector3 store) {
        if (_vertexBuffer == null || _indexBuffer == null) {
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

    public void translatePoints(final double x, final double y, final double z) {
        translatePoints(new Vector3(x, y, z));
    }

    public void translatePoints(final Vector3 amount) {
        for (int x = 0; x < _vertexCount; x++) {
            BufferUtils.addInBuffer(amount, _vertexBuffer, x);
        }
    }

    public void rotatePoints(final Quaternion rotate) {
        final Vector3 store = new Vector3();
        for (int x = 0; x < _vertexCount; x++) {
            BufferUtils.populateFromBuffer(store, _vertexBuffer, x);
            rotate.apply(store, store);
            BufferUtils.setInBuffer(store, _vertexBuffer, x);
        }
    }

    public void rotateNormals(final Quaternion rotate) {
        final Vector3 store = new Vector3();
        for (int x = 0; x < _vertexCount; x++) {
            BufferUtils.populateFromBuffer(store, _normalBuffer, x);
            rotate.apply(store, store);
            BufferUtils.setInBuffer(store, _normalBuffer, x);
        }
    }

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

    /**
     * Pack all used buffers into an interleaved buffer. TODO: Ugly, experimental and incomplete...
     */
    public void packInterleaved() {
        final boolean hasVertexBuffer = _vertexBuffer != null;
        final boolean hasNormalBuffer = _normalBuffer != null;
        final boolean hasColorBuffer = _colorBuffer != null;

        final FloatBuffer textureBuffer = getTextureBuffer(0);
        final boolean hasTextureBuffer = textureBuffer != null;

        int bufferSize = 0;
        if (hasVertexBuffer) {
            bufferSize += _vertexCount * 3;
            _vertexBuffer.rewind();
        }
        if (hasNormalBuffer) {
            bufferSize += _vertexCount * 3;
            _normalBuffer.rewind();
        }
        if (hasColorBuffer) {
            bufferSize += _vertexCount * 4;
            _colorBuffer.rewind();
        }
        if (hasTextureBuffer) {
            bufferSize += _vertexCount * 2;
            textureBuffer.rewind();
        }

        _interleavedBuffer = BufferUtils.createFloatBuffer(bufferSize);
        _interleavedBuffer.rewind();

        for (int i = 0; i < _vertexCount; i++) {
            if (hasTextureBuffer) {
                _interleavedBuffer.put(textureBuffer.get()).put(textureBuffer.get());
            }

            if (hasColorBuffer) {
                _interleavedBuffer.put(_colorBuffer.get()).put(_colorBuffer.get()).put(_colorBuffer.get());
                _colorBuffer.get();
            }

            if (hasNormalBuffer) {
                _interleavedBuffer.put(_normalBuffer.get()).put(_normalBuffer.get()).put(_normalBuffer.get());
            }

            if (hasVertexBuffer) {
                _interleavedBuffer.put(_vertexBuffer.get()).put(_vertexBuffer.get()).put(_vertexBuffer.get());
            }
        }

        _interleavedFormat = InterleavedFormat.GL_V3F;
        if (!hasNormalBuffer && !hasColorBuffer && hasTextureBuffer) {
            _interleavedFormat = InterleavedFormat.GL_T2F_V3F;
        } else if (!hasNormalBuffer && hasColorBuffer && !hasTextureBuffer) {
            _interleavedFormat = InterleavedFormat.GL_C3F_V3F;
        } else if (!hasNormalBuffer && hasColorBuffer && hasTextureBuffer) {
            _interleavedFormat = InterleavedFormat.GL_T2F_C3F_V3F;
        } else if (hasNormalBuffer && !hasColorBuffer && !hasTextureBuffer) {
            _interleavedFormat = InterleavedFormat.GL_N3F_V3F;
        } else if (hasNormalBuffer && !hasColorBuffer && hasTextureBuffer) {
            _interleavedFormat = InterleavedFormat.GL_T2F_N3F_V3F;
        } else if (hasNormalBuffer && hasColorBuffer && hasTextureBuffer) {
            _interleavedFormat = InterleavedFormat.GL_T2F_C4F_N3F_V3F;
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
        capsule.write(_vertexBuffer, "vertexBuffer", null);
        capsule.write(_normalBuffer, "normalBuffer", null);
        capsule.write(_colorBuffer, "colorBuffer", null);
        capsule.writeSavableList(_textureCoords, "textureCoords", new ArrayList<TexCoords>(1));
        capsule.write(_indexBuffer, "indexBuffer", null);
        capsule.write(_interleavedBuffer, "interleavedBuffer", null);
        capsule.write(_tangentBuffer, "tangentBuffer", null);
        capsule.write(_indexLengths, "indexLengths", null);
        capsule.write(_indexModes, "indexModes");
    }

    public void read(final Ardor3DImporter e) throws IOException {
        final InputCapsule capsule = e.getCapsule(this);

        _vertexCount = capsule.readInt("vertexCount", 0);
        _vertexBuffer = capsule.readFloatBuffer("vertexBuffer", null);
        _normalBuffer = capsule.readFloatBuffer("normalBuffer", null);
        _colorBuffer = capsule.readFloatBuffer("colorBuffer", null);
        _textureCoords = capsule.readSavableList("textureCoords", new ArrayList<TexCoords>(1));
        _indexBuffer = capsule.readIntBuffer("indexBuffer", null);
        _interleavedBuffer = capsule.readFloatBuffer("interleavedBuffer", null);
        _tangentBuffer = capsule.readFloatBuffer("tangentBuffer", null);
        _indexLengths = capsule.readIntArray("indexLengths", null);
        _indexModes = capsule.readEnumArray("indexModes", IndexMode.class, new IndexMode[] { IndexMode.Triangles });

        updatePrimitiveCounts();
    }
}
