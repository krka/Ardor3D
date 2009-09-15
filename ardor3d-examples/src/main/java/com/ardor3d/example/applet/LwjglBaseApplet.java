/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.applet;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.Scene;
import com.ardor3d.framework.lwjgl.LwjglCanvasRenderer;
import com.ardor3d.framework.lwjgl.LwjglDisplayCanvas;
import com.ardor3d.image.util.AWTImageLoader;
import com.ardor3d.input.Key;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.control.FirstPersonControl;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyReleasedCondition;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.input.lwjgl.LwjglKeyboardWrapper;
import com.ardor3d.input.lwjgl.LwjglMouseWrapper;
import com.ardor3d.intersection.PickResults;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.util.ContextGarbageCollector;
import com.ardor3d.util.GameTaskQueue;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.Timer;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;

/**
 * An example base class for ardor3d/lwjgl applets. This is not meant to be a "best-practices" applet, just a rough demo
 * showing possibilities. As such, there are likely bugs, etc. Please report these. :)
 */
public abstract class LwjglBaseApplet extends Applet implements Scene {

    private static final long serialVersionUID = 1L;

    protected LwjglDisplayCanvas _glCanvas;
    protected LogicalLayer _logicalLayer;
    protected PhysicalLayer _physicalLayer;
    protected Canvas _displayCanvas;

    protected Thread _gameThread;
    protected boolean _running = false;

    protected final Timer _timer = new Timer();
    protected final Node _root = new Node();

    @Override
    public void init() {
        setLayout(new BorderLayout(0, 0));
        try {
            _displayCanvas = new Canvas() {
                private static final long serialVersionUID = 1L;

                @Override
                public final void addNotify() {
                    super.addNotify();
                    startLWJGL();
                }

                @Override
                public final void removeNotify() {
                    stopLWJGL();
                    super.removeNotify();
                }
            };
            _displayCanvas.setSize(getWidth(), getHeight());
            add(_displayCanvas, BorderLayout.CENTER);
            _displayCanvas.setFocusable(true);
            _displayCanvas.requestFocus();
            _displayCanvas.setIgnoreRepaint(true);
            _displayCanvas.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(final ComponentEvent e) {
                    GameTaskQueueManager.getManager().update(new Callable<Void>() {
                        public Void call() throws Exception {
                            final Camera cam = _glCanvas.getCanvasRenderer().getCamera();
                            cam.resize(getWidth(), getHeight());
                            cam.setFrustumPerspective(cam.getFovY(), getWidth() / (float) getHeight(), cam
                                    .getFrustumNear(), cam.getFrustumFar());
                            return null;
                        }
                    });
                }
            });
            setVisible(true);
        } catch (final Exception e) {
            System.err.println(e);
            throw new RuntimeException("Unable to create display");
        }
    }

    @Override
    public void stop() {
        stopLWJGL();
    }

    protected void startLWJGL() {
        _gameThread = new Thread() {
            @Override
            public void run() {
                _running = true;
                try {
                    initGL();
                    initInput();
                    initBaseScene();
                    initAppletScene();
                    gameLoop();
                } catch (final LWJGLException ex) {
                    ex.printStackTrace();
                }
            }
        };
        _gameThread.start();
    }

    protected void stopLWJGL() {
        _running = false;
        try {
            _gameThread.join();
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void gameLoop() {
        while (_running) {
            update();
            _glCanvas.draw(null);
            Thread.yield();
        }
    }

    protected void update() {
        _timer.update();

        if (_logicalLayer != null) {
            _logicalLayer.checkTriggers(_timer.getTimePerFrame());
        }

        // Execute updateQueue item
        GameTaskQueueManager.getManager(_glCanvas.getCanvasRenderer().getRenderContext())
                .getQueue(GameTaskQueue.UPDATE).execute();

        updateAppletScene(_timer);

        // Update controllers/render states/transforms/bounds for rootNode.
        _root.updateGeometricState(_timer.getTimePerFrame(), true);
    }

    protected void initGL() throws LWJGLException {
        final LwjglCanvasRenderer canvasRenderer = new LwjglCanvasRenderer(this);
        final DisplaySettings settings = new DisplaySettings(getWidth(), getHeight(), 8, 0);
        _glCanvas = new LwjglDisplayCanvas(_displayCanvas, settings, canvasRenderer);
        _glCanvas.init();
    }

    protected void initInput() {
        _logicalLayer = new LogicalLayer();
        _physicalLayer = new PhysicalLayer(new LwjglKeyboardWrapper(), new LwjglMouseWrapper(), _glCanvas);
        _logicalLayer.registerInput(_glCanvas, _physicalLayer);
        FirstPersonControl.setupTriggers(_logicalLayer, Vector3.UNIT_Y, true);

        _logicalLayer.registerTrigger(new InputTrigger(new KeyReleasedCondition(Key.F), new TriggerAction() {

            public void perform(final com.ardor3d.framework.Canvas source, final TwoInputStates inputState,
                    final double tpf) {
                try {
                    _glCanvas.setFullScreen(!_glCanvas.isFullScreen());
                    final Camera cam = _glCanvas.getCanvasRenderer().getCamera();
                    if (_glCanvas.isFullScreen()) {
                        final DisplayMode mode = Display.getDisplayMode();
                        cam.resize(mode.getWidth(), mode.getHeight());
                        cam.setFrustumPerspective(cam.getFovY(), mode.getWidth() / (float) mode.getHeight(), cam
                                .getFrustumNear(), cam.getFrustumFar());
                    } else {
                        cam.resize(getWidth(), getHeight());
                        cam.setFrustumPerspective(cam.getFovY(), getWidth() / (float) getHeight(),
                                cam.getFrustumNear(), cam.getFrustumFar());
                    }

                    System.err.println("full: " + _glCanvas.isFullScreen());
                } catch (final LWJGLException ex) {
                    ex.printStackTrace();
                }
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyReleasedCondition(Key.V), new TriggerAction() {
            public void perform(final com.ardor3d.framework.Canvas source, final TwoInputStates inputState,
                    final double tpf) {
                _glCanvas.setVSyncEnabled(true);
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyReleasedCondition(Key.B), new TriggerAction() {
            public void perform(final com.ardor3d.framework.Canvas source, final TwoInputStates inputState,
                    final double tpf) {
                _glCanvas.setVSyncEnabled(false);
            }
        }));
    }

    protected void initBaseScene() {
        // Add our awt based image loader.
        AWTImageLoader.registerLoader();

        // Set the location of our example resources.
        try {
            final SimpleResourceLocator srl = new SimpleResourceLocator(ExampleBase.class.getClassLoader().getResource(
                    "com/ardor3d/example/media/"));
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, srl);
        } catch (final URISyntaxException ex) {
            ex.printStackTrace();
        }

        // Create a ZBuffer to display pixels closest to the camera above farther ones.
        final ZBufferState buf = new ZBufferState();
        buf.setEnabled(true);
        buf.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
        _root.setRenderState(buf);
    }

    public PickResults doPick(final Ray3 pickRay) {
        // ignore
        return null;
    }

    public boolean renderUnto(final Renderer renderer) {
        // Execute renderQueue item
        GameTaskQueueManager.getManager(_glCanvas.getCanvasRenderer().getRenderContext())
                .getQueue(GameTaskQueue.RENDER).execute(renderer);

        // Clean up card garbage such as textures, vbos, etc.
        ContextGarbageCollector.doRuntimeCleanup(renderer);

        renderScene(renderer);

        return true;
    }

    protected abstract void initAppletScene();

    protected void updateAppletScene(final Timer timer) {};

    protected void renderScene(final Renderer renderer) {
        // Draw the root and all its children.
        renderer.draw(_root);
    }
}
