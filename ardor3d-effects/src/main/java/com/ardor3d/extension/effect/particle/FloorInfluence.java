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

import com.ardor3d.math.Plane;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;

public class FloorInfluence extends ParticleInfluence {

    /**
     * Bouncyness is the factor of multiplication when bouncing off the floor. A bouncyness factor of 1 means the ball
     * leaves the floor with the same velocity as it hit the floor.
     */
    private double bouncyness = 1;

    /**
     * The normal vector of the floor. Same semantics as in math.Plane.
     */
    private Vector3 normal;

    /**
     * The position vector for the (imaginary) center of the floor.
     */
    private Vector3 pos;

    private Plane floor;

    /**
     * @param pos
     *            The position vector for the (imaginary) center of the floor.
     * @param normal
     *            The normal vector of the floor. Same semantics as in math.Plane.
     * @param bouncynessBouncyness
     *            is the factor of multiplication when bouncing off the floor. A bouncyness factor of 1 means the ball
     *            leaves the floor with the same velocity as it hit the floor, much like a rubber ball.
     */
    public FloorInfluence(final Vector3 pos, final Vector3 normal, final double bouncyness) {
        this.bouncyness = bouncyness;
        this.normal = normal;
        this.pos = pos;
        floor = new Plane(normal, normal.dot(pos));
    }

    @Override
    public void apply(final double dt, final Particle particle, final int index) {

        if (particle.getStatus() == Particle.Status.Alive && floor.pseudoDistance(particle.getPosition()) <= 0) {

            final Vector3 tempVect1 = Vector3.fetchTempInstance();
            final Vector3 tempVect2 = Vector3.fetchTempInstance();
            final double t = (floor.getNormal().dot(particle.getPosition()) - floor.getConstant())
                    / floor.getNormal().dot(particle.getVelocity());
            final Vector3 s = particle.getPosition().subtract(particle.getVelocity().multiply(t, tempVect1), tempVect1);

            normal.normalizeLocal();
            final Vector3 v1 = normal.cross(s.subtract(pos, s), tempVect1);
            final Vector3 v2 = normal.cross(v1, tempVect2);
            v1.normalizeLocal();
            v2.normalizeLocal();

            final Vector3 newVel = new Vector3(particle.getVelocity());
            newVel.setY(newVel.getY() * -bouncyness);
            final Quaternion q = new Quaternion();
            q.fromAxes(v1, normal, v2);
            q.apply(newVel, newVel);

            particle.setVelocity(newVel);

            Vector3.releaseTempInstance(tempVect1);
            Vector3.releaseTempInstance(tempVect2);
        }
    }

    public double getBouncyness() {
        return bouncyness;
    }

    public void setBouncyness(final double bouncyness) {
        this.bouncyness = bouncyness;
    }

    public Plane getFloor() {
        return floor;
    }

    public void setFloor(final Plane floor) {

        this.floor = floor;
    }

    public Vector3 getNormal() {
        return normal;
    }

    public void setNormal(final Vector3 normal) {
        this.normal = normal;
        floor = new Plane(normal, normal.dot(pos));
    }

    public Vector3 getPos() {
        return pos;
    }

    public void setPos(final Vector3 pos) {
        this.pos = pos;
        floor = new Plane(normal, normal.dot(pos));
    }

    @Override
    public Class<? extends FloorInfluence> getClassTag() {
        return this.getClass();
    }

}
