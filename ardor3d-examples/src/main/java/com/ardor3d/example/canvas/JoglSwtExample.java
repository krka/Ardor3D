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
import com.ardor3d.framework.jogl.JoglCanvasRenderer;
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

public class JoglSwtExample {
    private static final Logger logger = Logger.getLogger(JoglSwtExample.class.toString());
    private static int i = 0;

    public static void main(final String[] args) {

        final Module ardorModule = new ArdorModule();

        final Injector injector = Guice.createInjector(Stage.PRODUCTION, ardorModule);

        final FrameHandler frameWork = injector.getInstance(FrameHandler.class);

        final MyExit exit = new MyExit();
        final ExampleScene scene = new ExampleScene();
        final RotatingCubeGame game = new RotatingCubeGame(scene, exit, injector.getInstance(LogicalLayer.class), Key.T);

        frameWork.registerUpdater(game);

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

        while (!shell.isDisposed() && !exit.isExit()) {
            display.readAndDispatch();
            frameWork.updateFrame();
        }

        display.dispose();
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
        final SashForm splitterRight = new SashForm(splitter, SWT.VERTICAL);
        canvasParent.layout();

        final SwtCanvas canvas1 = new SwtCanvas(splitterLeft, SWT.NONE, data);
        final JoglCanvasRenderer canvasRenderer1 = new JoglCanvasRenderer(scene);
        canvas1.setCanvasRenderer(canvasRenderer1);
        frameWork.registerCanvas(canvas1);
        canvas1.addControlListener(newResizeHandler(canvas1, canvasRenderer1));

        final SwtCanvas canvas2 = new SwtCanvas(splitterLeft, SWT.NONE, data);
        final JoglCanvasRenderer canvasRenderer2 = new JoglCanvasRenderer(scene);
        canvas2.setCanvasRenderer(canvasRenderer2);
        frameWork.registerCanvas(canvas2);
        canvas2.addControlListener(newResizeHandler(canvas2, canvasRenderer2));

        final SwtCanvas canvas3 = new SwtCanvas(splitterRight, SWT.NONE, data);
        final JoglCanvasRenderer canvasRenderer3 = new JoglCanvasRenderer(scene);
        canvas3.setCanvasRenderer(canvasRenderer3);
        frameWork.registerCanvas(canvas3);
        canvas3.addControlListener(newResizeHandler(canvas3, canvasRenderer3));

        final SwtCanvas canvas4 = new SwtCanvas(splitterRight, SWT.NONE, data);
        final JoglCanvasRenderer canvasRenderer4 = new JoglCanvasRenderer(scene);
        canvas4.setCanvasRenderer(canvasRenderer4);
        frameWork.registerCanvas(canvas4);
        canvas4.addControlListener(newResizeHandler(canvas4, canvasRenderer4));

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
