/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.util;

import java.nio.FloatBuffer;
import java.util.LinkedList;
import java.util.logging.Logger;

import com.ardor3d.extension.model.collada.binding.ColladaException;
import com.ardor3d.extension.model.collada.binding.DaeList;
import com.ardor3d.extension.model.collada.binding.DaeTreeNode;
import com.ardor3d.extension.model.collada.binding.core.Collada;
import com.ardor3d.extension.model.collada.binding.core.DaeInputShared;
import com.ardor3d.extension.model.collada.binding.core.DaeInputUnshared;
import com.ardor3d.extension.model.collada.binding.core.DaeParam;
import com.ardor3d.extension.model.collada.binding.core.DaeParamType;
import com.ardor3d.extension.model.collada.binding.core.DaeSource;
import com.ardor3d.extension.model.collada.binding.core.DaeVertices;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.util.geom.BufferUtils;

public class ColladaInputPipe {
    private static final Logger logger = Logger.getLogger(ColladaInputPipe.class.getName());

    private final int _offset;
    private final int _set;
    private final DaeSource _source;
    private final DaeList<DaeParam> _params;

    private Type _type;

    private FloatBuffer _buffer;

    enum Type {
        VERTEX, NORMAL, TEXCOORD, UNKNOWN
    }

    public ColladaInputPipe(final DaeInputShared input, final Collada rootNode) {
        // Setup our type

        try {
            _type = Type.valueOf(input.getSemantic());
        } catch (final Exception ex) {
            logger.warning("Unknown input type: " + input.getSemantic());
            _type = Type.UNKNOWN;
        }

        // Locate our source
        final DaeTreeNode n = rootNode.resolveUrl(input.getSource());
        if (n instanceof DaeSource) {
            _source = (DaeSource) n;
        } else if (n instanceof DaeVertices) {
            _source = getPositionSource((DaeVertices) n, rootNode);
        } else {
            throw new ColladaException("Input source not found: " + input.getSource(), input);
        }

        // add a hook to our params from the technique_common
        _params = _source.getCommonAccessor().getParams();

        // save our offset
        _offset = input.getOffset();

        // save our set
        _set = input.getSet();
    }

    public static DaeSource getPositionSource(final DaeVertices v, final Collada rootNode) {
        if (v.getInputs() != null) {
            for (final DaeInputUnshared input : v.getInputs()) {
                if ("POSITION".equals(input.getSemantic())) {
                    final DaeTreeNode n = rootNode.resolveUrl(input.getSource());
                    if (n instanceof DaeSource) {
                        return (DaeSource) n;
                    }
                }
            }
        }

        // changed this to throw an exception instead - otherwise, there will just be a nullpointer exception
        // outside. This provides much more information about what went wrong / Petter
        // return null;
        throw new ColladaException("Unable to find POSITION semantic for inputs under DaeVertices", v);
    }

    public void setupBuffer(final int numEntries, final MeshData meshData) {
        // use our source and the number of params to determine our buffer length
        // we'll use the params from the common technique accessor:
        final int size = _params.size() * numEntries;
        switch (_type) {
            case VERTEX:
                _buffer = BufferUtils.createFloatBuffer(size);
                meshData.setVertexBuffer(_buffer);
                break;
            case NORMAL:
                _buffer = BufferUtils.createFloatBuffer(size);
                meshData.setNormalBuffer(_buffer);
                break;
            case TEXCOORD:
                _buffer = BufferUtils.createFloatBuffer(size);
                meshData.setTextureCoords(new FloatBufferData(_buffer, _params.size()), _set);
                break;
            default:
        }
    }

    private void pushValues(final int memberIndex) {
        if (_buffer == null) {
            return;
        }
        if (memberIndex >= _source.getCommonAccessor().getCount()) {
            logger.warning("Accessed invalid count " + memberIndex + " on source " + _source + ".");
            return;
        }
        int index = memberIndex * _source.getCommonAccessor().getStride() + _source.getCommonAccessor().getOffset();
        for (final DaeParam param : _params) {
            if (param.getName() != null) {
                if (param.getType() == DaeParamType.FLOAT) {
                    _buffer.put(_source.getFloatArray().getData()[index]);
                } else if (param.getType() == DaeParamType.DOUBLE) {
                    _buffer.put((float) _source.getDoubleArray().getData()[index]);
                } else if (param.getType() == DaeParamType.INT) {
                    _buffer.put(_source.getIntArray().getData()[index]);
                }
            }
            index++;
        }
    }

    public static void processPipes(final LinkedList<ColladaInputPipe> pipes, final int[] currentVal) {
        // go through our pipes. use the indices in currentVal to pull the correct float val
        // from our source and set into our buffer.
        for (final ColladaInputPipe pipe : pipes) {
            pipe.pushValues(currentVal[pipe._offset]);
        }
    }
}
