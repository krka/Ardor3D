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
import com.ardor3d.extension.effect.particle.RampEntry;
import com.ardor3d.framework.FrameHandler;
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
import com.google.inject.Inject;

public class ParticleRampExample extends ExampleBase {

    public static void main(final String[] args) {
        start(ParticleRampExample.class);
    }

    @Inject
    public ParticleRampExample(final LogicalLayer layer, final FrameHandler frameWork) {
        super(layer, frameWork);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Particle System - Example");
        _lightState.setEnabled(false);

        final ParticleSystem particles = ParticleFactory.buildParticles("particles", 1);
        particles.setEmissionDirection(new Vector3(0, 1, 0));
        particles.setInitialVelocity(0);
        particles.setMinimumLifeTime(2500);
        particles.setMaximumLifeTime(2500);
        particles.setMaximumAngle(45 * MathUtils.DEG_TO_RAD);
        particles.getParticleController().setControlFlow(false);
        particles.setParticlesInWorldCoords(true);

        // Start color is RED, opaque
        particles.setStartColor(new ColorRGBA(1, 0, 0, 1));
        particles.setStartSize(2.5);

        // At 25% life, let's have the color be WHITE, opaque
        final RampEntry entry25 = new RampEntry(.25);
        entry25.setColor(new ColorRGBA(1, 1, 1, 1));
        particles.getRamp().addEntry(entry25);

        // At 50% life, (25% higher than previous) let's have the color be RED, opaque and twice as big.
        // Note that at 25% life the size will be about 3.75 since we did not set a size on that.
        final RampEntry entry50 = new RampEntry(.25);
        entry50.setColor(new ColorRGBA(1, 0, 0, 1));
        entry50.setSize(5);
        particles.getRamp().addEntry(entry50);

        // At 75% life, (25% higher than previous) let's have the color be WHITE, opaque
        final RampEntry entry75 = new RampEntry(.25);
        entry75.setColor(new ColorRGBA(1, 1, 1, 1));
        particles.getRamp().addEntry(entry75);

        // End color is BLUE, opaque (size is back to 2.5 now.
        particles.setEndColor(new ColorRGBA(0, 0, 1, 1));
        particles.setEndSize(2.5);

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
