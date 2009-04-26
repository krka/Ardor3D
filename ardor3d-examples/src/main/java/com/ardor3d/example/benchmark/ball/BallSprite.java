/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.benchmark.ball;

import com.ardor3d.framework.Canvas;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.scenegraph.shape.Quad;

class BallSprite extends Quad {

    private final Ball _ball;
    private final Canvas _canvas;

    public BallSprite(final String name, final double width, final double height, final Canvas canvas) {
        super(name, width, height);
        _ball = new Ball();
        _ball.setRandomPositionIn(canvas);
        _canvas = canvas;
        setTranslation(_ball._x + Ball.radius, _ball._y + Ball.radius, 0);
        setRenderBucketType(RenderBucketType.Ortho);
    }

    @Override
    public void updateGeometricState(final double time, final boolean initiator) {
        super.updateGeometricState(time, initiator);
        _ball.move(_canvas);
        setTranslation(_ball._x + Ball.radius, _ball._y + Ball.radius, 0);
    }

    public Ball getBall() {
        return _ball;
    }
}
