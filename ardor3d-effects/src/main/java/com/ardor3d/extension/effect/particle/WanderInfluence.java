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
import java.util.ArrayList;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

public class WanderInfluence extends ParticleInfluence {

    public static final double DEFAULT_RADIUS = .03f;
    public static final double DEFAULT_DISTANCE = .2f;
    public static final double DEFAULT_JITTER = .005f;

    private double wanderRadius = DEFAULT_RADIUS;
    private double wanderDistance = DEFAULT_DISTANCE;
    private double wanderJitter = DEFAULT_JITTER;

    private ArrayList<Vector3> wanderTargets = new ArrayList<Vector3>(1);
    private final Vector3 workVect = new Vector3();

    @Override
    public void prepare(final ParticleSystem system) {
        if (wanderTargets.size() != system.getNumParticles()) {
            wanderTargets = new ArrayList<Vector3>(system.getNumParticles());
            for (int x = system.getNumParticles(); --x >= 0;) {
                wanderTargets.add(new Vector3(system.getEmissionDirection()).normalizeLocal());
            }
        }
    }

    @Override
    public void apply(final double dt, final Particle particle, final int index) {
        if (wanderRadius == 0 && wanderDistance == 0 && wanderJitter == 0) {
            return;
        }

        final Vector3 wanderTarget = wanderTargets.get(index);

        wanderTarget.addLocal(calcNewJitter(), calcNewJitter(), calcNewJitter());
        wanderTarget.normalizeLocal();
        wanderTarget.multiplyLocal(wanderRadius);

        workVect.set(particle.getVelocity()).normalizeLocal().multiplyLocal(wanderDistance);
        workVect.addLocal(wanderTarget).normalizeLocal();
        workVect.multiplyLocal(particle.getVelocity().length());
        particle.getVelocity().set(workVect);
    }

    private double calcNewJitter() {
        return ((MathUtils.nextRandomFloat() * 2.0f) - 1.0f) * wanderJitter;
    }

    public double getWanderDistance() {
        return wanderDistance;
    }

    public void setWanderDistance(final double wanderDistance) {
        this.wanderDistance = wanderDistance;
    }

    public double getWanderJitter() {
        return wanderJitter;
    }

    public void setWanderJitter(final double wanderJitter) {
        this.wanderJitter = wanderJitter;
    }

    public double getWanderRadius() {
        return wanderRadius;
    }

    public void setWanderRadius(final double wanderRadius) {
        this.wanderRadius = wanderRadius;
    }

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);
        final OutputCapsule cap = e.getCapsule(this);
        cap.write(wanderRadius, "wanderRadius", DEFAULT_RADIUS);
        cap.write(wanderDistance, "wanderDistance", DEFAULT_DISTANCE);
        cap.write(wanderJitter, "wanderJitter", DEFAULT_JITTER);
    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule cap = e.getCapsule(this);
        wanderRadius = cap.readDouble("wanderRadius", DEFAULT_RADIUS);
        wanderDistance = cap.readDouble("wanderDistance", DEFAULT_DISTANCE);
        wanderJitter = cap.readDouble("wanderJitter", DEFAULT_JITTER);
    }

    @Override
    public Class<? extends WanderInfluence> getClassTag() {
        return getClass();
    }
}
