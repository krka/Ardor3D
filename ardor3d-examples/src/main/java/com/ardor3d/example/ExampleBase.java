/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example;

import java.awt.EventQueue;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.ArdorModule;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.framework.NativeCanvas;
import com.ardor3d.framework.Scene;
import com.ardor3d.framework.Updater;
import com.ardor3d.framework.jogl.JoglCanvasRenderer;
import com.ardor3d.image.util.AWTImageLoader;
import com.ardor3d.image.util.ScreenShotImageExporter;
import com.ardor3d.input.FocusWrapper;
import com.ardor3d.input.GrabbedState;
import com.ardor3d.input.Key;
import com.ardor3d.input.KeyboardWrapper;
import com.ardor3d.input.MouseButton;
import com.ardor3d.input.MouseManager;
import com.ardor3d.input.MouseWrapper;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.control.FirstPersonControl;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.MouseButtonClickedCondition;
import com.ardor3d.input.logical.MouseButtonPressedCondition;
import com.ardor3d.input.logical.MouseButtonReleasedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.input.logical.AnyKeyCondition;
import com.ardor3d.intersection.PickData;
import com.ardor3d.intersection.PickResults;
import com.ardor3d.intersection.PickingUtil;
import com.ardor3d.intersection.PrimitivePickResults;
import com.ardor3d.light.PointLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.TextureRendererFactory;
import com.ardor3d.renderer.jogl.JoglTextureRendererProvider;
import com.ardor3d.renderer.lwjgl.LwjglTextureRendererProvider;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.renderer.state.WireframeState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.event.DirtyType;
import com.ardor3d.util.Constants;
import com.ardor3d.util.ContextGarbageCollector;
import com.ardor3d.util.GameTaskQueue;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.geom.Debugger;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;
import com.ardor3d.util.screen.ScreenExporter;
import com.ardor3d.util.stat.StatCollector;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.google.inject.Stage;

public abstract class ExampleBase extends Thread implements Updater, Scene, Exit {
    private static final Logger logger = Logger.getLogger(ExampleBase.class.getName());

    protected final LogicalLayer _logicalLayer;

    protected final Node _root = new Node();

    protected final FrameHandler _frameHandler;

    protected LightState _lightState;

    protected WireframeState _wireframeState;

    protected static boolean exit = false;

    protected static boolean _stereo = false;

    protected boolean _showBounds = false;

    protected boolean _showNormals = false;

    protected boolean _doShot = false;

    protected NativeCanvas _canvas;

    protected ScreenShotImageExporter _screenShotExp = new ScreenShotImageExporter();

    protected MouseManager _mouseManager;

    protected FirstPersonControl _controlHandle;

    protected Vector3 _worldUp = new Vector3(0, 1, 0);

    protected static int _minDepthBits = -1;
    protected static int _minAlphaBits = -1;
    protected static int _minStencilBits = -1;

    @Inject
    public ExampleBase(final LogicalLayer logicalLayer, final FrameHandler frameHandler) {
        _logicalLayer = logicalLayer;
        _frameHandler = frameHandler;
    }

    @Override
    public void run() {
        try {
            _frameHandler.init();

            while (!exit) {
                _frameHandler.updateFrame();
                Thread.yield();
            }
            // grab the graphics context so cleanup will work out.
            _canvas.getCanvasRenderer().setCurrentContext();
            quit(_canvas.getCanvasRenderer().getRenderer());
        } catch (final Throwable t) {
            System.err.println("Throwable caught in MainThread - exiting");
            t.printStackTrace(System.err);
        }
    }

    public void exit() {
        exit = true;
    }

    @MainThread
    public void init() {

        registerInputTriggers();

        AWTImageLoader.registerLoader();

        try {
            SimpleResourceLocator srl = new SimpleResourceLocator(ExampleBase.class.getClassLoader().getResource(
                    "com/ardor3d/example/media/"));
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, srl);
            srl = new SimpleResourceLocator(ExampleBase.class.getClassLoader().getResource(
                    "com/ardor3d/example/media/models/"));
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_MODEL, srl);
        } catch (final URISyntaxException ex) {
            ex.printStackTrace();
        }

        /**
         * Create a ZBuffer to display pixels closest to the camera above farther ones.
         */
        final ZBufferState buf = new ZBufferState();
        buf.setEnabled(true);
        buf.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
        _root.setRenderState(buf);

        // ---- LIGHTS
        /** Set up a basic, default light. */
        final PointLight light = new PointLight();
        light.setDiffuse(new ColorRGBA(0.75f, 0.75f, 0.75f, 0.75f));
        light.setAmbient(new ColorRGBA(0.5f, 0.5f, 0.5f, 1.0f));
        light.setLocation(new Vector3(100, 100, 100));
        light.setEnabled(true);

        /** Attach the light to a lightState and the lightState to rootNode. */
        _lightState = new LightState();
        _lightState.setEnabled(true);
        _lightState.attach(light);
        _root.setRenderState(_lightState);

        _wireframeState = new WireframeState();
        _wireframeState.setEnabled(false);
        _root.setRenderState(_wireframeState);

        _root.getSceneHints().setRenderBucketType(RenderBucketType.Opaque);

        initExample();
    }

    protected abstract void initExample();

    @MainThread
    public void update(final ReadOnlyTimer timer) {
        if (_canvas.isClosing()) {
            exit();
        }

        /** update stats, if enabled. */
        if (Constants.stats) {
            StatCollector.update();
        }

        updateLogicalLayer(timer);

        // Execute updateQueue item
        GameTaskQueueManager.getManager(_canvas.getCanvasRenderer().getRenderContext()).getQueue(GameTaskQueue.UPDATE)
                .execute();

        /** Call simpleUpdate in any derived classes of SimpleGame. */
        updateExample(timer);

        /** Update controllers/render states/transforms/bounds for rootNode. */
        _root.updateGeometricState(timer.getTimePerFrame(), true);
    }

    protected void updateLogicalLayer(final ReadOnlyTimer timer) {
        // check and execute any input triggers, if we are concerned with input
        if (_logicalLayer != null) {
            _logicalLayer.checkTriggers(timer.getTimePerFrame());
        }
    }

    protected void updateExample(final ReadOnlyTimer timer) {
    // does nothing
    }

    @MainThread
    public boolean renderUnto(final Renderer renderer) {
        // Execute renderQueue item
        GameTaskQueueManager.getManager(_canvas.getCanvasRenderer().getRenderContext()).getQueue(GameTaskQueue.RENDER)
                .execute(renderer);

        // Clean up card garbage such as textures, vbos, etc.
        ContextGarbageCollector.doRuntimeCleanup(renderer);

        /** Draw the rootNode and all its children. */
        if (!_canvas.isClosing()) {
            /** Call renderExample in any derived classes. */
            renderExample(renderer);
            renderDebug(renderer);

            if (_doShot) {
                ScreenExporter.exportCurrentScreen(_canvas.getCanvasRenderer().getRenderer(), _screenShotExp);
                _doShot = false;
            }
            return true;
        } else {
            return false;
        }
    }

    protected void renderExample(final Renderer renderer) {
        renderer.draw(_root);
    }

    protected void renderDebug(final Renderer renderer) {
        if (_showBounds) {
            Debugger.drawBounds(_root, renderer, true);
        }

        if (_showNormals) {
            Debugger.drawNormals(_root, renderer);
            Debugger.drawTangents(_root, renderer);
        }

    }

    public PickResults doPick(final Ray3 pickRay) {
        final PrimitivePickResults pickResults = new PrimitivePickResults();
        pickResults.setCheckDistance(true);
        PickingUtil.findPick(_root, pickRay, pickResults);
        processPicks(pickResults);
        return pickResults;
    }

    protected void processPicks(final PrimitivePickResults pickResults) {
        int i = 0;
        while (pickResults.getNumber() > 0
                && pickResults.getPickData(i).getIntersectionRecord().getNumberOfIntersection() == 0
                && ++i < pickResults.getNumber()) {
        }
        if (pickResults.getNumber() > i) {
            final PickData pick = pickResults.getPickData(i);
            System.err.println("picked: " + pick.getTargetMesh() + " at: "
                    + pick.getIntersectionRecord().getIntersectionPoint(0));
        } else {
            System.err.println("picked: nothing");
        }
    }

    protected void quit(final Renderer renderer) {
        ContextGarbageCollector.doFinalCleanup(renderer);
        _canvas.close();
    }

    public static void start(final Class<? extends ExampleBase> exampleClazz) {
        // Ask for properties
        final PropertiesGameSettings prefs = getAttributes(new PropertiesGameSettings("ardorSettings.properties", null));

        // Convert to DisplayProperties (XXX: maybe merge these classes?)
        final DisplaySettings settings = new DisplaySettings(prefs.getWidth(), prefs.getHeight(), prefs.getDepth(),
                prefs.getFrequency(),
                // alpha
                _minAlphaBits != -1 ? _minAlphaBits : prefs.getAlphaBits(),
                // depth
                _minDepthBits != -1 ? _minDepthBits : prefs.getDepthBits(),
                // stencil
                _minStencilBits != -1 ? _minStencilBits : prefs.getStencilBits(),
                // samples
                prefs.getSamples(),
                // other
                prefs.isFullscreen(), _stereo);

        // get our framework
        final ArdorModule ardorModule = new ArdorModule();
        Module systemModule = null;

        if ("LWJGL".equalsIgnoreCase(prefs.getRenderer())) {
            systemModule = new LwjglModule();
            TextureRendererFactory.INSTANCE.setProvider(new LwjglTextureRendererProvider());
        } else if ("JOGL".equalsIgnoreCase(prefs.getRenderer())) {
            systemModule = new JoglModule();
            // We are in single screen mode.
            JoglCanvasRenderer.MULTI_CANVAS_MODE = false;
            TextureRendererFactory.INSTANCE.setProvider(new JoglTextureRendererProvider());
        }
        final Module exampleModule = new AbstractModule() {
            @Override
            protected void configure() {
                bind(ExampleBase.class).to(exampleClazz).in(Scopes.SINGLETON);
                bind(Scene.class).to(ExampleBase.class);
                bind(Updater.class).to(ExampleBase.class);
                bind(Exit.class).to(ExampleBase.class);
            }
        };
        final Provider<DisplaySettings> settingsProvider = new Provider<DisplaySettings>() {
            public DisplaySettings get() {
                return settings;
            }
        };

        // Setup our injector.
        final Injector injector = Guice.createInjector(Stage.PRODUCTION, ardorModule, systemModule, exampleModule,
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(DisplaySettings.class).toProvider(settingsProvider);
                    }
                });

        final LogicalLayer ll = injector.getInstance(LogicalLayer.class);
        final FrameHandler frameWork = injector.getInstance(FrameHandler.class);
        final ExampleBase gameThread = injector.getInstance(ExampleBase.class);
        final NativeCanvas canvas = injector.getInstance(NativeCanvas.class);
        final Updater updater = injector.getInstance(Updater.class);
        final PhysicalLayer physicalLayer = new PhysicalLayer(injector.getInstance(KeyboardWrapper.class), injector
                .getInstance(MouseWrapper.class), injector.getInstance(FocusWrapper.class));

        // set the mouse manager member. It's a bit of a hack to do that this way.
        gameThread._mouseManager = injector.getInstance(MouseManager.class);

        ll.registerInput(canvas, physicalLayer);

        // Register our example as an updater.
        frameWork.addUpdater(updater);

        // Make a native canvas and register it.
        frameWork.addCanvas(canvas);

        gameThread._canvas = canvas;

        gameThread.start();
    }

    protected static PropertiesGameSettings getAttributes(final PropertiesGameSettings settings) {
        // Always show the dialog in these examples.
        URL dialogImage = null;
        final String dflt = settings.getDefaultSettingsWidgetImage();
        if (dflt != null) {
            try {
                dialogImage = ExampleBase.class.getResource(dflt);
            } catch (final Exception e) {
                logger.log(Level.SEVERE, "Resource lookup of '" + dflt + "' failed.  Proceeding.");
            }
        }
        if (dialogImage == null) {
            logger.fine("No dialog image loaded");
        } else {
            logger.fine("Using dialog image '" + dialogImage + "'");
        }

        final URL dialogImageRef = dialogImage;
        final AtomicReference<PropertiesDialog> dialogRef = new AtomicReference<PropertiesDialog>();
        final Stack<Runnable> mainThreadTasks = new Stack<Runnable>();
        try {
            if (EventQueue.isDispatchThread()) {
                dialogRef.set(new PropertiesDialog(settings, dialogImageRef, mainThreadTasks));
            } else {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        dialogRef.set(new PropertiesDialog(settings, dialogImageRef, mainThreadTasks));
                    }
                });
            }
        } catch (final Exception e) {
            logger.logp(Level.SEVERE, ExampleBase.class.getClass().toString(), "ExampleBase.getAttributes(settings)",
                    "Exception", e);
            return null;
        }

        PropertiesDialog dialogCheck = dialogRef.get();
        while (dialogCheck == null || dialogCheck.isVisible()) {
            try {
                // check worker queue for work
                while (!mainThreadTasks.isEmpty()) {
                    mainThreadTasks.pop().run();
                }
                // go back to sleep for a while
                Thread.sleep(50);
            } catch (final InterruptedException e) {
                logger.warning("Error waiting for dialog system, using defaults.");
            }

            dialogCheck = dialogRef.get();
        }

        if (dialogCheck.isCancelled()) {
            System.exit(0);
        }
        return settings;
    }

    protected void registerInputTriggers() {

        // check if this example worries about input at all
        if (_logicalLayer == null) {
            return;
        }

        _controlHandle = FirstPersonControl.setupTriggers(_logicalLayer, _worldUp, true);

        _logicalLayer.registerTrigger(new InputTrigger(new MouseButtonClickedCondition(MouseButton.RIGHT),
                new TriggerAction() {
                    public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {

                        final Vector2 pos = Vector2.fetchTempInstance().set(
                                inputStates.getCurrent().getMouseState().getX(),
                                inputStates.getCurrent().getMouseState().getY());
                        final Ray3 pickRay = new Ray3();
                        _canvas.getCanvasRenderer().getCamera().getPickRay(pos, false, pickRay);
                        Vector2.releaseTempInstance(pos);
                        doPick(pickRay);
                    }
                }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ESCAPE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                exit();
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.L), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                _lightState.setEnabled(!_lightState.isEnabled());
                // Either an update or a markDirty is needed here since we did not touch the affected spatial directly.
                _root.markDirty(DirtyType.RenderState);
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.T), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                _wireframeState.setEnabled(!_wireframeState.isEnabled());
                // Either an update or a markDirty is needed here since we did not touch the affected spatial directly.
                _root.markDirty(DirtyType.RenderState);
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.B), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                _showBounds = !_showBounds;
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.N), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                _showNormals = !_showNormals;
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.F1), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                _doShot = true;
            }
        }));

        final Predicate<TwoInputStates> clickLeftOrRight = Predicates.or(new MouseButtonClickedCondition(
                MouseButton.LEFT), new MouseButtonClickedCondition(MouseButton.RIGHT));

        _logicalLayer.registerTrigger(new InputTrigger(clickLeftOrRight, new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                System.err.println("clicked: " + inputStates.getCurrent().getMouseState().getClickCounts());
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new MouseButtonPressedCondition(MouseButton.LEFT),
                new TriggerAction() {
                    public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                        if (_mouseManager.isSetGrabbedSupported()) {
                            _mouseManager.setGrabbed(GrabbedState.GRABBED);
                        }
                    }
                }));
        _logicalLayer.registerTrigger(new InputTrigger(new MouseButtonReleasedCondition(MouseButton.LEFT),
                new TriggerAction() {
                    public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                        if (_mouseManager.isSetGrabbedSupported()) {
                            _mouseManager.setGrabbed(GrabbedState.NOT_GRABBED);
                        }
                    }
                }));

        _logicalLayer.registerTrigger(new InputTrigger(new AnyKeyCondition(), new TriggerAction() {
            public void perform(Canvas source, TwoInputStates inputState, double tpf) {
                System.out.println("Key character pressed: " + inputState.getCurrent().getKeyboardState().getKeyEvent().getKeyChar());
            }
        }));

    }
}
