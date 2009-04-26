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

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import com.ardor3d.example.Exit;
import com.ardor3d.framework.ArdorModule;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.framework.lwjgl.LwjglCanvasRenderer;
import com.ardor3d.framework.swt.SwtCanvas;
import com.ardor3d.input.Key;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.SwtFocusWrapper;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.swt.SwtKeyboardWrapper;
import com.ardor3d.input.swt.SwtMouseWrapper;
import com.ardor3d.renderer.Camera;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;

public class LwjglSwtExample {
    private static final Logger logger = Logger.getLogger(LwjglSwtExample.class.toString());
    private static int i = 0;

    public static void main(final String[] args) {

        final Module ardorModule = new ArdorModule();

        final Injector injector = Guice.createInjector(Stage.PRODUCTION, ardorModule);

        final FrameHandler frameWork = injector.getInstance(FrameHandler.class);

        final MyExit exit = new MyExit();
        final ExampleScene scene = new ExampleScene();
        final RotatingCubeGame game = new RotatingCubeGame(scene, exit, injector.getInstance(LogicalLayer.class), Key.T);

        frameWork.addUpdater(game);

        // INIT SWT STUFF
        final Display display = new Display();
        final Shell shell = new Shell(display);
        shell.setLayout(new FillLayout());

        // This is our tab folder, it will be accepting our 3d canvases
        final TabFolder tabFolder = new TabFolder(shell, SWT.BORDER);

        // Add a menu item that will create and add a new canvas.
        final Menu bar = new Menu(shell, SWT.BAR);
        shell.setMenuBar(bar);

        final MenuItem fileItem = new MenuItem(bar, SWT.CASCADE);
        fileItem.setText("&Tasks");

        final Menu submenu = new Menu(shell, SWT.DROP_DOWN);
        fileItem.setMenu(submenu);
        final MenuItem item = new MenuItem(submenu, SWT.PUSH);
        item.addListener(SWT.Selection, new Listener() {
            public void handleEvent(final Event e) {
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        addNewCanvas(tabFolder, scene, injector);
                    }
                });
            }
        });
        item.setText("Add &3d Canvas");
        item.setAccelerator(SWT.MOD1 + '3');

        addNewCanvas(tabFolder, scene, injector);

        shell.open();

        game.init();
        // frameWork.init();

        while (!shell.isDisposed() && !exit.isExit()) {
            display.readAndDispatch();
            frameWork.updateFrame();

            // using the below way makes things really jerky. Not sure how to handle that.

            // if (display.readAndDispatch()) {
            // frameWork.updateFrame();
            // }
            // else {
            // display.sleep();
            // }
        }

        display.dispose();
        System.exit(0);
    }

    private static void addNewCanvas(final TabFolder tabFolder, final ExampleScene scene, final Injector injector) {
        i++;
        logger.info("Adding canvas");

        final FrameHandler frameWork = injector.getInstance(FrameHandler.class);

        // Add a new tab to hold our canvas
        final TabItem item = new TabItem(tabFolder, SWT.NONE);
        item.setText("Canvas #" + i);
        tabFolder.setSelection(item);
        final Composite canvasParent = new Composite(tabFolder, SWT.NONE);
        canvasParent.setLayout(new FillLayout());
        item.setControl(canvasParent);

        final GLData data = new GLData();
        data.depthSize = 8;
        data.doubleBuffer = true;

        final SashForm splitter = new SashForm(canvasParent, SWT.HORIZONTAL);

        final SashForm splitterLeft = new SashForm(splitter, SWT.VERTICAL);
        final Composite topLeft = new Composite(splitterLeft, SWT.NONE);
        topLeft.setLayout(new FillLayout());
        final Composite bottomLeft = new Composite(splitterLeft, SWT.NONE);
        bottomLeft.setLayout(new FillLayout());

        final SashForm splitterRight = new SashForm(splitter, SWT.VERTICAL);
        final Composite topRight = new Composite(splitterRight, SWT.NONE);
        topRight.setLayout(new FillLayout());
        final Composite bottomRight = new Composite(splitterRight, SWT.NONE);
        bottomRight.setLayout(new FillLayout());

        canvasParent.layout();

        final SwtCanvas canvas1 = new SwtCanvas(topLeft, SWT.NONE, data);
        final LwjglCanvasRenderer lwjglCanvasRenderer1 = new LwjglCanvasRenderer(scene);
        canvas1.setCanvasRenderer(lwjglCanvasRenderer1);
        frameWork.addCanvas(canvas1);
        canvas1.addControlListener(newResizeHandler(canvas1, lwjglCanvasRenderer1));
        canvas1.setFocus();

        final SwtCanvas canvas2 = new SwtCanvas(bottomLeft, SWT.NONE, data);
        final LwjglCanvasRenderer lwjglCanvasRenderer2 = new LwjglCanvasRenderer(scene);
        canvas2.setCanvasRenderer(lwjglCanvasRenderer2);
        frameWork.addCanvas(canvas2);
        canvas2.addControlListener(newResizeHandler(canvas2, lwjglCanvasRenderer2));

        final SwtCanvas canvas3 = new SwtCanvas(topRight, SWT.NONE, data);
        final LwjglCanvasRenderer lwjglCanvasRenderer3 = new LwjglCanvasRenderer(scene);
        canvas3.setCanvasRenderer(lwjglCanvasRenderer3);
        frameWork.addCanvas(canvas3);
        canvas3.addControlListener(newResizeHandler(canvas3, lwjglCanvasRenderer3));

        final SwtCanvas canvas4 = new SwtCanvas(bottomRight, SWT.NONE, data);
        final LwjglCanvasRenderer lwjglCanvasRenderer4 = new LwjglCanvasRenderer(scene);
        canvas4.setCanvasRenderer(lwjglCanvasRenderer4);
        frameWork.addCanvas(canvas4);
        canvas4.addControlListener(newResizeHandler(canvas4, lwjglCanvasRenderer4));

        final LogicalLayer logicalLayer = injector.getInstance(LogicalLayer.class);

        final SwtKeyboardWrapper keyboardWrapper = new SwtKeyboardWrapper(canvas1);
        final SwtMouseWrapper mouseWrapper = new SwtMouseWrapper(canvas1);
        final SwtFocusWrapper focusWrapper = new SwtFocusWrapper(canvas1);

        final PhysicalLayer pl = new PhysicalLayer(keyboardWrapper, mouseWrapper, focusWrapper);

        // pl.init();

        // canvas.addKeyListener(keyboardWrapper);
        // mouseWrapper.listenTo(canvas);

        logicalLayer.registerInput(canvas1, pl);
    }

    static ControlListener newResizeHandler(final SwtCanvas swtCanvas, final CanvasRenderer canvasRenderer) {
        final ControlListener retVal = new ControlListener() {
            public void controlMoved(final ControlEvent e) {}

            public void controlResized(final ControlEvent event) {
                final Rectangle size = swtCanvas.getClientArea();
                if ((size.width == 0) && (size.height == 0)) {
                    return;
                }
                final float aspect = (float) size.width / (float) size.height;
                final Camera camera = canvasRenderer.getCamera();
                if (camera != null) {
                    final float fovY = 45; // XXX no camera.getFov()
                    final double near = camera.getFrustumNear();
                    final double far = camera.getFrustumFar();
                    camera.setFrustumPerspective(fovY, aspect, near, far);
                    camera.resize(size.width, size.height);
                }
            }
        };
        return retVal;
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

// class LwjglSwtModule extends AbstractModule {
// public LwjglSwtModule() {}
//
// @Override
// protected void configure() {
// // enforce a single instance of SwtKeyboardWrapper will handle both the KeyListener and the KeyboardWrapper
// // interfaces
// bind(SwtKeyboardWrapper.class).in(Scopes.SINGLETON);
// bind(KeyboardWrapper.class).to(SwtKeyboardWrapper.class);
// bind(KeyListener.class).to(SwtKeyboardWrapper.class);
//
// bind(MouseWrapper.class).to(SwtMouseWrapper.class);
// }
// }