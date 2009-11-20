/** * Copyright (c) 2008-2009 Ardor Labs, Inc. * * This file is part of Ardor3D. * * Ardor3D is free software: you can redistribute it and/or modify it  * under the terms of its license which may be found in the accompanying * LICENSE file or at <http://www.ardor3d.com/LICENSE>. */package com.ardor3d.example.effect;import java.nio.FloatBuffer;import com.ardor3d.bounding.BoundingBox;import com.ardor3d.example.ExampleBase;import com.ardor3d.extension.effect.water.WaterNode;import com.ardor3d.framework.Canvas;import com.ardor3d.framework.FrameHandler;import com.ardor3d.image.Image;import com.ardor3d.image.Texture;import com.ardor3d.image.Image.Format;import com.ardor3d.input.Key;import com.ardor3d.input.logical.InputTrigger;import com.ardor3d.input.logical.KeyPressedCondition;import com.ardor3d.input.logical.LogicalLayer;import com.ardor3d.input.logical.TriggerAction;import com.ardor3d.input.logical.TwoInputStates;import com.ardor3d.math.ColorRGBA;import com.ardor3d.math.Matrix3;import com.ardor3d.math.Plane;import com.ardor3d.math.Vector3;import com.ardor3d.renderer.Camera;import com.ardor3d.renderer.Renderer;import com.ardor3d.renderer.queue.RenderBucketType;import com.ardor3d.renderer.state.CullState;import com.ardor3d.renderer.state.FogState;import com.ardor3d.renderer.state.MaterialState;import com.ardor3d.renderer.state.TextureState;import com.ardor3d.renderer.state.MaterialState.ColorMaterial;import com.ardor3d.scenegraph.Node;import com.ardor3d.scenegraph.controller.SpatialController;import com.ardor3d.scenegraph.extension.Skybox;import com.ardor3d.scenegraph.hint.CullHint;import com.ardor3d.scenegraph.hint.LightCombineMode;import com.ardor3d.scenegraph.shape.Box;import com.ardor3d.scenegraph.shape.Quad;import com.ardor3d.scenegraph.shape.Torus;import com.ardor3d.scenegraph.visitor.UpdateModelBoundVisitor;import com.ardor3d.ui.text.BasicText;import com.ardor3d.util.ReadOnlyTimer;import com.ardor3d.util.TextureManager;import com.google.inject.Inject;/** * * Example showing the WaterNode. */public class WaterExample extends ExampleBase {    /** The water instance taking care of the water rendering. */    private WaterNode waterNode;    /** The skybox. */    private Skybox skybox;    /** The quad used as geometry for the water. */    private Quad waterQuad;    /** The far plane. */    private final double farPlane = 10000.0;    /** The texture scale to use for the water quad. */    private final double textureScale = 0.02;    /** Node containing debug quads for showing waternode render textures. */    private Node debugQuadsNode;    /** Flag for showing/hiding debug quads. */    private boolean showDebugQuads = true;    /** Text fields used to present info about the example. */    private final BasicText _exampleInfo[] = new BasicText[1];    /**     * The main method.     *      * @param args     *            the args     */    public static void main(final String[] args) {        start(WaterExample.class);    }    /**     * Instantiates a new quad water example.     *      * @param layer     *            the layer     * @param frameWork     *            the frame work     */    @Inject    public WaterExample(final LogicalLayer layer, final FrameHandler frameWork) {        super(layer, frameWork);    }    /**     * Update skybox location and waterQuad position.     *      * @param timer     *            the timer     */    @Override    protected void updateExample(final ReadOnlyTimer timer) {        final Camera cam = _canvas.getCanvasRenderer().getCamera();        skybox.setTranslation(cam.getLocation());        skybox.updateGeometricState(0.0f, true);        final Vector3 transVec = new Vector3(cam.getLocation().getX(), waterNode.getWaterHeight(), cam.getLocation()                .getZ());        setTextureCoords(0, transVec.getX(), -transVec.getZ(), textureScale);        // vertex coords        setVertexCoords(transVec.getX(), transVec.getY(), transVec.getZ());        waterNode.update(timer.getTimePerFrame());    }    /**     * Render example.     *      * @param renderer     *            the renderer     */    @Override    protected void renderExample(final Renderer renderer) {        super.renderExample(renderer);        if (debugQuadsNode == null) {            createDebugQuads();            _root.attachChild(debugQuadsNode);        }    }    /**     * Initialize water node and scene.     */    @Override    protected void initExample() {        // Setup main camera.        _canvas.setTitle("Quad Water - Example");        _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(100, 50, 100));        _canvas.getCanvasRenderer().getCamera().setFrustumPerspective(                45.0,                (float) _canvas.getCanvasRenderer().getCamera().getWidth()                        / (float) _canvas.getCanvasRenderer().getCamera().getHeight(), 1, farPlane);        _canvas.getCanvasRenderer().getCamera().lookAt(new Vector3(0, 0, 0), Vector3.UNIT_Y);        // Setup some standard states for the scene.        final CullState cullFrontFace = new CullState();        cullFrontFace.setEnabled(true);        cullFrontFace.setCullFace(CullState.Face.Back);        _root.setRenderState(cullFrontFace);        final TextureState ts = new TextureState();        ts.setEnabled(true);        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear,                Format.GuessNoCompression, true));        _root.setRenderState(ts);        final MaterialState ms = new MaterialState();        ms.setColorMaterial(ColorMaterial.Diffuse);        _root.setRenderState(ms);        // Need to setup fog cause the water use it for various calculations.        setupFog();        // Collect everything we want reflected in the water under a node.        final Node reflectedNode = new Node("reflectNode");        reflectedNode.attachChild(createObjects());        buildSkyBox();        reflectedNode.attachChild(skybox);        _root.attachChild(reflectedNode);        final Camera cam = _canvas.getCanvasRenderer().getCamera();        // Create a new WaterNode with refraction enabled.        waterNode = new WaterNode(cam, 4, false, true);        // Setup textures to use for the water.        waterNode.setNormalMapTextureString("images/water/normalmap3.dds");        waterNode.setDudvMapTextureString("images/water/dudvmap.png");        waterNode.setFallbackMapTextureString("images/water/water2.png");        // setting to default value just to show        waterNode.setWaterPlane(new Plane(new Vector3(0.0, 1.0, 0.0), 0.0));        // Create a quad to use as geometry for the water.        waterQuad = new Quad("waterQuad", 1, 1);        // Hack the quad normals to point up in the y-axis. Since we are manipulating the vertices as        // we move this is more convenient than rotating the quad.        final FloatBuffer normBuf = waterQuad.getMeshData().getNormalBuffer();        normBuf.clear();        normBuf.put(0).put(1).put(0);        normBuf.put(0).put(1).put(0);        normBuf.put(0).put(1).put(0);        normBuf.put(0).put(1).put(0);        waterNode.attachChild(waterQuad);        waterNode.addReflectedScene(reflectedNode);        waterNode.setSkybox(skybox);        _root.attachChild(waterNode);        // Setyp textfields for presenting example info.        final Node textNodes = new Node("Text");        _root.attachChild(textNodes);        textNodes.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);        textNodes.getSceneHints().setLightCombineMode(LightCombineMode.Off);        final double infoStartY = _canvas.getCanvasRenderer().getCamera().getHeight() / 2;        for (int i = 0; i < _exampleInfo.length; i++) {            _exampleInfo[i] = BasicText.createDefaultTextLabel("Text", "", 16);            _exampleInfo[i].setTranslation(new Vector3(10, infoStartY - i * 20, 0));            textNodes.attachChild(_exampleInfo[i]);        }        textNodes.updateGeometricState(0.0);        updateText();        // Register keyboard triggers for manipulating example        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), new TriggerAction() {            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                switchShowDebug();                updateText();            }        }));        // Make sure all boundings are updated.        _root.acceptVisitor(new UpdateModelBoundVisitor(), false);    }    /**     * Sets the vertex coords of the quad.     *      * @param x     *            the x     * @param y     *            the y     * @param z     *            the z     */    private void setVertexCoords(final double x, final double y, final double z) {        final FloatBuffer vertBuf = waterQuad.getMeshData().getVertexBuffer();        vertBuf.clear();        vertBuf.put((float) (x - farPlane)).put((float) y).put((float) (z - farPlane));        vertBuf.put((float) (x - farPlane)).put((float) y).put((float) (z + farPlane));        vertBuf.put((float) (x + farPlane)).put((float) y).put((float) (z + farPlane));        vertBuf.put((float) (x + farPlane)).put((float) y).put((float) (z - farPlane));    }    /**     * Sets the texture coords of the quad.     *      * @param buffer     *            the buffer     * @param x     *            the x     * @param y     *            the y     * @param textureScale     *            the texture scale     */    private void setTextureCoords(final int buffer, double x, double y, double textureScale) {        x *= textureScale * 0.5f;        y *= textureScale * 0.5f;        textureScale = farPlane * textureScale;        FloatBuffer texBuf;        texBuf = waterQuad.getMeshData().getTextureBuffer(buffer);        texBuf.clear();        texBuf.put((float) x).put((float) (textureScale + y));        texBuf.put((float) x).put((float) y);        texBuf.put((float) (textureScale + x)).put((float) y);        texBuf.put((float) (textureScale + x)).put((float) (textureScale + y));    }    /**     * Setup fog.     */    private void setupFog() {        final FogState fogState = new FogState();        fogState.setDensity(1.0f);        fogState.setEnabled(true);        fogState.setColor(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f));        fogState.setEnd((float) farPlane);        fogState.setStart((float) farPlane / 10.0f);        fogState.setDensityFunction(FogState.DensityFunction.Linear);        fogState.setQuality(FogState.Quality.PerVertex);        _root.setRenderState(fogState);    }    /**     * Builds the sky box.     */    private void buildSkyBox() {        skybox = new Skybox("skybox", 10, 10, 10);        final String dir = "images/skybox/";        final Texture north = TextureManager.load(dir + "1.jpg", Texture.MinificationFilter.BilinearNearestMipMap,                Image.Format.GuessNoCompression, true);        final Texture south = TextureManager.load(dir + "3.jpg", Texture.MinificationFilter.BilinearNearestMipMap,                Image.Format.GuessNoCompression, true);        final Texture east = TextureManager.load(dir + "2.jpg", Texture.MinificationFilter.BilinearNearestMipMap,                Image.Format.GuessNoCompression, true);        final Texture west = TextureManager.load(dir + "4.jpg", Texture.MinificationFilter.BilinearNearestMipMap,                Image.Format.GuessNoCompression, true);        final Texture up = TextureManager.load(dir + "6.jpg", Texture.MinificationFilter.BilinearNearestMipMap,                Image.Format.GuessNoCompression, true);        final Texture down = TextureManager.load(dir + "5.jpg", Texture.MinificationFilter.BilinearNearestMipMap,                Image.Format.GuessNoCompression, true);        skybox.setTexture(Skybox.Face.North, north);        skybox.setTexture(Skybox.Face.West, west);        skybox.setTexture(Skybox.Face.South, south);        skybox.setTexture(Skybox.Face.East, east);        skybox.setTexture(Skybox.Face.Up, up);        skybox.setTexture(Skybox.Face.Down, down);    }    /**     * Creates the scene objects.     *      * @return the node containing the objects     */    private Node createObjects() {        final Node objects = new Node("objects");        final Torus torus = new Torus("Torus", 50, 50, 8, 17);        torus.setTranslation(new Vector3(50, -5, 20));        TextureState ts = new TextureState();        torus.addController(new SpatialController<Torus>() {            private static final long serialVersionUID = 1L;            private double timer = 0;            private final Matrix3 rotation = new Matrix3();            public void update(final double time, final Torus caller) {                timer += time * 0.5;                caller.setTranslation(Math.sin(timer) * 40.0, Math.sin(timer) * 40.0, Math.cos(timer) * 40.0);                rotation.fromAngles(timer * 0.5, timer * 0.5, timer * 0.5);                caller.setRotation(rotation);            }        });        Texture t0 = TextureManager.load("images/ardor3d_white_256.jpg",                Texture.MinificationFilter.BilinearNearestMipMap, Image.Format.GuessNoCompression, true);        ts.setTexture(t0, 0);        ts.setEnabled(true);        torus.setRenderState(ts);        objects.attachChild(torus);        ts = new TextureState();        t0 = TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.BilinearNearestMipMap,                Image.Format.GuessNoCompression, true);        t0.setWrap(Texture.WrapMode.Repeat);        ts.setTexture(t0);        Box box = new Box("box1", new Vector3(-10, -10, -10), new Vector3(10, 10, 10));        box.setTranslation(new Vector3(0, -7, 0));        box.setRenderState(ts);        objects.attachChild(box);        box = new Box("box2", new Vector3(-5, -5, -5), new Vector3(5, 5, 5));        box.setTranslation(new Vector3(15, 10, 0));        box.setRenderState(ts);        objects.attachChild(box);        box = new Box("box3", new Vector3(-5, -5, -5), new Vector3(5, 5, 5));        box.setTranslation(new Vector3(0, -10, 15));        box.setRenderState(ts);        objects.attachChild(box);        box = new Box("box4", new Vector3(-5, -5, -5), new Vector3(5, 5, 5));        box.setTranslation(new Vector3(20, 0, 0));        box.setRenderState(ts);        objects.attachChild(box);        ts = new TextureState();        t0 = TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.BilinearNearestMipMap,                Image.Format.GuessNoCompression, true);        t0.setWrap(Texture.WrapMode.Repeat);        ts.setTexture(t0);        box = new Box("box5", new Vector3(-50, -2, -50), new Vector3(50, 2, 50));        box.setTranslation(new Vector3(0, -15, 0));        box.setRenderState(ts);        box.setModelBound(new BoundingBox());        objects.attachChild(box);        return objects;    }    /**     * Switch show debug.     */    private void switchShowDebug() {        showDebugQuads = !showDebugQuads;        if (showDebugQuads) {            debugQuadsNode.getSceneHints().setCullHint(CullHint.Never);        } else {            debugQuadsNode.getSceneHints().setCullHint(CullHint.Always);        }    }    /**     * Creates the debug quads.     */    private void createDebugQuads() {        debugQuadsNode = new Node("quadNode");        debugQuadsNode.getSceneHints().setCullHint(CullHint.Never);        final double quadSize = _canvas.getCanvasRenderer().getCamera().getWidth() / 10;        Quad debugQuad = new Quad("reflectionQuad", quadSize, quadSize);        debugQuad.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);        debugQuad.getSceneHints().setCullHint(CullHint.Never);        debugQuad.getSceneHints().setLightCombineMode(LightCombineMode.Off);        TextureState ts = new TextureState();        ts.setTexture(waterNode.getTextureReflect());        debugQuad.setRenderState(ts);        debugQuad.setTranslation(quadSize * 0.6, quadSize * 1.0, 1.0);        debugQuadsNode.attachChild(debugQuad);        if (waterNode.getTextureRefract() != null) {            debugQuad = new Quad("refractionQuad", quadSize, quadSize);            debugQuad.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);            debugQuad.getSceneHints().setCullHint(CullHint.Never);            debugQuad.getSceneHints().setLightCombineMode(LightCombineMode.Off);            ts = new TextureState();            ts.setTexture(waterNode.getTextureRefract());            debugQuad.setRenderState(ts);            debugQuad.setTranslation(quadSize * 0.6, quadSize * 2.1, 1.0);            debugQuadsNode.attachChild(debugQuad);        }    }    /**     * Update text information.     */    private void updateText() {        _exampleInfo[0].setText("[SPACE] Show debug quads: " + showDebugQuads);    }}