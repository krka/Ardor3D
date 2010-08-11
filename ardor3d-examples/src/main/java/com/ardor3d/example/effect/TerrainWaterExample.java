/** * Copyright (c) 2008-2010 Ardor Labs, Inc. * * This file is part of Ardor3D. * * Ardor3D is free software: you can redistribute it and/or modify it  * under the terms of its license which may be found in the accompanying * LICENSE file or at <http://www.ardor3d.com/LICENSE>. */package com.ardor3d.example.effect;import java.awt.Color;import java.awt.image.BufferedImage;import java.nio.FloatBuffer;import com.ardor3d.example.ExampleBase;import com.ardor3d.example.Purpose;import com.ardor3d.extension.effect.water.WaterNode;import com.ardor3d.extension.terrain.GeometryClipmapTerrain;import com.ardor3d.extension.terrain.basic.BasicHeightmap;import com.ardor3d.extension.terrain.basic.BasicHeightmapPyramid;import com.ardor3d.extension.terrain.heightmap.MidPointHeightMapGenerator;import com.ardor3d.extension.terrain.util.BresenhamYUpGridTracer;import com.ardor3d.extension.terrain.util.HeightmapPyramidPicker;import com.ardor3d.framework.Canvas;import com.ardor3d.image.Image;import com.ardor3d.image.Texture;import com.ardor3d.image.util.AWTImageLoader;import com.ardor3d.input.Key;import com.ardor3d.input.logical.InputTrigger;import com.ardor3d.input.logical.KeyPressedCondition;import com.ardor3d.input.logical.TriggerAction;import com.ardor3d.input.logical.TwoInputStates;import com.ardor3d.light.DirectionalLight;import com.ardor3d.math.ColorRGBA;import com.ardor3d.math.MathUtils;import com.ardor3d.math.Plane;import com.ardor3d.math.Ray3;import com.ardor3d.math.Vector3;import com.ardor3d.renderer.Camera;import com.ardor3d.renderer.Renderer;import com.ardor3d.renderer.queue.RenderBucketType;import com.ardor3d.renderer.state.CullState;import com.ardor3d.renderer.state.FogState;import com.ardor3d.renderer.state.TextureState;import com.ardor3d.renderer.state.FogState.DensityFunction;import com.ardor3d.scenegraph.Node;import com.ardor3d.scenegraph.extension.Skybox;import com.ardor3d.scenegraph.hint.CullHint;import com.ardor3d.scenegraph.hint.LightCombineMode;import com.ardor3d.scenegraph.shape.Quad;import com.ardor3d.scenegraph.shape.Sphere;import com.ardor3d.ui.text.BasicText;import com.ardor3d.util.ReadOnlyTimer;import com.ardor3d.util.TextureManager;/** * Example showing how to combine the terrain and water systems. Requires GLSL support. */@Purpose(htmlDescriptionKey = "com.ardor3d.example.effect.TerrainWaterExample", //thumbnailPath = "com/ardor3d/example/media/thumbnails/effect_TerrainWaterExample.jpg", //maxHeapMemory = 128)public class TerrainWaterExample extends ExampleBase {    final int SIZE = 2048;    private boolean updateTerrain = true;    private final float farPlane = 3500.0f;    private final float heightOffset = 3.0f;    // Clipmap parameters and a list of clipmaps that are used.    private final int clipLevelCount = 7;    private final int clipSideSize = 127;    private final float heightScale = 500;    /** The water instance taking care of the water rendering. */    private WaterNode waterNode;    private boolean aboveWater = true;    /** Node containing debug quads for showing waternode render textures. */    private Node debugQuadsNode;    /** The quad used as geometry for the water. */    private Quad waterQuad;    private GeometryClipmapTerrain geometryClipmapTerrain;    private BasicHeightmapPyramid heightmapPyramid;    private boolean groundCamera = false;    private Camera terrainCamera;    private Skybox skybox;    private final Sphere sphere = new Sphere("sp", 16, 16, 1);    private HeightmapPyramidPicker picker;    private final double textureScale = 0.05;    private FogState fogState;    private boolean showUI = true;    private final Ray3 pickRay = new Ray3();    /** Text fields used to present info about the example. */    private final BasicText _exampleInfo[] = new BasicText[10];    /**     * The main method.     *      * @param args     *            the arguments     */    public static void main(final String[] args) {        start(TerrainWaterExample.class);    }    private double counter = 0;    private int frames = 0;    /**     * Update the PassManager, skybox, camera position, etc.     *      * @param timer     *            the application timer     */    @Override    protected void updateExample(final ReadOnlyTimer timer) {        final Camera camera = _canvas.getCanvasRenderer().getCamera();        final double height = getHeight(camera.getLocation().getX(), camera.getLocation().getZ()) + heightOffset;        if (groundCamera || camera.getLocation().getY() < height) {            camera.setLocation(new Vector3(camera.getLocation().getX(), height, camera.getLocation().getZ()));        }        if (aboveWater && camera.getLocation().getY() < waterNode.getWaterHeight()) {            fogState.setStart(-1000f);            fogState.setEnd(farPlane / 10f);            fogState.setColor(new ColorRGBA(0.0f, 0.0f, 0.1f, 1.0f));            aboveWater = false;        } else if (!aboveWater && camera.getLocation().getY() >= waterNode.getWaterHeight()) {            fogState.setStart(farPlane / 2.0f);            fogState.setEnd(farPlane);            fogState.setColor(new ColorRGBA(0.96f, 0.97f, 1.0f, 1.0f));            aboveWater = true;        }        if (updateTerrain) {            terrainCamera.set(camera);        }        skybox.setTranslation(camera.getLocation());        counter += timer.getTimePerFrame();        frames++;        if (counter > 1) {            final double fps = (frames / counter);            counter = 0;            frames = 0;            System.out.printf("%7.1f FPS\n", fps);        }        // if we're picking...        if (sphere.getSceneHints().getCullHint() == CullHint.Dynamic) {            // Set up our pick ray            pickRay.setOrigin(camera.getLocation());            pickRay.setDirection(camera.getDirection());            // do pick and move the sphere            final Vector3 loc = picker                    .getTerrainIntersection(geometryClipmapTerrain.getWorldTransform(), pickRay, null);            if (loc != null && Vector3.isValid(loc)) {                sphere.setTranslation(loc);                // XXX: maybe change the color of the ball for valid vs. invalid?            }        }        final Vector3 transVec = new Vector3(camera.getLocation().getX(), waterNode.getWaterHeight(), camera                .getLocation().getZ());        setTextureCoords(0, transVec.getX(), -transVec.getZ(), textureScale);        // vertex coords        setVertexCoords(transVec.getX(), transVec.getY(), transVec.getZ());        waterNode.update(timer.getTimePerFrame());    }    /**     * Render example.     *      * @param renderer     *            the renderer     */    @Override    protected void renderExample(final Renderer renderer) {        super.renderExample(renderer);        if (debugQuadsNode == null) {            createDebugQuads();            _root.attachChild(debugQuadsNode);        }    }    /**     * Initialize pssm pass and scene.     */    @Override    protected void initExample() {        // Setup main camera.        _canvas.setTitle("Terrain + Water - Example");        _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(0, 100, 0));        _canvas.getCanvasRenderer().getCamera().lookAt(new Vector3(0, 100, 1), Vector3.UNIT_Y);        _canvas.getCanvasRenderer().getCamera().setFrustumPerspective(                65.0,                (float) _canvas.getCanvasRenderer().getCamera().getWidth()                        / _canvas.getCanvasRenderer().getCamera().getHeight(), 1.0f, farPlane);        _controlHandle.setMoveSpeed(50);        _lightState.detachAll();        final DirectionalLight dLight = new DirectionalLight();        dLight.setEnabled(true);        dLight.setAmbient(new ColorRGBA(0.4f, 0.4f, 0.5f, 1));        dLight.setDiffuse(new ColorRGBA(0.6f, 0.6f, 0.5f, 1));        dLight.setSpecular(new ColorRGBA(0.3f, 0.3f, 0.2f, 1));        dLight.setDirection(new Vector3(-1, -1, -1).normalizeLocal());        _lightState.attach(dLight);        _lightState.setEnabled(true);        final CullState cs = new CullState();        cs.setEnabled(true);        cs.setCullFace(CullState.Face.Back);        _root.setRenderState(cs);        fogState = new FogState();        fogState.setStart(farPlane / 2.0f);        fogState.setEnd(farPlane);        fogState.setColor(new ColorRGBA(0.96f, 0.97f, 1.0f, 1.0f));        fogState.setDensityFunction(DensityFunction.Linear);        _root.setRenderState(fogState);        // add our sphere, but have it off for now.        sphere.getSceneHints().setCullHint(CullHint.Always);        _root.attachChild(sphere);        try {            // Load an ugly procedural heightmap            final float[] heightMap = loadHeightMap();            // Create ugly procedural texturing            final TextureState ts = loadTextures(heightMap);            // Create a heightmap pyramid for the clipmap terrain to use            heightmapPyramid = new BasicHeightmapPyramid(new BasicHeightmap(heightMap, SIZE), clipLevelCount);            // Keep a separate camera to be able to freeze terrain update            final Camera camera = _canvas.getCanvasRenderer().getCamera();            terrainCamera = new Camera(camera);            // Create the monster terrain engine            geometryClipmapTerrain = new GeometryClipmapTerrain(terrainCamera, heightmapPyramid, clipSideSize,                    heightScale);            // XXX: Set this if using other height ranges than the default 0..1            // geometryClipmapTerrain.setHeightRange(-0.5f, 0.5f);            // geometryClipmapTerrain.setTranslation(200, 100, 500);            // geometryClipmapTerrain.setScale(10, 1.0, 10);            geometryClipmapTerrain.setRenderState(ts);            // _root.attachChild(geometryClipmapTerrain);            // Create a picker            picker = new HeightmapPyramidPicker(heightmapPyramid, heightScale, new BresenhamYUpGridTracer(), farPlane);        } catch (final Exception ex1) {            ex1.printStackTrace();        }        final Node reflectedNode = new Node("reflectNode");        reflectedNode.attachChild(geometryClipmapTerrain);        skybox = buildSkyBox();        reflectedNode.attachChild(skybox);        final Camera cam = _canvas.getCanvasRenderer().getCamera();        // Create a new WaterNode with refraction enabled.        waterNode = new WaterNode(cam, 2, false, true);        // Setup textures to use for the water.        waterNode.setNormalMapTextureString("images/water/normalmap3.dds");        waterNode.setDudvMapTextureString("images/water/dudvmap.png");        waterNode.setFallbackMapTextureString("images/water/water2.png");        waterNode.useFadeToFogColor(true);        waterNode.setSpeedReflection(0.02);        waterNode.setSpeedReflection(-0.01);        // setting to default value just to show        waterNode.setWaterPlane(new Plane(new Vector3(0.0, 1.0, 0.0), 250.0));        // Create a quad to use as geometry for the water.        waterQuad = new Quad("waterQuad", 1, 1);        // Hack the quad normals to point up in the y-axis. Since we are manipulating the vertices as        // we move this is more convenient than rotating the quad.        final FloatBuffer normBuf = waterQuad.getMeshData().getNormalBuffer();        normBuf.clear();        normBuf.put(0).put(1).put(0);        normBuf.put(0).put(1).put(0);        normBuf.put(0).put(1).put(0);        normBuf.put(0).put(1).put(0);        waterNode.attachChild(waterQuad);        waterNode.addReflectedScene(reflectedNode);        waterNode.setSkybox(skybox);        _root.attachChild(reflectedNode);        _root.attachChild(waterNode);        // Setup cam above water and terrain        final Camera camera = _canvas.getCanvasRenderer().getCamera();        final double height = Math.max(getHeight(camera.getLocation().getX(), camera.getLocation().getZ()), waterNode                .getWaterHeight())                + heightOffset;        if (camera.getLocation().getY() < height) {            camera.setLocation(new Vector3(camera.getLocation().getX(), height, camera.getLocation().getZ()));        }        // Setup labels for presenting example info.        final Node textNodes = new Node("Text");        _root.attachChild(textNodes);        textNodes.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);        textNodes.getSceneHints().setLightCombineMode(LightCombineMode.Off);        final double infoStartY = _canvas.getCanvasRenderer().getCamera().getHeight() - 20;        for (int i = 0; i < _exampleInfo.length; i++) {            _exampleInfo[i] = BasicText.createDefaultTextLabel("Text", "", 16);            _exampleInfo[i].setTranslation(new Vector3(10, infoStartY - i * 20, 0));            textNodes.attachChild(_exampleInfo[i]);        }        textNodes.updateGeometricState(0.0);        updateText();        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.U), new TriggerAction() {            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                updateTerrain = !updateTerrain;                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ONE), new TriggerAction() {            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                _controlHandle.setMoveSpeed(5);                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.TWO), new TriggerAction() {            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                _controlHandle.setMoveSpeed(50);                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.THREE), new TriggerAction() {            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                _controlHandle.setMoveSpeed(400);                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), new TriggerAction() {            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                groundCamera = !groundCamera;                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.I), new TriggerAction() {            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                if (geometryClipmapTerrain.getVisibleLevels() < clipLevelCount - 1) {                    geometryClipmapTerrain.setVisibleLevels(geometryClipmapTerrain.getVisibleLevels() + 1);                    updateText();                }            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.K), new TriggerAction() {            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                if (geometryClipmapTerrain.getVisibleLevels() > 0) {                    geometryClipmapTerrain.setVisibleLevels(geometryClipmapTerrain.getVisibleLevels() - 1);                    updateText();                }            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.P), new TriggerAction() {            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                sphere.getSceneHints().setCullHint(                        sphere.getSceneHints().getCullHint() == CullHint.Always ? CullHint.Dynamic : CullHint.Always);                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.J), new TriggerAction() {            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                heightmapPyramid.setDoWrap(!heightmapPyramid.isDoWrap());                geometryClipmapTerrain.regenerate();                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.F), new TriggerAction() {            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                geometryClipmapTerrain.reloadShader();                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.V), new TriggerAction() {            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                showUI = !showUI;                if (showUI) {                    textNodes.getSceneHints().setCullHint(CullHint.Never);                    debugQuadsNode.getSceneHints().setCullHint(CullHint.Never);                } else {                    textNodes.getSceneHints().setCullHint(CullHint.Always);                    debugQuadsNode.getSceneHints().setCullHint(CullHint.Always);                }                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.B), new TriggerAction() {            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                waterNode.setDoBlurReflection(!waterNode.isDoBlurReflection());                waterNode.reloadShader();                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.F), new TriggerAction() {            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                final Camera camera = _canvas.getCanvasRenderer().getCamera();                camera.setLocation(new Vector3(camera.getLocation().getX() + 5000, camera.getLocation().getY(), camera                        .getLocation().getZ()));                updateText();            }        }));    }    private float[] loadHeightMap() {        // TODO: loading raw heightmap disabled for lower example media size        // final RawHeightMap raw = new RawHeightMap("heightmap.raw", SIZE);        // raw.setFormat(HeightMapFormat.UnsignedShort);        // raw.setLittleEndian(true);        // raw.setFlipY(true);        final MidPointHeightMapGenerator heightData = new MidPointHeightMapGenerator(SIZE, 1.0f, 1337);        heightData.generateHeightData();        final float[] heightMap = heightData.getHeightData();        return heightMap;    }    /**     * Sets the vertex coords of the quad.     *      * @param x     *            the x     * @param y     *            the y     * @param z     *            the z     */    private void setVertexCoords(final double x, final double y, final double z) {        final FloatBuffer vertBuf = waterQuad.getMeshData().getVertexBuffer();        vertBuf.clear();        vertBuf.put((float) (x - farPlane)).put((float) y).put((float) (z - farPlane));        vertBuf.put((float) (x - farPlane)).put((float) y).put((float) (z + farPlane));        vertBuf.put((float) (x + farPlane)).put((float) y).put((float) (z + farPlane));        vertBuf.put((float) (x + farPlane)).put((float) y).put((float) (z - farPlane));    }    /**     * Sets the texture coords of the quad.     *      * @param buffer     *            the buffer     * @param x     *            the x     * @param y     *            the y     * @param textureScale     *            the texture scale     */    private void setTextureCoords(final int buffer, double x, double y, double textureScale) {        x *= textureScale * 0.5f;        y *= textureScale * 0.5f;        textureScale = farPlane * textureScale;        FloatBuffer texBuf;        texBuf = waterQuad.getMeshData().getTextureBuffer(buffer);        texBuf.clear();        texBuf.put((float) x).put((float) (textureScale + y));        texBuf.put((float) x).put((float) y);        texBuf.put((float) (textureScale + x)).put((float) y);        texBuf.put((float) (textureScale + x)).put((float) (textureScale + y));    }    private TextureState loadTextures(final float[] heightMap) {        // TODO: loading generated texture/normals disabled for lower example media size        // final Texture colorTexture = TextureManager.load("texture.jpg",        // Texture.MinificationFilter.Trilinear, Format.GuessNoCompression, true);        final BufferedImage imgColor = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);        final BufferedImage imgNormal = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);        final ColorRGBA grass = new ColorRGBA(0.2f, 0.5f, 0.2f, 1.0f);        final ColorRGBA rock = new ColorRGBA(0.3f, 0.3f, 0.3f, 1.0f);        final ColorRGBA snow = new ColorRGBA(0.9f, 0.9f, 0.95f, 1.0f);        final ColorRGBA mud = new ColorRGBA(0.4f, 0.3f, 0.2f, 1.0f);        final ColorRGBA result = new ColorRGBA();        for (int i = 0; i < SIZE; i++) {            for (int j = 0; j < SIZE; j++) {                final float xDiff = heightMap[MathUtils.moduloPositive(i - 1, SIZE) * SIZE + (j)]                        - heightMap[MathUtils.moduloPositive(i + 1, SIZE) * SIZE + (j)];                final float zDiff = heightMap[(i) * SIZE + MathUtils.moduloPositive(j - 1, SIZE)]                        - heightMap[(i) * SIZE + MathUtils.moduloPositive(j + 1, SIZE)];                final Vector3 vec = new Vector3(xDiff, zDiff, 2f / heightScale).normalizeLocal();                final double dotAngle = vec.dot(0, 0, 1);                vec.multiplyLocal(0.5).addLocal(0.5, 0.5, 0.5);                imgNormal.setRGB(j, i, new Color(vec.getXf(), vec.getYf(), vec.getZf(), 0).getRGB());                ColorRGBA.lerp(rock, grass, (float) (dotAngle * dotAngle), result);                final float noise = (float) Math.random() * 0.06f - 0.03f;                result.addLocal(noise, noise, noise, 0.0f);                final float height = heightMap[i * SIZE + j];                ColorRGBA.lerp(result, snow, (float) Math.min(Math.max((height - 0.8) / 0.2, 0.0), 1.0), result);                ColorRGBA.lerp(result, mud, (float) Math.min(Math.max((0.2 - height) / 0.2, 0.0), 1.0), result);                imgColor.setRGB(j, i, new Color(result.getRed(), result.getGreen(), result.getBlue(), 0).getRGB());            }        }        final Image imageN = AWTImageLoader.makeArdor3dImage(imgNormal, false);        final Image imageT = AWTImageLoader.makeArdor3dImage(imgColor, false);        imgColor.flush();        imgNormal.flush();        final Texture colorTexture = TextureManager.loadFromImage(imageT, Texture.MinificationFilter.Trilinear);        final Texture normalTexture = TextureManager.loadFromImage(imageN, Texture.MinificationFilter.Trilinear);        final TextureState ts = new TextureState();        ts.setEnabled(true);        ts.setTexture(colorTexture, 0);        ts.setTexture(normalTexture, 1);        return ts;    }    /**     * Builds the sky box.     */    private Skybox buildSkyBox() {        final Skybox skybox = new Skybox("skybox", 10, 10, 10);        final String dir = "images/skybox/";        final Texture north = TextureManager                .load(dir + "1.jpg", Texture.MinificationFilter.BilinearNearestMipMap, true);        final Texture south = TextureManager                .load(dir + "3.jpg", Texture.MinificationFilter.BilinearNearestMipMap, true);        final Texture east = TextureManager.load(dir + "2.jpg", Texture.MinificationFilter.BilinearNearestMipMap, true);        final Texture west = TextureManager.load(dir + "4.jpg", Texture.MinificationFilter.BilinearNearestMipMap, true);        final Texture up = TextureManager.load(dir + "6.jpg", Texture.MinificationFilter.BilinearNearestMipMap, true);        final Texture down = TextureManager.load(dir + "5.jpg", Texture.MinificationFilter.BilinearNearestMipMap, true);        skybox.setTexture(Skybox.Face.North, north);        skybox.setTexture(Skybox.Face.West, west);        skybox.setTexture(Skybox.Face.South, south);        skybox.setTexture(Skybox.Face.East, east);        skybox.setTexture(Skybox.Face.Up, up);        skybox.setTexture(Skybox.Face.Down, down);        return skybox;    }    /**     * Update text information.     */    private void updateText() {        _exampleInfo[0].setText("Heightmap size: " + SIZE + "x" + SIZE);        _exampleInfo[1].setText("Spec: One meter per heightmap value");        _exampleInfo[2].setText("[1/2/3] Moving speed: " + _controlHandle.getMoveSpeed() + " m/s");        _exampleInfo[3].setText("[I/K] Max visible level: " + geometryClipmapTerrain.getVisibleLevels());        _exampleInfo[4].setText("[U] Update terrain: " + updateTerrain);        _exampleInfo[5].setText("[SPACE] Toggle fly/walk: " + (groundCamera ? "walk" : "fly"));        _exampleInfo[6].setText("[P] Toggle showing a sphere that follows the ground using picking: "                + (sphere.getSceneHints().getCullHint() != CullHint.Always));        _exampleInfo[7].setText("[J] Toggle heightmap wrapping: " + heightmapPyramid.isDoWrap());        _exampleInfo[8].setText("[B] Blur reflection: " + waterNode.isDoBlurReflection());        _exampleInfo[9].setText("[V] Show/Hide UI");    }    private final Vector3 transformedFrustumPos = new Vector3();    private double getHeight(double x, double z) {        transformedFrustumPos.set(x, 0, z);        geometryClipmapTerrain.getWorldTransform().applyInverse(transformedFrustumPos, transformedFrustumPos);        x = transformedFrustumPos.getX();        z = transformedFrustumPos.getZ();        x = x % SIZE;        x += x < 0 ? SIZE : 0;        z = z % SIZE;        z += z < 0 ? SIZE : 0;        final double col = Math.floor(x);        final double row = Math.floor(z);        final double intOnX = x - col, intOnZ = z - row;        double col1 = (col + 1) % SIZE;        col1 += col1 < 0 ? SIZE : 0;        double row1 = (row + 1) % SIZE;        row1 += row1 < 0 ? SIZE : 0;        // find the heightmap point closest to this position (but will always        // be to the left ( < x) and above (< z) of the spot.        final double topLeft = heightmapPyramid.getHeight(0, (int) col, (int) row);        // now find the next point to the right of topLeft's position...        final double topRight = heightmapPyramid.getHeight(0, (int) col1, (int) row);        // now find the next point below topLeft's position...        final double bottomLeft = heightmapPyramid.getHeight(0, (int) col, (int) row1);        // now find the next point below and to the right of topLeft's        // position...        final double bottomRight = heightmapPyramid.getHeight(0, (int) col1, (int) row1);        // Use linear interpolation to find the height.        return MathUtils.lerp(intOnZ, MathUtils.lerp(intOnX, topLeft, topRight), MathUtils.lerp(intOnX, bottomLeft,                bottomRight))                * heightScale                * geometryClipmapTerrain.getScale().getY()                + geometryClipmapTerrain.getTranslation().getY();    }    /**     * Creates the debug quads.     */    private void createDebugQuads() {        debugQuadsNode = new Node("quadNode");        debugQuadsNode.getSceneHints().setCullHint(CullHint.Never);        final double quadSize = _canvas.getCanvasRenderer().getCamera().getWidth() / 10;        Quad debugQuad = new Quad("reflectionQuad", quadSize, quadSize);        debugQuad.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);        debugQuad.getSceneHints().setCullHint(CullHint.Never);        debugQuad.getSceneHints().setLightCombineMode(LightCombineMode.Off);        TextureState ts = new TextureState();        ts.setTexture(waterNode.getTextureReflect());        debugQuad.setRenderState(ts);        debugQuad.setTranslation(quadSize * 0.6, quadSize * 1.0, 1.0);        debugQuadsNode.attachChild(debugQuad);        if (waterNode.getTextureRefract() != null) {            debugQuad = new Quad("refractionQuad", quadSize, quadSize);            debugQuad.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);            debugQuad.getSceneHints().setCullHint(CullHint.Never);            debugQuad.getSceneHints().setLightCombineMode(LightCombineMode.Off);            ts = new TextureState();            ts.setTexture(waterNode.getTextureRefract());            debugQuad.setRenderState(ts);            debugQuad.setTranslation(quadSize * 0.6, quadSize * 2.1, 1.0);            debugQuadsNode.attachChild(debugQuad);        }    }}