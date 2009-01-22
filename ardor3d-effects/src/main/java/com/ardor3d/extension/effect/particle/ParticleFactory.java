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

import com.ardor3d.scenegraph.Mesh;

public class ParticleFactory {

    public static ParticleMesh buildParticles(final String name, final int number) {
        return buildParticles(name, number, ParticleSystem.ParticleType.Quad);
    }

    public static ParticleMesh buildParticles(final String name, final int number,
            final ParticleSystem.ParticleType particleType) {
        if (particleType != ParticleSystem.ParticleType.Triangle && particleType != ParticleSystem.ParticleType.Quad) {
            throw new IllegalArgumentException(
                    "particleType should be either ParticleSystem.ParticleType.TRIANGLE or ParticleSystem.ParticleType.QUAD");
        }
        final ParticleMesh particleMesh = new ParticleMesh(name, number, particleType);
        final ParticleController particleController = new ParticleController(particleMesh);
        particleMesh.addController(particleController);
        return particleMesh;
    }

    public static ParticleMesh buildMeshParticles(final String name, final Mesh mesh) {
        final ParticleMesh particleMesh = new ParticleMesh(name, mesh);
        final ParticleController particleController = new ParticleController(particleMesh);
        particleMesh.addController(particleController);
        return particleMesh;
    }

}
