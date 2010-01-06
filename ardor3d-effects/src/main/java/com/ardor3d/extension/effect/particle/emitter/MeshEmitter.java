/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.effect.particle.emitter;

import java.io.IOException;

import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

public class MeshEmitter extends SavableParticleEmitter {

    private Mesh _source;
    private boolean _onlyVertices;

    public MeshEmitter() {}

    /**
     * @param source
     *            the mesh to use as our source
     * @param onlyVertices
     *            if true, only the vertices of the emitter should be used for spawning particles. Otherwise, the mesh's
     *            face surfaces should be used.
     */
    public MeshEmitter(final Mesh source, final boolean onlyVertices) {
        _source = source;
        _onlyVertices = onlyVertices;
    }

    public void setSource(final Mesh source) {
        _source = source;
    }

    public Mesh getSource() {
        return _source;
    }

    public void setOnlyVertices(final boolean onlyVertices) {
        _onlyVertices = onlyVertices;
    }

    public boolean isOnlyVertices() {
        return _onlyVertices;
    }

    public Vector3 randomEmissionPoint(final Vector3 store) {
        Vector3 rVal = store;
        if (rVal == null) {
            rVal = new Vector3();
        }

        if (_onlyVertices) {
            getSource().getMeshData().randomVertex(rVal);
        } else {
            getSource().getMeshData().randomPointOnPrimitives(rVal);
        }
        return rVal;
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    public void read(final Ardor3DImporter im) throws IOException {
        final InputCapsule capsule = im.getCapsule(this);
        _source = (Mesh) capsule.readSavable("source", null);
        _onlyVertices = capsule.readBoolean("onlyVertices", false);
    }

    public void write(final Ardor3DExporter ex) throws IOException {
        final OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(_source, "source", null);
        capsule.write(_onlyVertices, "onlyVertices", false);
    }
}
