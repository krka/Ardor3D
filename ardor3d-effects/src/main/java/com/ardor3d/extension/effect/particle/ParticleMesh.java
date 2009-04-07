/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.effect.particle;

import java.io.IOException;

import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

/**
 * ParticleMesh is a particle system that uses Mesh as its underlying geometric data.
 */
public class ParticleMesh extends ParticleSystem {

    private static final long serialVersionUID = 2L;

    private boolean _useMeshTexCoords = true;
    private boolean _useTriangleNormalEmit = true;

    public ParticleMesh() {}

    public ParticleMesh(final String name, final int numParticles) {
        super(name, numParticles);
        setRenderBucketType(RenderBucketType.Transparent);
        setLightCombineMode(LightCombineMode.Off);
        setTextureCombineMode(TextureCombineMode.Replace);
    }

    public ParticleMesh(final String name, final int numParticles, final ParticleSystem.ParticleType type) {
        super(name, numParticles, type);
        setRenderBucketType(RenderBucketType.Transparent);
        setLightCombineMode(LightCombineMode.Off);
        setTextureCombineMode(TextureCombineMode.Replace);
    }

    public ParticleMesh(final String name, final Mesh geom) {
        super(name, 0, ParticleSystem.ParticleType.GeomMesh);
        _numParticles = geom.getMeshData().getTotalPrimitiveCount();
        _psGeom = geom;
        setRenderBucketType(RenderBucketType.Transparent);
        setLightCombineMode(LightCombineMode.Off);
        setTextureCombineMode(TextureCombineMode.Replace);
        initializeParticles(_numParticles);
    }

    @Override
    protected void initializeParticles(final int numParticles) {

        if (_particleGeom != null) {
            detachChild(_particleGeom);
        }
        final Mesh mesh = new Mesh(getName() + "_mesh") {
            private static final long serialVersionUID = 1L;

            @Override
            public void updateWorldTransform(final boolean recurse) {
                ; // Do nothing.
            }
        };
        _particleGeom = mesh;
        attachChild(mesh);
        _particles = new Particle[numParticles];
        if (numParticles == 0) {
            return;
        }
        Vector2 sharedTextureData[];

        // setup texture coords
        switch (getParticleType()) {
            case GeomMesh:
            case Triangle:
                sharedTextureData = new Vector2[] { new Vector2(0.0, 0.0), new Vector2(0.0, 2.0), new Vector2(2.0, 0.0) };
                break;
            case Quad:
                sharedTextureData = new Vector2[] { new Vector2(0.0, 0.0), new Vector2(0.0, 1.0),
                        new Vector2(1.0, 0.0), new Vector2(1.0, 1.0) };
                break;
            default:
                throw new IllegalStateException(
                        "Particle Mesh may only have particle type of ParticleType.Quad, ParticleType.GeomMesh or ParticleType.Triangle");
        }

        final int verts = getVertsForParticleType(getParticleType());

        _geometryCoordinates = BufferUtils.createVector3Buffer(numParticles * verts);

        // setup indices
        int[] indices;
        switch (getParticleType()) {
            case Triangle:
            case GeomMesh:
                indices = new int[numParticles * 3];
                for (int j = 0; j < numParticles; j++) {
                    indices[0 + j * 3] = j * 3 + 2;
                    indices[1 + j * 3] = j * 3 + 1;
                    indices[2 + j * 3] = j * 3 + 0;
                }
                break;
            case Quad:
                indices = new int[numParticles * 6];
                for (int j = 0; j < numParticles; j++) {
                    indices[0 + j * 6] = j * 4 + 2;
                    indices[1 + j * 6] = j * 4 + 1;
                    indices[2 + j * 6] = j * 4 + 0;

                    indices[3 + j * 6] = j * 4 + 2;
                    indices[4 + j * 6] = j * 4 + 3;
                    indices[5 + j * 6] = j * 4 + 1;
                }
                break;
            default:
                throw new IllegalStateException(
                        "Particle Mesh may only have particle type of ParticleType.Quad, ParticleType.GeomMesh or ParticleType.Triangle");
        }

        _appearanceColors = BufferUtils.createColorBuffer(numParticles * verts);

        mesh.getMeshData().setVertexBuffer(_geometryCoordinates);
        mesh.getMeshData().setColorBuffer(_appearanceColors);
        mesh.getMeshData().setTextureBuffer(BufferUtils.createVector2Buffer(numParticles * verts), 0);
        mesh.getMeshData().setIndexBuffer(BufferUtils.createIntBuffer(indices));

        final Vector3 temp = Vector3.fetchTempInstance();
        for (int k = 0; k < numParticles; k++) {
            _particles[k] = new Particle(this);
            _particles[k].init();
            _particles[k].setStartIndex(k * verts);
            for (int a = verts - 1; a >= 0; a--) {
                final int ind = (k * verts) + a;
                if (_particleType == ParticleSystem.ParticleType.GeomMesh && _useMeshTexCoords) {
                    final int index = _psGeom.getMeshData().getIndexBuffer() != null ? _psGeom.getMeshData()
                            .getIndexBuffer().get(ind) : ind;
                    BufferUtils.populateFromBuffer(temp, _psGeom.getMeshData().getTextureCoords(0).getBuffer(), index);
                    BufferUtils.setInBuffer(temp, mesh.getMeshData().getTextureCoords(0).getBuffer(), ind);
                } else {
                    BufferUtils.setInBuffer(sharedTextureData[a], mesh.getMeshData().getTextureCoords(0).getBuffer(), ind);
                }
                BufferUtils.setInBuffer(_particles[k].getCurrentColor(), _appearanceColors, (ind));
            }

        }
        Vector3.releaseTempInstance(temp);
        updateWorldRenderStates(true);
        _particleGeom.setCastsShadows(false);
    }

    @Override
    public void draw(final Renderer r) {
        final Camera camera = Camera.getCurrentCamera();
        for (int i = 0; i < _particles.length; i++) {
            final Particle particle = _particles[i];
            if (particle.getStatus() == Particle.Status.Alive) {
                particle.updateVerts(camera);
            }
        }

        if (!_particlesInWorldCoords) {
            getParticleGeometry().setWorldTranslation(getWorldTranslation());
            getParticleGeometry().setWorldRotation(getWorldRotation());
        } else {
            getParticleGeometry().setWorldTranslation(Vector3.ZERO);
            getParticleGeometry().setWorldRotation(Matrix3.IDENTITY);
        }
        getParticleGeometry().setWorldScale(getWorldScale());

        getParticleGeometry().draw(r);
    }

    @Override
    public void resetParticleVelocity(final int i) {
        if (_particleType == ParticleSystem.ParticleType.GeomMesh && _useTriangleNormalEmit) {
            _particles[i].getVelocity().set(_particles[i].getTriangleModel().getNormal());
            _particles[i].getVelocity().multiplyLocal(_emissionDirection);
            _particles[i].getVelocity().multiplyLocal(getInitialVelocity());
        } else {
            super.resetParticleVelocity(i);
        }
    }

    public boolean isUseMeshTexCoords() {
        return _useMeshTexCoords;
    }

    public void setUseMeshTexCoords(final boolean useMeshTexCoords) {
        _useMeshTexCoords = useMeshTexCoords;
    }

    public boolean isUseTriangleNormalEmit() {
        return _useTriangleNormalEmit;
    }

    public void setUseTriangleNormalEmit(final boolean useTriangleNormalEmit) {
        _useTriangleNormalEmit = useTriangleNormalEmit;
    }

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(_useMeshTexCoords, "useMeshTexCoords", true);
        capsule.write(_useTriangleNormalEmit, "useTriangleNormalEmit", true);
    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);
        _useMeshTexCoords = capsule.readBoolean("useMeshTexCoords", true);
        _useTriangleNormalEmit = capsule.readBoolean("useTriangleNormalEmit", true);
    }

    @Override
    public Mesh getParticleGeometry() {
        return _particleGeom;
    }
}
