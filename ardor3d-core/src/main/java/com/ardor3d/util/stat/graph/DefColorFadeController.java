/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.stat.graph;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.scenegraph.Controller;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.Spatial.CullHint;

/**
 * <p>
 * A controller that changes over time the alpha value of the default color of a given Geometry. When coupled with an
 * appropriate BlendState, this can be used to fade in and out unlit objects.
 * </p>
 * 
 * <p>
 * An example of an appropriate BlendState to use with this class:
 * </p>
 * 
 * <pre>
 * BlendState blend = DisplaySystem.getDisplaySystem().getRenderer().createBlendState();
 * blend.setBlendEnabled(true);
 * blend.setSourceFunction(SourceFunction.SourceAlpha);
 * blend.setDestinationFunction(DestinationFunction.OneMinusSourceAlpha);
 * </pre>
 */
public class DefColorFadeController extends Controller {

    private static final long serialVersionUID = 1L;

    private Mesh target;
    private final float targetAlpha;
    private final double rate;
    private final boolean dir;

    /**
     * Sets up a new instance of the controller. The
     * 
     * @param target
     *            the object whose default color we want to change the alpha on.
     * @param targetAlpha
     *            the alpha value we want to end up at.
     * @param rate
     *            the amount, per second, to change the alpha. This value will be have its sign flipped if it is not the
     *            appropriate direction given the current default color's alpha.
     */
    public DefColorFadeController(final Mesh target, final float targetAlpha, double rate) {
        this.target = target;
        this.targetAlpha = targetAlpha;
        dir = target.getDefaultColor().getAlpha() > targetAlpha;
        if ((dir && rate > 0) || (!dir && rate < 0)) {
            rate *= -1;
        }
        this.rate = rate;
    }

    @Override
    public void update(final double time, final Spatial caller) {
        if (target == null) {
            return;
        }
        final ColorRGBA color = ColorRGBA.fetchTempInstance().set(target.getDefaultColor());
        float alpha = color.getAlpha();

        alpha += rate * time;
        if (dir && alpha <= targetAlpha) {
            alpha = targetAlpha;
        } else if (!dir && alpha >= targetAlpha) {
            alpha = targetAlpha;
        }

        if (alpha != 0) {
            target.setCullHint(CullHint.Inherit);
        } else {
            target.setCullHint(CullHint.Always);
        }

        color.setAlpha(alpha);
        target.setDefaultColor(color);
        ColorRGBA.releaseTempInstance(color);

        if (alpha == targetAlpha) {
            target.removeController(this);

            // enable gc
            target = null;
        }
    }

}
