/** * Copyright (c) 2008-2010 Ardor Labs, Inc. * * This file is part of Ardor3D. * * Ardor3D is free software: you can redistribute it and/or modify it  * under the terms of its license which may be found in the accompanying * LICENSE file or at <http://www.ardor3d.com/LICENSE>. */package com.ardor3d.example.effect;import com.ardor3d.example.ExampleBase;import com.ardor3d.example.Purpose;import com.ardor3d.extension.terrain.HeightmapPyramid;import com.ardor3d.extension.terrain.TexturedGeometryClipmapTerrain;import com.ardor3d.extension.terrain.procedural.ProceduralHeightmap;import com.ardor3d.extension.terrain.procedural.ProceduralHeightmapPyramid;import com.ardor3d.extension.terrain.util.BresenhamYUpGridTracer;import com.ardor3d.extension.terrain.util.HeightmapPyramidPicker;import com.ardor3d.extension.texturing.ProceduralTextureStreamer;import com.ardor3d.extension.texturing.TextureClipmap;import com.ardor3d.framework.Canvas;import com.ardor3d.framework.FrameHandler;import com.ardor3d.image.Image;import com.ardor3d.image.Texture;import com.ardor3d.image.util.GeneratedImageFactory;import com.ardor3d.input.Key;import com.ardor3d.input.logical.InputTrigger;import com.ardor3d.input.logical.KeyPressedCondition;import com.ardor3d.input.logical.LogicalLayer;import com.ardor3d.input.logical.TriggerAction;import com.ardor3d.input.logical.TwoInputStates;import com.ardor3d.light.DirectionalLight;import com.ardor3d.math.ColorRGBA;import com.ardor3d.math.MathUtils;import com.ardor3d.math.Ray3;import com.ardor3d.math.Vector3;import com.ardor3d.math.functions.FbmFunction3D;import com.ardor3d.math.functions.Function3D;import com.ardor3d.math.functions.Functions;import com.ardor3d.math.type.ReadOnlyColorRGBA;import com.ardor3d.renderer.Camera;import com.ardor3d.renderer.queue.RenderBucketType;import com.ardor3d.renderer.state.CullState;import com.ardor3d.renderer.state.FogState;import com.ardor3d.renderer.state.FogState.DensityFunction;import com.ardor3d.scenegraph.Node;import com.ardor3d.scenegraph.extension.Skybox;import com.ardor3d.scenegraph.hint.CullHint;import com.ardor3d.scenegraph.hint.LightCombineMode;import com.ardor3d.scenegraph.shape.Sphere;import com.ardor3d.ui.text.BasicText;import com.ardor3d.util.ReadOnlyTimer;import com.ardor3d.util.TextureManager;import com.google.inject.Inject;/** * Example showing geometry and texture clipmapping using Functions as a data source. */@Purpose(htmlDescription = "Example showing geometry and texture clipmapping using Functions as a data source.   Requires support for GLSL.", //thumbnailPath = "/com/ardor3d/example/media/thumbnails/effect_ProceduralClipmapTerrainExample.jpg", //maxHeapMemory = 128)public class ProceduralClipmapTerrainExample extends ExampleBase {    final int SIZE = 2048;    private boolean updateTerrain = true;    private final float farPlane = 3500.0f;    private final float heightOffset = 3.0f;    // Clipmap parameters and a list of clipmaps that are used.    private final int clipLevelCount = 7;    private final int clipSideSize = 127;    private final float heightScale = 200;    private TexturedGeometryClipmapTerrain geometryClipmapTerrain;    private HeightmapPyramid heightmapPyramid;    private boolean groundCamera = true;    private Camera terrainCamera;    private Skybox skybox;    private final Sphere sphere = new Sphere("sp", 16, 16, 1);    private HeightmapPyramidPicker picker;    Ray3 pickRay = new Ray3();    /** Text fields used to present info about the example. */    private final BasicText _exampleInfo[] = new BasicText[8];    /**     * The main method.     *      * @param args     *            the arguments     */    public static void main(final String[] args) {        start(ProceduralClipmapTerrainExample.class);    }    /**     * Instantiates a new parallel split shadow map example.     *      * @param layer     *            the layer     * @param frameWork     *            the frame work     */    @Inject    public ProceduralClipmapTerrainExample(final LogicalLayer layer, final FrameHandler frameWork) {        super(layer, frameWork);    }    private double counter = 0;    private int frames = 0;    /**     * Update the terrain and our camera.     *      * @param timer     *            the application timer     */    @Override    protected void updateExample(final ReadOnlyTimer timer) {        final Camera camera = _canvas.getCanvasRenderer().getCamera();        final double height = getHeight(camera.getLocation().getX(), camera.getLocation().getZ());        if (groundCamera || camera.getLocation().getY() < height + heightOffset) {            camera.setLocation(new Vector3(camera.getLocation().getX(), height + heightOffset, camera.getLocation()                    .getZ()));        }        if (updateTerrain) {            terrainCamera.set(camera);        }        skybox.setTranslation(camera.getLocation());        counter += timer.getTimePerFrame();        frames++;        if (counter > 1) {            final double fps = (frames / counter);            counter = 0;            frames = 0;            System.out.printf("%7.1f FPS\n", fps);        }        // if we're picking...        if (sphere.getSceneHints().getCullHint() == CullHint.Dynamic) {            // Set up our pick ray            pickRay.setOrigin(camera.getLocation());            pickRay.setDirection(camera.getDirection());            // do pick and move the sphere            final Vector3 loc = picker                    .getTerrainIntersection(geometryClipmapTerrain.getWorldTransform(), pickRay, null);            if (loc != null && Vector3.isValid(loc)) {                sphere.setTranslation(loc);                // XXX: maybe change the color of the ball for valid vs. invalid?            }        }    }    /**     * Initialize terrain     */    @Override    protected void initExample() {        // Setup main camera.        _canvas.setTitle("Geometry Clipmap Terrain - Example");        _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(0, 100, 0));        _canvas.getCanvasRenderer().getCamera().lookAt(new Vector3(0, 100, 1), Vector3.UNIT_Y);        _canvas.getCanvasRenderer().getCamera().setFrustumPerspective(                65.0,                (float) _canvas.getCanvasRenderer().getCamera().getWidth()                        / _canvas.getCanvasRenderer().getCamera().getHeight(), 1.0f, farPlane);        _controlHandle.setMoveSpeed(5);        _lightState.detachAll();        final DirectionalLight dLight = new DirectionalLight();        dLight.setEnabled(true);        dLight.setAmbient(new ColorRGBA(0.4f, 0.4f, 0.5f, 1));        dLight.setDiffuse(new ColorRGBA(0.6f, 0.6f, 0.5f, 1));        dLight.setSpecular(new ColorRGBA(0.3f, 0.3f, 0.2f, 1));        dLight.setDirection(new Vector3(1, 1, 1).normalizeLocal());        _lightState.attach(dLight);        _lightState.setEnabled(true);        final CullState cs = new CullState();        cs.setEnabled(true);        cs.setCullFace(CullState.Face.Back);        _root.setRenderState(cs);        final FogState fs = new FogState();        fs.setStart(farPlane / 2.0f);        fs.setEnd(farPlane);        fs.setColor(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f));        fs.setDensityFunction(DensityFunction.Linear);        _root.setRenderState(fs);        // add our sphere, but have it off for now.        sphere.getSceneHints().setCullHint(CullHint.Always);        _root.attachChild(sphere);        try {            final int textureSliceSize = 256;            final double scale = 1.0 / 4000.0;            Function3D function = new FbmFunction3D(Functions.simplexNoise(), 5, 0.5, 0.5, 3.14);            function = Functions.clamp(function, -1.2, 1.2);            function = Functions.scaleInput(function, scale, scale, 1);            final ReadOnlyColorRGBA[] terrainColors = new ReadOnlyColorRGBA[256];            terrainColors[0] = new ColorRGBA(0, 0, .5f, 1);            terrainColors[95] = new ColorRGBA(0, 0, 1, 1);            terrainColors[127] = new ColorRGBA(0, .5f, 1, 1);            terrainColors[137] = new ColorRGBA(240 / 255f, 240 / 255f, 64 / 255f, 1);            terrainColors[143] = new ColorRGBA(32 / 255f, 160 / 255f, 0, 1);            terrainColors[175] = new ColorRGBA(224 / 255f, 224 / 255f, 0, 1);            terrainColors[223] = new ColorRGBA(128 / 255f, 128 / 255f, 128 / 255f, 1);            terrainColors[255] = ColorRGBA.WHITE;            GeneratedImageFactory.fillInColorTable(terrainColors);            // Add a touch of noise as a "detail texture"            final Function3D texFunction = Functions.lerp(function, Functions.simplexNoise(), 0.02);            final ProceduralTextureStreamer streamer = new ProceduralTextureStreamer(65536, textureSliceSize,                    texFunction, terrainColors);            final int validLevels = streamer.getValidLevels();            final TextureClipmap textureClipmap = new TextureClipmap(streamer, textureSliceSize, validLevels, 64f);            // Create a heightmap pyramid for the clipmap terrain to use            heightmapPyramid = new ProceduralHeightmapPyramid(new ProceduralHeightmap(function), clipLevelCount);            // Keep a separate camera to be able to freeze terrain update            final Camera camera = _canvas.getCanvasRenderer().getCamera();            terrainCamera = new Camera(camera);            // Create the monster terrain engine            geometryClipmapTerrain = new TexturedGeometryClipmapTerrain(textureClipmap, terrainCamera,                    heightmapPyramid, clipSideSize, heightScale);            geometryClipmapTerrain.setHeightRange(-1.25f, 1.25f);            // geometryClipmapTerrain.setRenderState(ts);            _root.attachChild(geometryClipmapTerrain);            // Create a picker            picker = new HeightmapPyramidPicker(heightmapPyramid, heightScale, new BresenhamYUpGridTracer(), farPlane);        } catch (final Exception ex1) {            ex1.printStackTrace();        }        skybox = buildSkyBox();        _root.attachChild(skybox);        // Setup labels for presenting example info.        final Node textNodes = new Node("Text");        _root.attachChild(textNodes);        textNodes.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);        textNodes.getSceneHints().setLightCombineMode(LightCombineMode.Off);        final double infoStartY = _canvas.getCanvasRenderer().getCamera().getHeight() / 2;        for (int i = 0; i < _exampleInfo.length; i++) {            _exampleInfo[i] = BasicText.createDefaultTextLabel("Text", "", 16);            _exampleInfo[i].setTranslation(new Vector3(10, infoStartY - i * 20, 0));            textNodes.attachChild(_exampleInfo[i]);        }        textNodes.updateGeometricState(0.0);        updateText();        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.U), new TriggerAction() {            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                updateTerrain = !updateTerrain;                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ONE), new TriggerAction() {            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                _controlHandle.setMoveSpeed(5);                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.TWO), new TriggerAction() {            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                _controlHandle.setMoveSpeed(50);                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.THREE), new TriggerAction() {            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                _controlHandle.setMoveSpeed(400);                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), new TriggerAction() {            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                groundCamera = !groundCamera;                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.I), new TriggerAction() {            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                if (geometryClipmapTerrain.getVisibleLevels() < clipLevelCount - 1) {                    geometryClipmapTerrain.setVisibleLevels(geometryClipmapTerrain.getVisibleLevels() + 1);                    updateText();                }            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.K), new TriggerAction() {            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                if (geometryClipmapTerrain.getVisibleLevels() > 0) {                    geometryClipmapTerrain.setVisibleLevels(geometryClipmapTerrain.getVisibleLevels() - 1);                    updateText();                }            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.P), new TriggerAction() {            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                sphere.getSceneHints().setCullHint(                        sphere.getSceneHints().getCullHint() == CullHint.Always ? CullHint.Dynamic : CullHint.Always);                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.R), new TriggerAction() {            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                geometryClipmapTerrain.getTextureClipmap().setShowDebug(                        !geometryClipmapTerrain.getTextureClipmap().isShowDebug());                geometryClipmapTerrain.reloadShaderParameters();                updateText();            }        }));    }    /**     * Builds the sky box.     */    private Skybox buildSkyBox() {        final Skybox skybox = new Skybox("skybox", 10, 10, 10);        final String dir = "images/skybox/";        final Texture north = TextureManager.load(dir + "1.jpg", Texture.MinificationFilter.BilinearNearestMipMap,                Image.Format.GuessNoCompression, true);        final Texture south = TextureManager.load(dir + "3.jpg", Texture.MinificationFilter.BilinearNearestMipMap,                Image.Format.GuessNoCompression, true);        final Texture east = TextureManager.load(dir + "2.jpg", Texture.MinificationFilter.BilinearNearestMipMap,                Image.Format.GuessNoCompression, true);        final Texture west = TextureManager.load(dir + "4.jpg", Texture.MinificationFilter.BilinearNearestMipMap,                Image.Format.GuessNoCompression, true);        final Texture up = TextureManager.load(dir + "6.jpg", Texture.MinificationFilter.BilinearNearestMipMap,                Image.Format.GuessNoCompression, true);        final Texture down = TextureManager.load(dir + "5.jpg", Texture.MinificationFilter.BilinearNearestMipMap,                Image.Format.GuessNoCompression, true);        skybox.setTexture(Skybox.Face.North, north);        skybox.setTexture(Skybox.Face.West, west);        skybox.setTexture(Skybox.Face.South, south);        skybox.setTexture(Skybox.Face.East, east);        skybox.setTexture(Skybox.Face.Up, up);        skybox.setTexture(Skybox.Face.Down, down);        return skybox;    }    /**     * Update text information.     */    private void updateText() {        _exampleInfo[0].setText("Heightmap size: infinite");// + SIZE + "x" + SIZE);        _exampleInfo[1].setText("Spec: One meter per heightmap value");        _exampleInfo[2].setText("[1/2/3] Moving speed: " + _controlHandle.getMoveSpeed() + " m/s");        _exampleInfo[3].setText("[I/K] Max visible level: " + geometryClipmapTerrain.getVisibleLevels());        _exampleInfo[4].setText("[U] Update terrain: " + updateTerrain);        _exampleInfo[5].setText("[SPACE] Toggle fly/walk: " + (groundCamera ? "walk" : "fly"));        _exampleInfo[6].setText("[P] Toggle showing a sphere that follows the ground using picking: "                + (sphere.getSceneHints().getCullHint() != CullHint.Always));        _exampleInfo[7].setText("[R] Toggle texture clipmap debug: "                + geometryClipmapTerrain.getTextureClipmap().isShowDebug());    }    private double getHeight(final double x, final double z) {        final double col = Math.floor(x);        final double row = Math.floor(z);        final double intOnX = x - col, intOnZ = z - row;        final double col1 = (col + 1);        final double row1 = (row + 1);        // find the heightmap point closest to this position (but will always        // be to the left ( < x) and above (< z) of the spot.        final double topLeft = heightmapPyramid.getHeight(0, (int) col, (int) row);        // now find the next point to the right of topLeft's position...        final double topRight = heightmapPyramid.getHeight(0, (int) col1, (int) row);        // now find the next point below topLeft's position...        final double bottomLeft = heightmapPyramid.getHeight(0, (int) col, (int) row1);        // now find the next point below and to the right of topLeft's        // position...        final double bottomRight = heightmapPyramid.getHeight(0, (int) col1, (int) row1);        // Use linear interpolation to find the height.        return MathUtils.lerp(intOnZ, MathUtils.lerp(intOnX, topLeft, topRight), MathUtils.lerp(intOnX, bottomLeft,                bottomRight))                * heightScale;    }}