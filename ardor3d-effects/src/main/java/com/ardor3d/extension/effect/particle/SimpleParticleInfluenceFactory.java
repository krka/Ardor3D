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

import com.ardor3d.math.Line3;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

public final class SimpleParticleInfluenceFactory {

    public static class BasicWind extends ParticleInfluence {
        private double strength;
        private Vector3 windDirection;
        private boolean random, rotateWithScene;
        private final Vector3 vector = new Vector3();

        public BasicWind() {}

        public BasicWind(final double windStr, final Vector3 windDir, final boolean addRandom,
                final boolean rotateWithScene) {
            strength = windStr;
            windDirection = windDir;
            random = addRandom;
            this.rotateWithScene = rotateWithScene;
        }

        public double getStrength() {
            return strength;
        }

        public void setStrength(final double windStr) {
            strength = windStr;
        }

        public Vector3 getWindDirection() {
            return windDirection;
        }

        public void setWindDirection(final Vector3 windDir) {
            windDirection = windDir;
        }

        public boolean isRandom() {
            return random;
        }

        public void setRandom(final boolean addRandom) {
            random = addRandom;
        }

        public boolean isRotateWithScene() {
            return rotateWithScene;
        }

        public void setRotateWithScene(final boolean rotateWithScene) {
            this.rotateWithScene = rotateWithScene;
        }

        @Override
        public void prepare(final ParticleSystem system) {
            vector.set(windDirection);
            if (rotateWithScene) {
                system.getEmitterTransform().applyForwardVector(vector);
            }
        }

        @Override
        public void apply(final double dt, final Particle p, final int index) {
            final double tStr = (random ? MathUtils.nextRandomFloat() * strength : strength);
            vector.scaleAdd(tStr * dt, p.getVelocity(), p.getVelocity());
        }

        @Override
        public void write(final Ardor3DExporter e) throws IOException {
            super.write(e);
            final OutputCapsule capsule = e.getCapsule(this);
            capsule.write(strength, "strength", 1f);
            capsule.write(windDirection, "windDirection", new Vector3(Vector3.UNIT_X));
            capsule.write(random, "random", false);
            capsule.write(rotateWithScene, "rotateWithScene", true);
        }

        @Override
        public void read(final Ardor3DImporter e) throws IOException {
            super.read(e);
            final InputCapsule capsule = e.getCapsule(this);
            strength = capsule.readFloat("strength", 1f);
            windDirection = (Vector3) capsule.readSavable("windDirection", new Vector3(Vector3.UNIT_X));
            random = capsule.readBoolean("random", false);
            rotateWithScene = capsule.readBoolean("rotateWithScene", true);
        }

        @Override
        public Class<? extends BasicWind> getClassTag() {
            return this.getClass();
        }
    }

    public static class BasicGravity extends ParticleInfluence {
        private Vector3 gravity;
        private boolean rotateWithScene;
        private final Vector3 vector = new Vector3();

        public BasicGravity() {}

        public BasicGravity(final Vector3 gravForce, final boolean rotateWithScene) {
            gravity = new Vector3(gravForce);
            this.rotateWithScene = rotateWithScene;
        }

        public Vector3 getGravityForce() {
            return gravity;
        }

        public void setGravityForce(final Vector3 gravForce) {
            gravity = gravForce;
        }

        public boolean isRotateWithScene() {
            return rotateWithScene;
        }

        public void setRotateWithScene(final boolean rotateWithScene) {
            this.rotateWithScene = rotateWithScene;
        }

        @Override
        public void prepare(final ParticleSystem system) {
            vector.set(gravity);
            if (rotateWithScene) {
                system.getEmitterTransform().applyForwardVector(vector);
            }
        }

        @Override
        public void apply(final double dt, final Particle p, final int index) {
            vector.scaleAdd(dt, p.getVelocity(), p.getVelocity());
        }

        @Override
        public void write(final Ardor3DExporter e) throws IOException {
            super.write(e);
            final OutputCapsule capsule = e.getCapsule(this);
            capsule.write(gravity, "gravity", new Vector3(Vector3.ZERO));
            capsule.write(rotateWithScene, "rotateWithScene", true);
        }

        @Override
        public void read(final Ardor3DImporter e) throws IOException {
            super.read(e);
            final InputCapsule capsule = e.getCapsule(this);
            gravity = (Vector3) capsule.readSavable("gravity", new Vector3(Vector3.ZERO));
            rotateWithScene = capsule.readBoolean("rotateWithScene", true);
        }

        @Override
        public Class<? extends BasicGravity> getClassTag() {
            return this.getClass();
        }
    }

    public static class BasicDrag extends ParticleInfluence {
        private final Vector3 velocity = new Vector3();
        private double dragCoefficient;

        public BasicDrag() {}

        public BasicDrag(final double dragCoef) {
            dragCoefficient = dragCoef;
        }

        public double getDragCoefficient() {
            return dragCoefficient;
        }

        public void setDragCoefficient(final double dragCoef) {
            dragCoefficient = dragCoef;
        }

        @Override
        public void apply(final double dt, final Particle p, final int index) {
            // viscous drag
            velocity.set(p.getVelocity());
            p.getVelocity().addLocal(velocity.multiplyLocal(-dragCoefficient * dt * p.getInvMass()));
        }

        @Override
        public void write(final Ardor3DExporter e) throws IOException {
            super.write(e);
            final OutputCapsule capsule = e.getCapsule(this);
            capsule.write(dragCoefficient, "dragCoefficient", 1f);
        }

        @Override
        public void read(final Ardor3DImporter e) throws IOException {
            super.read(e);
            final InputCapsule capsule = e.getCapsule(this);
            dragCoefficient = capsule.readFloat("dragCoefficient", 1f);
        }

        @Override
        public Class<? extends BasicDrag> getClassTag() {
            return this.getClass();
        }
    }

    public static class BasicVortex extends ParticleInfluence {

        public static final int VT_CYLINDER = 0;
        public static final int VT_TORUS = 1;

        private int type = VT_CYLINDER;
        private double strength, divergence, height, radius;
        private Line3 axis;
        private boolean random, transformWithScene;
        private final Vector3 v1 = new Vector3(), v2 = new Vector3(), v3 = new Vector3();
        private final Quaternion rot = new Quaternion();
        private final Line3 line = new Line3();

        public BasicVortex() {}

        public BasicVortex(final double strength, final double divergence, final Line3 axis, final boolean random,
                final boolean transformWithScene) {
            this.strength = strength;
            this.divergence = divergence;
            this.axis = axis;
            height = 0f;
            radius = 1f;
            this.random = random;
            this.transformWithScene = transformWithScene;
        }

        public int getType() {
            return type;
        }

        public void setType(final int type) {
            this.type = type;
        }

        public double getStrength() {
            return strength;
        }

        public void setStrength(final double strength) {
            this.strength = strength;
        }

        public double getDivergence() {
            return divergence;
        }

        public void setDivergence(final double divergence) {
            this.divergence = divergence;
        }

        public Line3 getAxis() {
            return axis;
        }

        public void setAxis(final Line3 axis) {
            this.axis = axis;
        }

        public double getHeight() {
            return height;
        }

        public void setHeight(final double height) {
            this.height = height;
        }

        public double getRadius() {
            return radius;
        }

        public void setRadius(final double radius) {
            this.radius = radius;
        }

        public boolean isRandom() {
            return random;
        }

        public void setRandom(final boolean random) {
            this.random = random;
        }

        public boolean isTransformWithScene() {
            return transformWithScene;
        }

        public void setTransformWithScene(final boolean transformWithScene) {
            this.transformWithScene = transformWithScene;
        }

        @Override
        public void prepare(final ParticleSystem system) {
            line.setOrigin(axis.getOrigin());
            line.setDirection(axis.getDirection());
            if (transformWithScene) {
                final Vector3 temp = Vector3.fetchTempInstance();
                system.getEmitterTransform().applyForward(line.getOrigin(), temp);
                line.setOrigin(temp);
                system.getEmitterTransform().applyForwardVector(line.getDirection(), temp);
                line.setDirection(temp);
                Vector3.releaseTempInstance(temp);
            }
            if (type == VT_CYLINDER) {
                rot.fromAngleAxis(-divergence, line.getDirection());
            }
        }

        @Override
        public void apply(final double dt, final Particle p, final int index) {
            final double dtStr = dt * strength * (random ? MathUtils.nextRandomFloat() : 1f);
            p.getPosition().subtract(line.getOrigin(), v1);
            line.getDirection().cross(v1, v2);
            if (v2.length() == 0) { // particle is on the axis
                return;
            }
            v2.normalizeLocal();
            if (type == VT_CYLINDER) {
                rot.apply(v2, v2);
                v2.scaleAdd(dtStr, p.getVelocity(), p.getVelocity());
                return;
            }
            v2.cross(line.getDirection(), v1);
            v1.multiplyLocal(radius);
            line.getDirection().scaleAdd(height, v1, v1);
            v1.addLocal(line.getOrigin());
            v1.subtractLocal(p.getPosition());
            if (v1.length() == 0) { // particle is on the ring
                return;
            }
            v1.normalizeLocal();
            v1.cross(v2, v3);
            rot.fromAngleAxis(-divergence, v2);
            rot.apply(v3, v3);
            v3.scaleAdd(dtStr, p.getVelocity(), p.getVelocity());
        }

        @Override
        public void write(final Ardor3DExporter e) throws IOException {
            super.write(e);
            final OutputCapsule capsule = e.getCapsule(this);
            capsule.write(type, "type", VT_CYLINDER);
            capsule.write(strength, "strength", 1f);
            capsule.write(divergence, "divergence", 0f);
            capsule.write(axis, "axis", new Line3(new Vector3(), new Vector3(Vector3.UNIT_Y)));
            capsule.write(height, "height", 0f);
            capsule.write(radius, "radius", 1f);
            capsule.write(random, "random", false);
            capsule.write(transformWithScene, "transformWithScene", true);
        }

        @Override
        public void read(final Ardor3DImporter e) throws IOException {
            super.read(e);
            final InputCapsule capsule = e.getCapsule(this);
            type = capsule.readInt("type", VT_CYLINDER);
            strength = capsule.readFloat("strength", 1f);
            divergence = capsule.readFloat("divergence", 0f);
            axis = (Line3) capsule.readSavable("axis", new Line3(new Vector3(), new Vector3(Vector3.UNIT_Y)));
            height = capsule.readFloat("height", 0f);
            radius = capsule.readFloat("radius", 1f);
            random = capsule.readBoolean("random", false);
            transformWithScene = capsule.readBoolean("transformWithScene", true);
        }

        @Override
        public Class<? extends BasicVortex> getClassTag() {
            return this.getClass();
        }
    }

    /**
     * Not used.
     */
    private SimpleParticleInfluenceFactory() {}

    /**
     * Creates a basic wind that always blows in a single direction.
     * 
     * @param windStr
     *            Max strength of wind.
     * @param windDir
     *            Direction wind should blow.
     * @param addRandom
     *            randomly alter the strength of the wind by 0-100%
     * @param rotateWithScene
     *            rotate the wind direction with the particle system
     * @return ParticleInfluence
     */
    public static ParticleInfluence createBasicWind(final double windStr, final Vector3 windDir,
            final boolean addRandom, final boolean rotateWithScene) {
        return new BasicWind(windStr, windDir, addRandom, rotateWithScene);
    }

    /**
     * Create a basic gravitational force.
     * 
     * @param rotateWithScene
     *            rotate the gravity vector with the particle system
     * @return ParticleInfluence
     */
    public static ParticleInfluence createBasicGravity(final Vector3 gravForce, final boolean rotateWithScene) {
        return new BasicGravity(gravForce, rotateWithScene);
    }

    /**
     * Create a basic drag force that will use the given drag coefficient. Drag is determined by figuring the current
     * velocity and reversing it, then multiplying by the drag coefficient and dividing by the particle mass.
     * 
     * @param dragCoef
     *            Should be positive. Larger values mean more drag but possibly more instability.
     * @return ParticleInfluence
     */
    public static ParticleInfluence createBasicDrag(final double dragCoef) {
        return new BasicDrag(dragCoef);
    }

    /**
     * Creates a basic vortex.
     * 
     * @param strength
     *            Max strength of vortex.
     * @param divergence
     *            The divergence in radians from the tangent vector
     * @param axis
     *            The center of the vortex.
     * @param random
     *            randomly alter the strength of the vortex by 0-100%
     * @param transformWithScene
     *            transform the axis with the particle system
     * @return ParticleInfluence
     */
    public static ParticleInfluence createBasicVortex(final double strength, final double divergence, final Line3 axis,
            final boolean random, final boolean transformWithScene) {
        return new BasicVortex(strength, divergence, axis, random, transformWithScene);
    }
}
