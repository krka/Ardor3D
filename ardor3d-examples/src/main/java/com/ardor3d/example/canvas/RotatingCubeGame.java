/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.canvas;

import java.util.Random;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.example.Exit;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.Updater;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Image.Format;
import com.ardor3d.input.ButtonState;
import com.ardor3d.input.InputState;
import com.ardor3d.input.Key;
import com.ardor3d.input.MouseState;
import com.ardor3d.input.logical.AnyKeyCondition;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyHeldCondition;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.KeyReleasedCondition;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.MouseButtonCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TriggerConditions;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.light.PointLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Camera.ProjectionMode;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.inject.Inject;

public class RotatingCubeGame implements Updater {
    // private final Canvas view;
    private final ExampleScene scene;
    private final Exit exit;
    private final LogicalLayer logicalLayer;
    private final Key toggleRotationKey;

    private final static float CUBE_ROTATE_SPEED = 1;
    private final Vector3 rotationAxis = new Vector3(1, 1, 0);
    private double angle = 0;
    private Mesh box;
    private final Matrix3 rotation = new Matrix3();

    private static final int MOVE_SPEED = 4;
    private static final double TURN_SPEED = 0.5;
    private final Matrix3 _incr = new Matrix3();
    private static final double MOUSE_TURN_SPEED = 1;
    private int rotationSign = 1;

    @Inject
    public RotatingCubeGame(final ExampleScene scene, final Exit exit, final LogicalLayer logicalLayer,
            final Key toggleRotationKey) {
        this.scene = scene;
        this.exit = exit;
        this.logicalLayer = logicalLayer;
        this.toggleRotationKey = toggleRotationKey;
    }

    @MainThread
    public void init() {
        // add a cube to the scene
        // add a rotating controller to the cube
        // add a light
        box = new Box("The cube", new Vector3(-1, -1, -1), new Vector3(1, 1, 1));
        // box = SimpleShapeFactory.createQuad("the 'box'", 1, 1);

        final ZBufferState buf = new ZBufferState();
        buf.setEnabled(true);
        buf.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
        scene.getRoot().setRenderState(buf);

        // Add a texture to the box.
        final TextureState ts = new TextureState();
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear,
                Format.GuessNoCompression, true));
        box.setRenderState(ts);

        final PointLight light = new PointLight();

        final Random random = new Random();

        final float r = random.nextFloat();
        final float g = random.nextFloat();
        final float b = random.nextFloat();
        final float a = random.nextFloat();

        light.setDiffuse(new ColorRGBA(r, g, b, a));
        light.setAmbient(new ColorRGBA(0.5f, 0.5f, 0.5f, 1.0f));
        light.setLocation(new Vector3(MOVE_SPEED, MOVE_SPEED, MOVE_SPEED));
        light.setEnabled(true);

        /** Attach the light to a lightState and the lightState to rootNode. */
        final LightState lightState = new LightState();
        lightState.setEnabled(true);
        lightState.attach(light);
        scene.getRoot().setRenderState(lightState);

        scene.getRoot().attachChild(box);

        registerInputTriggers();
    }

    private void registerInputTriggers() {
        logicalLayer.registerTrigger(new InputTrigger(new KeyHeldCondition(Key.W), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                moveForward(source, tpf);
            }
        }));
        logicalLayer.registerTrigger(new InputTrigger(new KeyHeldCondition(Key.S), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                moveBack(source, tpf);
            }
        }));
        logicalLayer.registerTrigger(new InputTrigger(new KeyHeldCondition(Key.A), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                turnLeft(source, tpf);
            }
        }));
        logicalLayer.registerTrigger(new InputTrigger(new KeyHeldCondition(Key.D), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                turnRight(source, tpf);
            }
        }));
        logicalLayer.registerTrigger(new InputTrigger(new KeyHeldCondition(Key.Q), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                moveLeft(source, tpf);
            }
        }));
        logicalLayer.registerTrigger(new InputTrigger(new KeyHeldCondition(Key.E), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                moveRight(source, tpf);
            }
        }));

        logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ESCAPE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                exit.exit();
            }
        }));

        logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(toggleRotationKey), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                toggleRotation();
            }
        }));
        logicalLayer.registerTrigger(new InputTrigger(new KeyReleasedCondition(Key.U), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                toggleRotation();
            }
        }));

        logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ZERO), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                resetCamera(source);
            }
        }));
        logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.NINE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                lookAtZero(source);
            }
        }));

        final Predicate<TwoInputStates> mouseMovedAndOneButtonPressed = Predicates.and(TriggerConditions.mouseMoved(),
                Predicates.or(TriggerConditions.leftButtonDown(), TriggerConditions.rightButtonDown()));

        logicalLayer.registerTrigger(new InputTrigger(mouseMovedAndOneButtonPressed, new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final MouseState mouseState = inputStates.getCurrent().getMouseState();

                turn(source, mouseState.getDx() * tpf * -MOUSE_TURN_SPEED);
                rotateUpDown(source, mouseState.getDy() * tpf * -MOUSE_TURN_SPEED);
            }
        }));
        logicalLayer.registerTrigger(new InputTrigger(new MouseButtonCondition(ButtonState.DOWN, ButtonState.DOWN,
                ButtonState.UNDEFINED), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                moveForward(source, tpf);
            }
        }));

        logicalLayer.registerTrigger(new InputTrigger(new AnyKeyCondition(), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final InputState current = inputStates.getCurrent();

                System.out.println("Key character pressed: " + current.getKeyboardState().getKeyEvent().getKeyChar());
            }
        }));
    }

    private void lookAtZero(final Canvas source) {
        source.getCanvasRenderer().getCamera().lookAt(Vector3.ZERO, Vector3.UNIT_Y);
    }

    private void resetCamera(final Canvas source) {
        final Vector3 loc = new Vector3(0.0f, 0.0f, 10.0f);
        final Vector3 left = new Vector3(-1.0f, 0.0f, 0.0f);
        final Vector3 up = new Vector3(0.0f, 1.0f, 0.0f);
        final Vector3 dir = new Vector3(0.0f, 0f, -1.0f);

        source.getCanvasRenderer().getCamera().setFrame(loc, left, up, dir);
    }

    private void toggleRotation() {
        rotationSign *= -1;
    }

    @MainThread
    public void update(final ReadOnlyTimer timer) {
        final double tpf = timer.getTimePerFrame();

        logicalLayer.checkTriggers(tpf);

        // rotate away

        angle += tpf * CUBE_ROTATE_SPEED * rotationSign;

        rotation.fromAngleAxis(angle, rotationAxis);
        box.setRotation(rotation);

        box.updateGeometricState(tpf, true);
    }

    private void rotateUpDown(final Canvas canvas, final double speed) {
        final Camera camera = canvas.getCanvasRenderer().getCamera();

        final Vector3 temp = Vector3.fetchTempInstance();
        _incr.fromAngleNormalAxis(speed, camera.getLeft());

        _incr.applyPost(camera.getLeft(), temp);
        camera.setLeft(temp);

        _incr.applyPost(camera.getDirection(), temp);
        camera.setDirection(temp);

        _incr.applyPost(camera.getUp(), temp);
        camera.setUp(temp);

        Vector3.releaseTempInstance(temp);

        camera.normalize();

    }

    private void turnRight(final Canvas canvas, final double tpf) {
        turn(canvas, -TURN_SPEED * tpf);
    }

    private void turn(final Canvas canvas, final double speed) {
        final Camera camera = canvas.getCanvasRenderer().getCamera();

        final Vector3 temp = Vector3.fetchTempInstance();
        _incr.fromAngleNormalAxis(speed, camera.getUp());

        _incr.applyPost(camera.getLeft(), temp);
        camera.setLeft(temp);

        _incr.applyPost(camera.getDirection(), temp);
        camera.setDirection(temp);

        _incr.applyPost(camera.getUp(), temp);
        camera.setUp(temp);
        Vector3.releaseTempInstance(temp);

        camera.normalize();
    }

    private void turnLeft(final Canvas canvas, final double tpf) {
        turn(canvas, TURN_SPEED * tpf);
    }

    private void moveForward(final Canvas canvas, final double tpf) {
        final Camera camera = canvas.getCanvasRenderer().getCamera();
        final Vector3 loc = Vector3.fetchTempInstance().set(camera.getLocation());
        final Vector3 dir = Vector3.fetchTempInstance();
        if (camera.getProjectionMode() == ProjectionMode.Perspective) {
            dir.set(camera.getDirection());
        } else {
            // move up if in parallel mode
            dir.set(camera.getUp());
        }
        dir.multiplyLocal(MOVE_SPEED * tpf);
        loc.addLocal(dir);
        camera.setLocation(loc);
        Vector3.releaseTempInstance(loc);
        Vector3.releaseTempInstance(dir);
    }

    private void moveLeft(final Canvas canvas, final double tpf) {
        final Camera camera = canvas.getCanvasRenderer().getCamera();
        final Vector3 loc = Vector3.fetchTempInstance().set(camera.getLocation());
        final Vector3 dir = Vector3.fetchTempInstance();

        dir.set(camera.getLeft());

        dir.multiplyLocal(MOVE_SPEED * tpf);
        loc.addLocal(dir);
        camera.setLocation(loc);
        Vector3.releaseTempInstance(loc);
        Vector3.releaseTempInstance(dir);
    }

    private void moveRight(final Canvas canvas, final double tpf) {
        final Camera camera = canvas.getCanvasRenderer().getCamera();
        final Vector3 loc = Vector3.fetchTempInstance().set(camera.getLocation());
        final Vector3 dir = Vector3.fetchTempInstance();

        dir.set(camera.getLeft());

        dir.multiplyLocal(-MOVE_SPEED * tpf);
        loc.addLocal(dir);
        camera.setLocation(loc);
        Vector3.releaseTempInstance(loc);
        Vector3.releaseTempInstance(dir);
    }

    private void moveBack(final Canvas canvas, final double tpf) {
        final Camera camera = canvas.getCanvasRenderer().getCamera();
        final Vector3 loc = Vector3.fetchTempInstance().set(camera.getLocation());
        final Vector3 dir = Vector3.fetchTempInstance();
        if (camera.getProjectionMode() == ProjectionMode.Perspective) {
            dir.set(camera.getDirection());
        } else {
            // move up if in parallel mode
            dir.set(camera.getUp());
        }
        dir.multiplyLocal(-MOVE_SPEED * tpf);
        loc.addLocal(dir);
        camera.setLocation(loc);
        Vector3.releaseTempInstance(loc);
        Vector3.releaseTempInstance(dir);
    }
}
