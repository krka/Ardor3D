/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.jogl;

import java.util.logging.Logger;

import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.TextureRenderer;
import com.ardor3d.renderer.TextureRendererProvider;

public class JoglTextureRendererProvider implements TextureRendererProvider {

    private static final Logger logger = Logger.getLogger(JoglTextureRendererProvider.class.getName());

    public TextureRenderer createTextureRenderer(final DisplaySettings settings, final Renderer renderer,
            final ContextCapabilities caps, final TextureRenderer.Target target) {

        if (caps.isFBOSupported()) {
            return new JoglTextureRenderer(settings, target, renderer, caps);
        } else if (caps.isPbufferSupported()) {
            return new JoglPbufferTextureRenderer(settings, target, renderer);
        } else {
            logger.severe("No texture renderer support (FBO or Pbuffer).");
            return null;
        }

    }

}
