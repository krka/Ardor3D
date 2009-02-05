/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.effect;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.extension.effect.particle.ParticleFactory;
import com.ardor3d.extension.effect.particle.ParticleSystem;
import com.ardor3d.framework.FrameWork;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Image.Format;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.Timer;
import com.google.inject.Inject;

public class ParticleSystemExample extends ExampleBase {
    private final Timer _timer;

    private ParticleSystem particles;
    private final Vector3 currentPos = new Vector3(), newPos = new Vector3();

    public static void main(final String[] args) {
        start(ParticleSystemExample.class);
    }

    @Inject
    public ParticleSystemExample(final LogicalLayer layer, final FrameWork frameWork, final Timer timer) {
        super(layer, frameWork);
        _timer = timer;
    }

    @Override
    protected void updateExample(final double tpf) {
        if ((int) currentPos.getX() == (int) newPos.getX() && (int) currentPos.getY() == (int) newPos.getY()
                && (int) currentPos.getZ() == (int) newPos.getZ()) {
            newPos.setX(MathUtils.nextRandomDouble() * 50 - 25);
            newPos.setY(MathUtils.nextRandomDouble() * 50 - 25);
            newPos.setZ(MathUtils.nextRandomDouble() * 50 - 150);
        }

        final double frameRate = _timer.getFrameRate() / 2;
        currentPos.setX(currentPos.getX() - (currentPos.getX() - newPos.getX()) / frameRate);
        currentPos.setY(currentPos.getY() - (currentPos.getY() - newPos.getY()) / frameRate);
        currentPos.setZ(currentPos.getZ() - (currentPos.getZ() - newPos.getZ()) / frameRate);

        _root.setTranslation(currentPos);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Particle System - Example");
        _lightState.setEnabled(false);

        particles = ParticleFactory.buildParticles("particles", 300);
        particles.setEmissionDirection(new Vector3(0, 1, 0));
        particles.setInitialVelocity(.006);
        particles.setStartSize(2.5);
        particles.setEndSize(.5);
        particles.setMinimumLifeTime(1200);
        particles.setMaximumLifeTime(1400);
        particles.setStartColor(new ColorRGBA(1, 0, 0, 1));
        particles.setEndColor(new ColorRGBA(0, 1, 0, 0));
        particles.setMaximumAngle(360 * MathUtils.DEG_TO_RAD);
        particles.getParticleController().setControlFlow(false);
        particles.setParticlesInWorldCoords(true);
        particles.warmUp(60);

        final BlendState blend = new BlendState();
        blend.setBlendEnabled(true);
        blend.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
        blend.setDestinationFunction(BlendState.DestinationFunction.One);
        particles.setRenderState(blend);

        final TextureState ts = new TextureState();
        ts.setTexture(TextureManager.load("images/flaresmall.jpg", Texture.MinificationFilter.Trilinear, Format.Guess,
                true));
        ts.setEnabled(true);
        particles.setRenderState(ts);

        final ZBufferState zstate = new ZBufferState();
        zstate.setEnabled(false);
        particles.setRenderState(zstate);

        particles.getParticleGeometry().setModelBound(new BoundingBox());

        _root.attachChild(particles);
    }
}
