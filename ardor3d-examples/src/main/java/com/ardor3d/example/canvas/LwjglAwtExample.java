/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.canvas;

import com.ardor3d.example.Exit;
import com.ardor3d.framework.ArdorModule;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.framework.awt.AwtCanvas;
import com.ardor3d.framework.lwjgl.LwjglCanvasRenderer;
import com.ardor3d.image.util.AWTImageLoader;
import com.ardor3d.input.InputState;
import com.ardor3d.input.Key;
import com.ardor3d.input.MouseCursor;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.awt.AwtFocusWrapper;
import com.ardor3d.input.awt.AwtKeyboardWrapper;
import com.ardor3d.input.awt.AwtMouseManager;
import com.ardor3d.input.awt.AwtMouseWrapper;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.TriggerAction;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LwjglAwtExample {
    static MouseCursor _cursor1;
    static MouseCursor _cursor2;

    static Map<Canvas, Boolean> _showCursor1 = new HashMap<Canvas, Boolean>();

    public static void main(final String[] args) throws Exception {

        final Module ardorModule = new ArdorModule();
        // final Module systemModule = new LwjglAwtModule();

        final Injector injector = Guice.createInjector(Stage.PRODUCTION, ardorModule);

        final FrameHandler frameWork = injector.getInstance(FrameHandler.class);

        final MyExit exit = new MyExit();
        final LogicalLayer logicalLayer = injector.getInstance(LogicalLayer.class);

        final ExampleScene scene1 = new ExampleScene();
        final RotatingCubeGame game1 = new RotatingCubeGame(scene1, exit, logicalLayer, Key.T);

        final ExampleScene scene2 = new ExampleScene();
        final RotatingCubeGame game2 = new RotatingCubeGame(scene2, exit, logicalLayer, Key.G);

        frameWork.registerUpdater(game1);
        frameWork.registerUpdater(game2);

        final JFrame frame = new JFrame("AWT Example");
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                exit.exit();
            }
        });

        frame.setLayout(new GridLayout(2, 3));


        AWTImageLoader awtImageLoader = new AWTImageLoader();
        _cursor1 = createMouseCursor(awtImageLoader, "/com/ardor3d/example/media/input/wait_cursor.png");
        _cursor2 = createMouseCursor(awtImageLoader, "/com/ardor3d/example/media/input/movedata.gif");

        addCanvas(frame, scene1, logicalLayer, frameWork);
        frame
                .add(new JLabel(
                        "<html>"
                                + "<table>"
                                + "<tr><th align=\"left\" style=\"font-size: 16\">Action</th><th align=\"left\" style=\"font-size: 16\">Command</th></tr>"
                                + "<tr><td>WS</td><td>Move camera position forward/back</td></tr>"
                                + "<tr><td>AD</td><td>Turn camera left/right</td></tr>"
                                + "<tr><td>QE</td><td>Strafe camera left/right</td></tr>"
                                + "<tr><td>T</td><td>Toggle cube rotation for scene 1 on press</td></tr>"
                                + "<tr><td>G</td><td>Toggle cube rotation for scene 2 on press</td></tr>"
                                + "<tr><td>U</td><td>Toggle both cube rotations on release</td></tr>"
                                + "<tr><td>0 (zero)</td><td>Reset camera position</td></tr>"
                                + "<tr><td>9</td><td>Face camera towards cube without changing position</td></tr>"
                                + "<tr><td>ESC</td><td>Quit</td></tr>"
                                + "<tr><td>Mouse</td><td>Press left button to rotate camera.</td></tr>" + "</table>"
                                + "</html>", SwingConstants.CENTER));
        addCanvas(frame, scene1, logicalLayer, frameWork);
        frame.add(new JLabel("", SwingConstants.CENTER));
        addCanvas(frame, scene2, logicalLayer, frameWork);
        frame.add(new JLabel("", SwingConstants.CENTER));

        frame.pack();
        frame.setVisible(true);

        game1.init();
        game2.init();

        while (!exit.isExit()) {
            frameWork.updateFrame();
        }

        frame.dispose();
        System.exit(0);
    }

    private static MouseCursor createMouseCursor(final AWTImageLoader awtImageLoader, final String resourceName) throws IOException {
        com.ardor3d.image.Image image = awtImageLoader.load(LwjglAwtExample.class.getResourceAsStream(resourceName), true);

        return new MouseCursor("cursor1", image, 0, image.getHeight() - 1);
    }

    private static void addCanvas(final JFrame frame, final ExampleScene scene, final LogicalLayer logicalLayer,
                                  final FrameHandler frameWork) throws Exception {
        final LwjglCanvasRenderer canvasRenderer = new LwjglCanvasRenderer(scene);

        final AwtCanvas theCanvas = new AwtCanvas();

        theCanvas.setCanvasRenderer(canvasRenderer);
        frame.add(theCanvas);

        _showCursor1.put(theCanvas, true);

        theCanvas.setSize(new Dimension(400, 300));
        theCanvas.setVisible(true);

        final AwtMouseWrapper mouseWrapper = new AwtMouseWrapper(theCanvas);
        final AwtKeyboardWrapper keyboardWrapper = new AwtKeyboardWrapper(theCanvas);
        final AwtFocusWrapper focusWrapper = new AwtFocusWrapper(theCanvas);
        final AwtMouseManager mouseManager = new AwtMouseManager(theCanvas);

        final PhysicalLayer pl = new PhysicalLayer(keyboardWrapper, mouseWrapper, focusWrapper);

        logicalLayer.registerInput(theCanvas, pl);

        logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.H),
                new TriggerAction() {
                    public void perform(Canvas source, InputState inputState, double tpf) {
                        if (source != theCanvas) {
                            return;
                        }

                        if (_showCursor1.get(theCanvas)) {
                            mouseManager.setCursor(_cursor1);
                        }
                        else {
                            mouseManager.setCursor(_cursor2);
                        }

                        _showCursor1.put(theCanvas, !_showCursor1.get(theCanvas));
                    }
                }));
        logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.J),
                new TriggerAction() {
                    public void perform(Canvas source, InputState inputState, double tpf) {
                        if (source != theCanvas) {
                            return;
                        }

                        mouseManager.setCursor(MouseCursor.SYSTEM_DEFAULT);
                    }
                }));

        frameWork.registerCanvas(theCanvas);

    }

    private static class MyExit implements Exit {
        private volatile boolean exit = false;

        public void exit() {
            exit = true;
        }

        public boolean isExit() {
            return exit;
        }
    }
}
