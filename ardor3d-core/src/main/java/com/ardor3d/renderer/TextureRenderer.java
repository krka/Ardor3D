/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer;

import java.util.List;

import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture2D;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.scenegraph.Spatial;

/**
 * <code>TextureRenderer</code> defines an abstract class that handles rendering a scene to a buffer and copying it to a
 * texture. Creation of this object is usually handled via a TextureRendererFactory.
 */
public interface TextureRenderer {

    public enum Target {
        Texture1D, Texture2D, TextureCubeMap,
    }

    /**
     * <code>getCamera</code> retrieves the camera this renderer is using.
     * 
     * @return the camera this renderer is using.
     */
    Camera getCamera();

    /**
     * <code>render</code> renders a scene. As it receives a base class of <code>Spatial</code> the renderer hands off
     * management of the scene to spatial for it to determine when a <code>Geometry</code> leaf is reached. The result
     * of the rendering is then copied into the given texture(s). What is copied is based on the Texture object's
     * rttSource field.
     * 
     * NOTE: If more than one texture is given, copy-texture is used regardless of card capabilities to decrease render
     * time.
     * 
     * @param spat
     *            the scene to render.
     * @param tex
     *            the Texture to render it to.
     */
    void render(Spatial spat, Texture tex);

    /**
     * <code>render</code> renders a scene. As it receives a base class of <code>Spatial</code> the renderer hands off
     * management of the scene to spatial for it to determine when a <code>Geometry</code> leaf is reached. The result
     * of the rendering is then copied into the given texture(s). What is copied is based on the Texture object's
     * rttSource field.
     * 
     * NOTE: If more than one texture is given, copy-texture is used regardless of card capabilities to decrease render
     * time.
     * 
     * @param spat
     *            the scene to render.
     * @param tex
     *            the Texture to render it to.
     */
    void render(Spatial spat, Texture tex, boolean doClear);

    /**
     * <code>render</code> renders a scene. As it receives a base class of <code>Spatial</code> the renderer hands off
     * management of the scene to spatial for it to determine when a <code>Geometry</code> leaf is reached. The result
     * of the rendering is then copied into the given textures. What is copied is based on the Texture object's
     * rttSource field.
     * 
     * NOTE: If more than one texture is given, copy-texture is used regardless of card capabilities to decrease render
     * time.
     * 
     * @param spats
     *            an array of Spatials to render.
     * @param tex
     *            the Texture to render it to.
     */
    void render(List<? extends Spatial> spats, List<Texture> tex);

    /**
     * <code>render</code> renders a scene. As it receives a base class of <code>Spatial</code> the renderer hands off
     * management of the scene to spatial for it to determine when a <code>Geometry</code> leaf is reached. The result
     * of the rendering is then copied into the given textures. What is copied is based on the Texture object's
     * rttSource field.
     * 
     * NOTE: If more than one texture is given, copy-texture is used regardless of card capabilities to decrease render
     * time.
     * 
     * @param spats
     *            an array of Spatials to render.
     * @param tex
     *            the Texture to render it to.
     */
    void render(List<? extends Spatial> spats, List<Texture> tex, boolean doClear);

    /**
     * <code>setBackgroundColor</code> sets the color of window. This color will be shown for any pixel that is not set
     * via typical rendering operations.
     * 
     * @param c
     *            the color to set the background to.
     */
    void setBackgroundColor(ColorRGBA c);

    /**
     * <code>getBackgroundColor</code> retrieves the color used for the window background.
     * 
     * @return the background color that is currently set to the background.
     */
    ReadOnlyColorRGBA getBackgroundColor();

    /**
     * <code>setupTexture</code> initializes a Texture object for use with TextureRenderer. Generates a valid gl texture
     * id for this texture and sets up data storage for it. The texture will be equal to the pbuffer size.
     * 
     * Note that the pbuffer size is not necessarily what is specified in the constructor.
     * 
     * @param tex
     *            The texture to setup for use in Texture Rendering.
     */
    void setupTexture(Texture2D tex);

    /**
     * <code>copyToTexture</code> copies the current frame buffer contents to the given Texture. What is copied is based
     * on the rttSource of the texture object when it was setup.
     * 
     * @param tex
     *            The Texture to copy into.
     * @param width
     *            the width of the texture image
     * @param height
     *            the height of the texture image
     */
    void copyToTexture(Texture tex, int width, int height);

    /**
     * Any wrapping up and cleaning up of TextureRenderer information is performed here.
     */
    void cleanup();

    /**
     * Set up this textureRenderer for use with multiple targets. If you are going to use this texture renderer to
     * render to more than one texture, call this with true.
     * 
     * @param multi
     *            true if you plan to use this texture renderer to render different content to more than one texture.
     */
    void setMultipleTargets(boolean multi);

    int getWidth();

    int getHeight();

    /**
     * Enforce a particular state whenever this texture renderer is used. In other words, the given state will override
     * any state of the same type set on a scene object rendered with this texture renderer.
     * 
     * @param state
     *            state to enforce
     */
    void enforceState(final RenderState state);

    /**
     * @param state
     *            state to clear
     */
    void clearEnforcedState(final RenderState state);

    /**
     * Clear all enforced states on this texture renderer.
     */
    void clearEnforcedStates();
}
