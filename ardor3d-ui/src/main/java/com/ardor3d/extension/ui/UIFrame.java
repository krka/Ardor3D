/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui;

import java.nio.FloatBuffer;
import java.util.EnumSet;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.extension.ui.backdrop.SolidBackdrop;
import com.ardor3d.extension.ui.event.DragListener;
import com.ardor3d.extension.ui.layout.BorderLayout;
import com.ardor3d.extension.ui.layout.BorderLayoutData;
import com.ardor3d.extension.ui.util.UIQuad;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.Image.Format;
import com.ardor3d.image.Texture.MagnificationFilter;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.TextureRenderer;
import com.ardor3d.renderer.TextureRendererFactory;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.hint.TextureCombineMode;
import com.ardor3d.util.GameTaskQueueManager;

/**
 * A component similar to an inner frame in Swing. It can be dragged around the screen, minimized, expanded, closed, and
 * resized. Frames can also have their opacity individually assigned which will affect all elements drawn within them.
 */
public class UIFrame extends UIContainer {
    private static final Logger _logger = Logger.getLogger(UIFrame.class.getName());

    /** Minimum height we'll allow during manual resize */
    public static int MIN_FRAME_HEIGHT = 60;
    /** Minimum width we'll allow during manual resize */
    public static int MIN_FRAME_WIDTH = 100;

    /** If true, use frame opacity settings to blend the frames against the 3d content. Default is false. */
    private static boolean _useTransparency = false;

    /** The opacity of the frame currently being rendered. */
    private static float _currentOpacity = .75f;
    /** Flag indicating whether a current render operation is drawing a "cached" version of a frame. */
    private static boolean _drawingStandin = false;

    /** The main panel containing the contents panel and status bar of the frame. */
    private final UIPanel _basePanel;
    /** The panel meant to hold the contents of the frame. */
    private UIPanel _contentPanel;
    /** The top title bar of the frame, part of the frame's "chrome" */
    private final UIFrameBar _titleBar;
    /** The bar running along the bottom of the frame. */
    private final UIFrameStatusBar _statusBar;
    /** The opacity of this frame. 0 is fully transparent and 1 is fully opaque. The default is .75. */
    private float _frameOpacity = .75f;

    /** If true, show our title and status bars. */
    private boolean _decorated = true;
    /**
     * A flag indicating that some part of this frame needs repainting. On the next draw call, we should update our
     * cached texture, if using one.
     */
    private boolean _dirty = true;
    /** If true, show a resize handle on this frame and allow its use. */
    private boolean _resizeable = true;
    /** If true, use a cached texture to display this frame (on a simple quad) instead of drawing all of its components. */
    private boolean _useStandin = false;

    /** The drag listener responsible for allowing repositioning of the frame by dragging the title label. */
    private final FrameDragListener _dragListener = new FrameDragListener();

    /** The quad used to draw the cached texture version of this frame, if set to use one. */
    private UIQuad _standin = null;
    /** The texture used to store the contents of this frame. */
    private Texture2D _fakeTexture;

    /** A texture renderer to use for cache operations. */
    protected static TextureRenderer _textureRenderer;

    /**
     * Construct a new UIFrame with the given title and default buttons (CLOSE).
     * 
     * @param title
     *            the text to display on the title bar of this frame
     */
    public UIFrame(final String title) {
        this(title, EnumSet.of(FrameButtons.CLOSE));
    }

    /**
     * Construct a new UIFrame with the given title and button.
     * 
     * @param title
     *            the text to display on the title bar of this frame
     * @param buttons
     *            which buttons we should show in the frame bar.
     */
    public UIFrame(final String title, final EnumSet<FrameButtons> buttons) {
        setLayout(new BorderLayout());

        _basePanel = new UIPanel(new BorderLayout());
        _basePanel.setBackdrop(new SolidBackdrop(ColorRGBA.LIGHT_GRAY));
        _basePanel.setLayoutData(BorderLayoutData.CENTER);
        add(_basePanel);

        _contentPanel = new UIPanel();
        _contentPanel.setLayoutData(BorderLayoutData.CENTER);
        _basePanel.add(_contentPanel);

        _titleBar = new UIFrameBar(buttons);
        _titleBar.setLayoutData(BorderLayoutData.NORTH);
        setTitle(title);
        add(_titleBar);

        _statusBar = new UIFrameStatusBar();
        _statusBar.setLayoutData(BorderLayoutData.SOUTH);
        _basePanel.add(_statusBar);

        applySkin();
    }

    /**
     * @param decorated
     *            true to show the title and status bars. False to remove both. Undecorated frames have no resize or
     *            drag handles, or close buttons, etc.
     */
    public void setDecorated(final boolean decorated) {
        _decorated = decorated;
        if (!_decorated) {
            remove(_titleBar);
        } else {
            add(_titleBar);
        }

        if (!_decorated) {
            _basePanel.remove(_statusBar);
        } else {
            _basePanel.add(_statusBar);
        }
    }

    /**
     * @return true if this frame is decorated.
     */
    public boolean isDecorated() {
        return _decorated;
    }

    /**
     * @param resizeable
     *            true if we should allow resizing of this frame via a resize handle in the status bar. This does not
     *            stop programmatic resizing of this frame.
     */
    public void setResizeable(final boolean resizeable) {
        if (_resizeable != resizeable) {
            _resizeable = resizeable;

            if (!_resizeable) {
                _statusBar.remove(_statusBar.getResizeButton());
            } else {
                _statusBar.add(_statusBar.getResizeButton());
            }
            _statusBar.updateMinimumSizeFromContents();
            _statusBar.layout();
        }
    }

    /**
     * @return true if this frame allows manual resizing.
     */
    public boolean isResizeable() {
        return _resizeable;
    }

    /**
     * @param use
     *            if true, we will draw the frame contents to a cached texture and use that to display this frame (on a
     *            simple quad) instead of drawing all of its components each time. When the frame is marked as dirty, we
     *            will update the contents of the texture.
     */
    public void setUseStandin(final boolean use) {
        _useStandin = use;
        if (!_useStandin) {
            clearStandin();
        }
    }

    /**
     * 
     * @return true if we should use a cached texture copy to draw this frame.
     * @see #setUseStandin(boolean)
     */
    public boolean isUseStandin() {
        return _useStandin;
    }

    /**
     * Remove this frame from the hud it is attached to.
     * 
     * @throws IllegalStateException
     *             if frame is not currently attached to a hud.
     */
    public void close() {
        final UIHud hud = getHud();
        if (hud == null) {
            throw new IllegalStateException("UIFrame is not attached to a hud.");
        }

        // Remove our drag listener
        hud.removeDragListener(_dragListener);

        // When a frame closes, close any open tooltip
        hud.getTooltip().setVisible(false);

        // clear any resources for standin
        clearStandin();

        hud.remove(this);
        _parent = null;
    }

    /**
     * Centers this frame on the location of the given component.
     * 
     * @param comp
     *            the component to center on.
     */
    public void setLocationRelativeTo(final UIComponent comp) {
        int x = (comp.getComponentWidth() - getComponentWidth()) / 2;
        int y = (comp.getComponentHeight() - getComponentHeight()) / 2;
        x += comp.getHudX();
        y += comp.getHudY();
        setHudXY(x, y);
        updateGeometricState(0);
    }

    /**
     * Centers this frame on the view of the camera
     * 
     * @param cam
     *            the camera to center on.
     */
    public void setLocationRelativeTo(final Camera cam) {
        final int x = (cam.getWidth() - getComponentWidth()) / 2;
        final int y = (cam.getHeight() - getComponentHeight()) / 2;
        setHudXY(x, y);
        updateGeometricState(0);
    }

    /**
     * @return this frame's title bar
     */
    public UIFrameBar getTitleBar() {
        return _titleBar;
    }

    /**
     * @return this frame's status bar
     */
    public UIFrameStatusBar getStatusBar() {
        return _statusBar;
    }

    /**
     * @return the center content panel of this frame.
     */
    public UIPanel getContentPanel() {
        return _contentPanel;
    }

    /**
     * @return the base panel of this frame which holds the content panel and status bar.
     */
    public UIPanel getBasePanel() {
        return _basePanel;
    }

    /**
     * Replaces the content panel of this frame with a new one.
     * 
     * @param panel
     *            the new content panel.
     */
    public void setContentPanel(final UIPanel panel) {
        _basePanel.remove(_contentPanel);
        _contentPanel = panel;
        panel.setLayoutData(BorderLayoutData.CENTER);
        _basePanel.add(panel);
    }

    /**
     * @return the current title of this frame
     */
    public String getTitle() {
        if (_titleBar != null) {
            return _titleBar.getTitleLabel().getText();
        }

        return null;
    }

    /**
     * Sets the title of this frame
     * 
     * @param title
     *            the new title
     */
    public void setTitle(final String title) {
        if (_titleBar != null) {
            _titleBar.getTitleLabel().setText(title);
            _titleBar.layout();
        }
    }

    @Override
    public synchronized void draw(final Renderer renderer) {

        // if we are not using standins, just draw as a normal Node.
        if (!_useStandin) {
            super.draw(renderer);
            return;
        }

        final int width = getComponentWidth();
        final int height = getComponentHeight();
        final int dispWidth = Camera.getCurrentCamera().getWidth();
        final int dispHeight = Camera.getCurrentCamera().getHeight();

        // If we are currently in the process of rendering this frame to a texture...
        if (UIFrame._drawingStandin) {
            renderer.setOrtho();

            // hold onto our old translation
            final ReadOnlyVector3 lTrans = getTranslation();
            final double x = lTrans.getX(), y = lTrans.getY(), z = lTrans.getZ();

            // set our new translation so that we are drawn in the bottom left corner of the texture.
            double newX = 0, newY = 0;
            if (width > dispWidth && x < 0) {
                newX = x;
            }
            if (height > dispHeight && y < 0) {
                newY = y;
            }
            setTranslation(newX, newY, 0);
            updateWorldTransform(true);

            // draw to texture
            super.draw(renderer);

            // replace our old translation
            setTranslation(x, y, z);
            updateWorldTransform(true);

            renderer.unsetOrtho();

            // exit
            return;
        }

        // Calculate our standin's translation (and size) so that we are drawn in the bottom left corner of the texture.
        // Take into account frames that are bigger than the screen.
        int newWidth = width, newHeight = height;
        int x = getHudX();
        int y = getHudY();
        if (width > dispWidth && x < 0) {
            newWidth += getHudX();
            x = 0;
        }
        if (height > dispHeight && y < 0) {
            newHeight += getHudY();
            y = 0;
        }

        // Otherwise we are not rendering to texture and we are using standins...
        // So check if we are dirty.
        if (isDirty()) {
            renderer.unsetOrtho();
            // Check if we have a standin yet
            if (_standin == null) {
                try {
                    buildStandin(renderer);
                } catch (final Exception e) {
                    UIFrame._logger.warning("Unable to create standin: " + e.getMessage());
                    UIFrame._logger.logp(Level.SEVERE, getClass().getName(), "draw(Renderer)", "Exception", e);
                }
            }

            // Check if we have a texture renderer yet before going further
            if (UIFrame._textureRenderer != null) {
                UIFrame._drawingStandin = true;
                // Save aside our opacity
                final float op = _frameOpacity;
                // Set our opacity to 1.0 for the cached texture
                setFrameOpacity(1.0f);
                // render the frame to a texture
                UIFrame._textureRenderer.render(this, _fakeTexture, true);
                // return our old transparency
                setFrameOpacity(op);
                UIFrame._drawingStandin = false;

                // Prepare the texture coordinates for our frame.
                float dW = newWidth / (float) UIFrame._textureRenderer.getWidth();
                if (dW > 1) {
                    dW = 1;
                }
                float dH = newHeight / (float) UIFrame._textureRenderer.getHeight();
                if (dH > 1) {
                    dH = 1;
                }
                final FloatBuffer tbuf = _standin.getMeshData().getTextureBuffer(0);
                tbuf.clear();
                tbuf.put(0).put(dH);
                tbuf.put(0).put(0);
                tbuf.put(dW).put(0);
                tbuf.put(dW).put(dH);
                tbuf.rewind();

                _dirty = false;
            }
            renderer.setOrtho();
            renderer.clearClips();
        }

        // Now, render the standin quad.
        if (_standin != null) {
            // See if we need to change the dimensions of our standin quad.

            if (newWidth != _standin.getWidth() || newHeight != _standin.getHeight()) {
                _standin.resize(newWidth, newHeight);
            }

            // Prepare our default color with the correct alpha value for opacity.
            final ColorRGBA color = ColorRGBA.fetchTempInstance();
            color.set(1, 1, 1, getFrameOpacity());
            _standin.setDefaultColor(color);
            ColorRGBA.releaseTempInstance(color);

            // Position standin quad properly
            _standin.setWorldTranslation(x, y, getWorldTranslation().getZ());

            // draw our standin quad with cached frame texture.
            _standin.draw(renderer);
        }
    }

    /**
     * Build our standin quad and (as necessary) a texture renderer.
     * 
     * @param renderer
     *            the renderer to use if we need to generate a texture renderer
     */
    private void buildStandin(final Renderer renderer) {
        _standin = new UIQuad("frame_standin", 1, 1);
        // no frustum culling checks
        _standin.getSceneHints().setCullHint(CullHint.Never);
        // no lighting
        _standin.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        // a single texture
        _standin.getSceneHints().setTextureCombineMode(TextureCombineMode.Replace);
        // immediate mode
        _standin.getSceneHints().setRenderBucketType(RenderBucketType.Skip);

        // Add an alpha blend state
        final BlendState blend = new BlendState();
        blend.setBlendEnabled(true);
        blend.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
        blend.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
        // throw out fragments with alpha of 0.
        blend.setTestFunction(BlendState.TestFunction.GreaterThan);
        blend.setReference(0.0f);
        blend.setTestEnabled(true);
        _standin.setRenderState(blend);

        // Check for and create a texture renderer if none exists yet.
        if (UIFrame._textureRenderer == null) {
            final Camera cam = Camera.getCurrentCamera();
            UIFrame._textureRenderer = TextureRendererFactory.INSTANCE.createTextureRenderer(cam.getWidth(), cam
                    .getHeight(), renderer, ContextManager.getCurrentContext().getCapabilities());
            UIFrame._textureRenderer.setBackgroundColor(new ColorRGBA(0f, 0f, 1f, 0f));
            UIFrame._textureRenderer.setMultipleTargets(true);
        }

        // create a texture to cache the contents to
        _fakeTexture = new Texture2D();
        _fakeTexture.setMagnificationFilter(MagnificationFilter.Bilinear);
        _fakeTexture.setMinificationFilter(MinificationFilter.NearestNeighborNoMipMaps);
        _fakeTexture.setRenderToTextureFormat(Format.RGBA8);
        _fakeTexture.setWrap(WrapMode.EdgeClamp);
        UIFrame._textureRenderer.setupTexture(_fakeTexture);

        // Set a texturestate on the standin, using the fake texture
        final TextureState ts = new TextureState();
        ts.setTexture(_fakeTexture);
        _standin.setRenderState(ts);

        // Update the standin, getting states, etc. all set.
        _standin.updateGeometricState(0);
    }

    /**
     * @return the opacity level set on this frame in [0,1], where 0 means completely transparent and 1 is completely
     *         opaque. If useTransparency is false, this will always return 1.
     */
    public float getFrameOpacity() {
        if (UIFrame._useTransparency) {
            return _frameOpacity;
        } else {
            return 1.0f;
        }
    }

    /**
     * Set the opacity level of this frame.
     * 
     * @param frameOpacity
     *            value in [0,1], where 0 means completely transparent and 1 is completely opaque.
     */
    public void setFrameOpacity(final float frameOpacity) {
        _frameOpacity = frameOpacity;
    }

    @Override
    public void attachedToHud() {
        super.attachedToHud();
        // add our drag listener to the hud
        getHud().addDragListener(_dragListener);
    }

    @Override
    public void detachedFromHud() {
        super.detachedFromHud();

        // Remove our drag listener from the hud
        if (getHud() != null) {
            getHud().removeDragListener(_dragListener);
        }

        // clean up visuals created for this frame
        clearStandin();
    }

    @Override
    protected void predrawComponent(final Renderer renderer) {
        // Set the current frame opacity
        UIFrame._currentOpacity = getFrameOpacity();
        super.predrawComponent(renderer);
    }

    /**
     * @return true if our parent is a UIHud.
     */
    public boolean isAttachedToHUD() {
        return getParent() instanceof UIHud;
    }

    /**
     * Resize the frame to fit the minimum size of its content panel.
     */
    public void pack() {
        _contentPanel.updateMinimumSizeFromContents();
        pack(_contentPanel.getMinimumComponentWidth(true), _contentPanel.getMinimumComponentHeight(true));
    }

    /**
     * Resize the frame to fit its content area to the given dimensions
     */
    public void pack(final int contentWidth, final int contentHeight) {
        int width = contentWidth + _basePanel.getTotalLeft() + _basePanel.getTotalRight();
        int height = contentHeight + _basePanel.getTotalTop() + _basePanel.getTotalBottom();
        if (isDecorated()) {
            height += _statusBar.getComponentHeight() + _titleBar.getComponentHeight();
        }
        setComponentSize(Math.max(width, UIFrame.MIN_FRAME_WIDTH), Math.max(height, UIFrame.MIN_FRAME_HEIGHT));
        layout();
        width = contentWidth + _basePanel.getTotalLeft() + _basePanel.getTotalRight();
        height = contentHeight + _basePanel.getTotalTop() + _basePanel.getTotalBottom();
        if (isDecorated()) {
            height += _statusBar.getComponentHeight() + _titleBar.getComponentHeight();
        }
        setComponentSize(Math.max(width, UIFrame.MIN_FRAME_WIDTH), Math.max(height, UIFrame.MIN_FRAME_HEIGHT));
    }

    /**
     * @return true if this frame has had recent content changes that would require a repaint.
     */
    public boolean isDirty() {
        return _dirty;
    }

    /**
     * @param dirty
     *            true if this frame has had recent content changes that would require a repaint.
     */
    void setDirty(final boolean dirty) {
        _dirty = dirty;
    }

    /**
     * Set ourselves dirty.
     */
    @Override
    public void fireComponentDirty() {
        setDirty(true);
    }

    /**
     * @return true if all frames should use their opacity value to blend against the 3d scene (and each other)
     */
    public static boolean isUseTransparency() {
        return UIFrame._useTransparency;
    }

    /**
     * @param useTransparency
     *            true if all frames should use their opacity value to blend against the 3d scene (and each other)
     */
    public static void setUseTransparency(final boolean useTransparency) {
        UIFrame._useTransparency = useTransparency;
    }

    /**
     * Causes our shared texture renderer - used to draw cached versions of all frames - to be recreated on the next
     * render loop.
     */
    public static void resetTextureRenderer() {
        final Callable<Void> exe = new Callable<Void>() {
            public Void call() {
                if (UIFrame._textureRenderer != null) {
                    UIFrame._textureRenderer.cleanup();
                }
                UIFrame._textureRenderer = null;
                return null;
            }
        };
        GameTaskQueueManager.getManager().render(exe);
    }

    /**
     * Release our standin and cached texture for gc. If needed again, they will be created from scratch.
     */
    public void clearStandin() {
        _fakeTexture = null;
        _standin = null;
    }

    /**
     * @return the currently rendering frame's opacity level. Used by subcomponents to alter their alpha values based on
     *         the opacity of the frame they are rendering within.
     */
    public static float getCurrentOpacity() {
        return UIFrame._currentOpacity;
    }

    /**
     * @return true if we are currently rendering a frame to texture.
     */
    public static boolean isDrawingStandin() {
        return UIFrame._drawingStandin;
    }

    /**
     * The drag listener responsible for allowing a frame to be moved around the screen with the mouse.
     */
    private final class FrameDragListener implements DragListener {
        int oldX = 0;
        int oldY = 0;

        public void startDrag(final int mouseX, final int mouseY) {
            oldX = mouseX;
            oldY = mouseY;
        }

        public void drag(final int mouseX, final int mouseY) {
            // check if we are off the edge... if so, flag for redraw (part of the frame may have been hidden)
            if (!smallerThanWindow()) {
                fireComponentDirty();
            }

            addTranslation(mouseX - oldX, mouseY - oldY, 0);
            oldX = mouseX;
            oldY = mouseY;

            // check if we are off the edge now... if so, flag for redraw (part of the frame may have been hidden)
            if (!smallerThanWindow()) {
                fireComponentDirty();
            }
        }

        /**
         * @return true if this frame can be fully contained by the display.
         */
        public boolean smallerThanWindow() {
            final int dispWidth = Camera.getCurrentCamera().getWidth();
            final int dispHeight = Camera.getCurrentCamera().getHeight();

            return getComponentWidth() <= dispWidth && getComponentHeight() <= dispHeight;
        }

        public void endDrag(final UIComponent component, final int mouseX, final int mouseY) {}

        public boolean isDragHandle(final UIComponent component, final int mouseX, final int mouseY) {
            return component == _titleBar.getTitleLabel();
        }
    }

    /**
     * Enumeration of possible frame chrome buttons.
     */
    public enum FrameButtons {
        CLOSE, MINIMIZE, MAXIMIZE, HELP;
    }
}
