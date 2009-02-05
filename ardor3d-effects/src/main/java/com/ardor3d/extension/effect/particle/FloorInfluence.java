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
    private double _bouncyness = 1;

    /**
     * The normal vector of the floor. Same semantics as in math.Plane.
     */
    private Vector3 _normal;

    /**
     * The position vector for the (imaginary) center of the floor.
     */
    private Vector3 _pos;

    private Plane _floor;

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
        _bouncyness = bouncyness;
        _normal = normal;
        _pos = pos;
        _floor = new Plane(normal, normal.dot(pos));
    }

    @Override
    public void apply(final double dt, final Particle particle, final int index) {

        if (particle.getStatus() == Particle.Status.Alive && _floor.pseudoDistance(particle.getPosition()) <= 0) {

            final Vector3 tempVect1 = Vector3.fetchTempInstance();
            final Vector3 tempVect2 = Vector3.fetchTempInstance();
            final double t = (_floor.getNormal().dot(particle.getPosition()) - _floor.getConstant())
                    / _floor.getNormal().dot(particle.getVelocity());
            final Vector3 s = particle.getPosition().subtract(particle.getVelocity().multiply(t, tempVect1), tempVect1);

            _normal.normalizeLocal();
            final Vector3 v1 = _normal.cross(s.subtract(_pos, s), tempVect1);
            final Vector3 v2 = _normal.cross(v1, tempVect2);
            v1.normalizeLocal();
            v2.normalizeLocal();

            final Vector3 newVel = new Vector3(particle.getVelocity());
            newVel.setY(newVel.getY() * -_bouncyness);
            final Quaternion q = new Quaternion();
            q.fromAxes(v1, _normal, v2);
            q.apply(newVel, newVel);

            particle.setVelocity(newVel);

            Vector3.releaseTempInstance(tempVect1);
            Vector3.releaseTempInstance(tempVect2);
        }
    }

    public double getBouncyness() {
        return _bouncyness;
    }

    public void setBouncyness(final double bouncyness) {
        _bouncyness = bouncyness;
    }

    public Plane getFloor() {
        return _floor;
    }

    public void setFloor(final Plane floor) {

        _floor = floor;
    }

    public Vector3 getNormal() {
        return _normal;
    }

    public void setNormal(final Vector3 normal) {
        _normal = normal;
        _floor = new Plane(normal, normal.dot(_pos));
    }

    public Vector3 getPos() {
        return _pos;
    }

    public void setPos(final Vector3 pos) {
        _pos = pos;
        _floor = new Plane(_normal, _normal.dot(pos));
    }

    @Override
    public Class<? extends FloorInfluence> getClassTag() {
        return this.getClass();
    }

}
