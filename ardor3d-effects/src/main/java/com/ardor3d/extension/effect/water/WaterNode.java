/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.effect.water;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture2D;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Plane;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.TextureRenderer;
import com.ardor3d.renderer.TextureRendererFactory;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.ClipState;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.FogState;
import com.ardor3d.renderer.state.GLSLShaderObjectsState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.TextureManager;

/**
 * The WaterNode handles rendering of a water effect on all of it's children. What is reflected in the water is
 * controlled through setReflectedScene/addReflectedScene. The skybox (if any) needs to be explicitly set through
 * setSkybox since it needs to be relocated when renderering the reflection. The water is treated as a plane no matter
 * what the geometry is, which is controlled through the water node plane equation settings.
 */
public class WaterNode extends Node {
    private static final Logger logger = Logger.getLogger(WaterNode.class.getName());

    private static final long serialVersionUID = 1L;

    protected Camera cam;
    protected double tpf;
    protected double reflectionThrottle = 1 / 50f, refractionThrottle = 1 / 50f;
    protected double reflectionTime = 0, refractionTime = 0;
    protected boolean useFadeToFogColor = false;

    protected TextureRenderer tRenderer;
    protected Texture2D textureReflect;
    protected Texture2D textureRefract;
    protected Texture2D textureDepth;

    protected ArrayList<Spatial> renderList;
    protected ArrayList<Texture> texArray = new ArrayList<Texture>();
    protected Node skyBox;

    protected GLSLShaderObjectsState waterShader;
    protected CullState cullBackFace;
    protected TextureState textureState;
    protected TextureState fallbackTextureState;

    private Texture normalmapTexture;
    private Texture dudvTexture;
    private Texture foamTexture;
    private Texture fallbackTexture;
    private Matrix4 fallbackTextureStateMatrix;

    protected BlendState as1;
    protected ClipState clipState;
    protected FogState noFog;

    protected Plane waterPlane;
    protected Vector3 tangent;
    protected Vector3 binormal;
    protected Vector3 calcVect = new Vector3();
    protected double clipBias;
    protected ColorRGBA waterColorStart;
    protected ColorRGBA waterColorEnd;
    protected double heightFalloffStart;
    protected double heightFalloffSpeed;
    protected double waterMaxAmplitude;
    protected double speedReflection;
    protected double speedRefraction;

    protected boolean aboveWater;
    protected double normalTranslation = 0.0;
    protected double refractionTranslation = 0.0;
    protected boolean supported = true;
    protected boolean useProjectedShader = false;
    protected boolean useRefraction = false;
    protected boolean useReflection = true;
    protected int renderScale;

    protected String simpleShaderStr = "com/ardor3d/extension/effect/water/flatwatershader";
    protected String simpleShaderRefractionStr = "com/ardor3d/extension/effect/water/flatwatershader_refraction";
    protected String projectedShaderStr = "com/ardor3d/extension/effect/water/projectedwatershader";
    protected String projectedShaderRefractionStr = "com/ardor3d/extension/effect/water/projectedwatershader_refraction";
    protected String currentShaderStr;

    protected String normalMapTextureString = "";
    protected String dudvMapTextureString = "";
    protected String foamMapTextureString = "";
    protected String fallbackMapTextureString = "";

    private boolean initialized;

    /**
     * Resets water parameters to default values
     * 
     */
    public void resetParameters() {
        waterPlane = new Plane(new Vector3(0.0f, 1.0f, 0.0f), 0.0f);
        tangent = new Vector3(1.0f, 0.0f, 0.0f);
        binormal = new Vector3(0.0f, 0.0f, 1.0f);

        waterMaxAmplitude = 1.0f;
        clipBias = 0.0f;
        waterColorStart = new ColorRGBA(0.0f, 0.0f, 0.1f, 1.0f);
        waterColorEnd = new ColorRGBA(0.0f, 0.3f, 0.1f, 1.0f);
        heightFalloffStart = 300.0f;
        heightFalloffSpeed = 500.0f;
        speedReflection = 0.1f;
        speedRefraction = -0.05f;
    }

    /**
     * Release pbuffers in TextureRenderer's. Preferably called from user cleanup method.
     */
    public void cleanup() {
        if (isSupported()) {
            tRenderer.cleanup();
        }
    }

    public boolean isSupported() {
        return supported;
    }

    /**
     * Creates a new WaterRenderPass
     * 
     * @param cam
     *            main rendercam to use for reflection settings etc
     * @param renderScale
     *            how many times smaller the reflection/refraction textures should be compared to the main display
     * @param useProjectedShader
     *            true - use the projected setup for variable height water meshes, false - use the flast shader setup
     * @param useRefraction
     *            enable/disable rendering of refraction textures
     */
    public WaterNode(final Camera cam, final int renderScale, final boolean useProjectedShader,
            final boolean useRefraction) {
        this.cam = cam;
        this.useProjectedShader = useProjectedShader;
        this.useRefraction = useRefraction;
        this.renderScale = renderScale;
        resetParameters();

        waterShader = new GLSLShaderObjectsState();

        cullBackFace = new CullState();
        cullBackFace.setEnabled(true);
        cullBackFace.setCullFace(CullState.Face.None);
        clipState = new ClipState();

    }

    /**
     * Initialize texture renderers. Load water textures. Create shaders.
     * 
     * @param r
     */
    private void initialize(final Renderer r) {
        if (initialized) {
            return;
        }
        initialized = true;

        final ContextCapabilities caps = ContextManager.getCurrentContext().getCapabilities();

        if (useRefraction && useProjectedShader && caps.getNumberOfFragmentTextureUnits() < 6 || useRefraction
                && caps.getNumberOfFragmentTextureUnits() < 5) {
            useRefraction = false;
            logger.info("Not enough textureunits, falling back to non refraction water");
        }

        if (!caps.isGLSLSupported()) {
            supported = false;
        }
        if (!(caps.isPbufferSupported() || caps.isFBOSupported())) {
            supported = false;
        }

        if (isSupported()) {
            final DisplaySettings settings = new DisplaySettings(cam.getWidth() / renderScale, cam.getHeight()
                    / renderScale, 0, 0, 0, 8, 0, 0, false, false);
            tRenderer = TextureRendererFactory.INSTANCE.createTextureRenderer(settings, r, caps,
                    TextureRenderer.Target.Texture2D);

            tRenderer.setMultipleTargets(true);
            tRenderer.setBackgroundColor(new ColorRGBA(0.0f, 0.0f, 0.0f, 1.0f));
            tRenderer.getCamera().setFrustum(cam.getFrustumNear(), cam.getFrustumFar(), cam.getFrustumLeft(),
                    cam.getFrustumRight(), cam.getFrustumTop(), cam.getFrustumBottom());

            textureState = new TextureState();
            textureState.setEnabled(true);

            setupTextures();
        }

        if (!isSupported()) {
            createFallbackData();
        } else {
            noFog = new FogState();
            noFog.setEnabled(false);
        }

        setCullHint(CullHint.Never);

        setWaterEffectOnSpatial(this);
    }

    /**
     * Load water textures.
     */
    protected void setupTextures() {
        textureReflect = new Texture2D();
        textureReflect.setWrap(Texture.WrapMode.EdgeClamp);
        textureReflect.setMagnificationFilter(Texture.MagnificationFilter.Bilinear);

        Matrix4 matrix = new Matrix4();
        matrix.setValue(0, 0, -1.0);
        matrix.setValue(3, 0, 1.0);
        textureReflect.setTextureMatrix(matrix);

        tRenderer.setupTexture(textureReflect);

        normalmapTexture = TextureManager.load(normalMapTextureString, Texture.MinificationFilter.Trilinear,
                Image.Format.Guess, true);
        textureState.setTexture(normalmapTexture, 0);
        normalmapTexture.setWrap(Texture.WrapMode.Repeat);

        textureState.setTexture(textureReflect, 1);

        dudvTexture = TextureManager.load(dudvMapTextureString, Texture.MinificationFilter.Trilinear,
                Image.Format.GuessNoCompression, true);
        matrix = new Matrix4();
        matrix.setValue(0, 0, 0.8);
        matrix.setValue(1, 1, 0.8);
        dudvTexture.setTextureMatrix(matrix);
        textureState.setTexture(dudvTexture, 2);
        dudvTexture.setWrap(Texture.WrapMode.Repeat);

        if (useRefraction) {
            textureRefract = new Texture2D();
            textureRefract.setWrap(Texture.WrapMode.EdgeClamp);
            textureRefract.setMagnificationFilter(Texture.MagnificationFilter.Bilinear);
            tRenderer.setupTexture(textureRefract);

            textureDepth = new Texture2D();
            textureDepth.setWrap(Texture.WrapMode.EdgeClamp);
            textureDepth.setMagnificationFilter(Texture.MagnificationFilter.NearestNeighbor);
            textureDepth.setRenderToTextureType(Texture.RenderToTextureType.Depth);
            tRenderer.setupTexture(textureDepth);

            textureState.setTexture(textureRefract, 3);
            textureState.setTexture(textureDepth, 4);
        }

        if (useProjectedShader) {
            foamTexture = TextureManager.load(foamMapTextureString, Texture.MinificationFilter.Trilinear,
                    Image.Format.Guess, true);
            if (useRefraction) {
                textureState.setTexture(foamTexture, 5);
            } else {
                textureState.setTexture(foamTexture, 3);
            }
            foamTexture.setWrap(Texture.WrapMode.Repeat);
        }

        clipState.setEnabled(true);
        clipState.setEnableClipPlane(ClipState.CLIP_PLANE0, true);

        reloadShader();
    }

    /**
     * Create setup to use as fallback if fancy water is not supported.
     */
    private void createFallbackData() {
        fallbackTextureState = new TextureState();
        fallbackTextureState.setEnabled(true);

        fallbackTexture = TextureManager.load(fallbackMapTextureString, Texture.MinificationFilter.Trilinear,
                Image.Format.Guess, true);
        fallbackTextureState.setTexture(fallbackTexture, 0);
        fallbackTexture.setWrap(Texture.WrapMode.Repeat);

        fallbackTextureStateMatrix = new Matrix4();

        as1 = new BlendState();
        as1.setBlendEnabled(true);
        as1.setTestEnabled(true);
        as1.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
        as1.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
        as1.setEnabled(true);
    }

    public void update(final double tpf) {
        this.tpf = tpf;
    }

    @Override
    public void draw(final Renderer r) {
        initialize(r);

        updateTranslations();

        final double camWaterDist = waterPlane.pseudoDistance(cam.getLocation());
        aboveWater = camWaterDist >= 0;

        if (isSupported()) {
            waterShader.setUniform("tangent", tangent);
            waterShader.setUniform("binormal", binormal);
            waterShader.setUniform("useFadeToFogColor", useFadeToFogColor);
            waterShader.setUniform("waterColor", waterColorStart);
            waterShader.setUniform("waterColorEnd", waterColorEnd);
            waterShader.setUniform("normalTranslation", (float) normalTranslation);
            waterShader.setUniform("refractionTranslation", (float) refractionTranslation);
            waterShader.setUniform("abovewater", aboveWater);
            if (useProjectedShader) {
                waterShader.setUniform("cameraPos", cam.getLocation());
                waterShader.setUniform("waterHeight", (float) waterPlane.getConstant());
                waterShader.setUniform("amplitude", (float) waterMaxAmplitude);
                waterShader.setUniform("heightFalloffStart", (float) heightFalloffStart);
                waterShader.setUniform("heightFalloffSpeed", (float) heightFalloffSpeed);
            }

            final double heightTotal = clipBias + waterMaxAmplitude - waterPlane.getConstant();
            final Vector3 normal = Vector3.fetchTempInstance();
            normal.set(waterPlane.getNormal());
            clipState.setEnabled(true);

            if (useReflection) {
                clipState.setClipPlaneEquation(ClipState.CLIP_PLANE0, normal.getX(), normal.getY(), normal.getZ(),
                        heightTotal);

                renderReflection();
            }

            if (useRefraction && aboveWater) {
                clipState.setClipPlaneEquation(ClipState.CLIP_PLANE0, -normal.getX(), -normal.getY(), -normal.getZ(),
                        -heightTotal);

                renderRefraction();
            }

            clipState.setEnabled(false);
        }

        if (fallbackTextureState != null) {
            fallbackTextureStateMatrix.setValue(3, 1, normalTranslation);
            fallbackTexture.setTextureMatrix(fallbackTextureStateMatrix);
        }

        super.draw(r);
    }

    protected void updateTranslations() {
        normalTranslation += speedReflection * tpf;
        refractionTranslation += speedRefraction * tpf;
    }

    public void reloadShader() {
        if (useProjectedShader) {
            if (useRefraction) {
                currentShaderStr = projectedShaderRefractionStr;
            } else {
                currentShaderStr = projectedShaderStr;
            }
        } else {
            if (useRefraction) {
                currentShaderStr = simpleShaderRefractionStr;
            } else {
                currentShaderStr = simpleShaderStr;
            }
        }

        try {
            System.out.println("loading " + currentShaderStr);
            waterShader.setVertexShader(WaterNode.class.getClassLoader()
                    .getResourceAsStream(currentShaderStr + ".vert"));
            waterShader.setFragmentShader(WaterNode.class.getClassLoader().getResourceAsStream(
                    currentShaderStr + ".frag"));
        } catch (final IOException e) {
            logger.log(Level.WARNING, "Error loading shader", e);
            return;
        }

        waterShader.setUniform("normalMap", 0);
        waterShader.setUniform("reflection", 1);
        waterShader.setUniform("dudvMap", 2);
        if (useRefraction) {
            waterShader.setUniform("refraction", 3);
            waterShader.setUniform("depthMap", 4);
        }
        if (useProjectedShader) {
            if (useRefraction) {
                waterShader.setUniform("foamMap", 5);
            } else {
                waterShader.setUniform("foamMap", 3);
            }
        }

        logger.info("Shader reloaded...");
    }

    /**
     * Sets a spatial up for being rendered with the watereffect
     * 
     * @param spatial
     *            Spatial to use as base for the watereffect
     */
    public void setWaterEffectOnSpatial(final Spatial spatial) {
        spatial.setRenderState(cullBackFace);
        if (isSupported()) {
            // spatial.setRenderBucketType(RenderBucketType.Skip);
            spatial.setRenderState(waterShader);
            spatial.setRenderState(textureState);
        } else {
            spatial.setRenderBucketType(RenderBucketType.Transparent);
            spatial.setLightCombineMode(Spatial.LightCombineMode.Off);
            spatial.setRenderState(fallbackTextureState);
            spatial.setRenderState(as1);
        }
    }

    private void setFallbackEffectOnSpatial(final Spatial spatial) {
        if (fallbackTextureState == null) {
            createFallbackData();
        }

        spatial.setRenderState(cullBackFace);
        spatial.setRenderBucketType(RenderBucketType.Transparent);
        spatial.setLightCombineMode(Spatial.LightCombineMode.Off);
        spatial.setRenderState(fallbackTextureState);
        spatial.setRenderState(as1);
    }

    // temporary vectors for mem opt.
    private final Vector3 tmpLocation = new Vector3();
    private final Vector3 camReflectPos = new Vector3();
    private final Vector3 camReflectDir = new Vector3();
    private final Vector3 camReflectUp = new Vector3();
    private final Vector3 camReflectLeft = new Vector3();
    private final Vector3 camLocation = new Vector3();

    /**
     * Render water reflection RTT
     */
    private void renderReflection() {
        if (renderList == null || renderList.isEmpty()) {
            return;
        }

        reflectionTime += tpf;
        if (reflectionTime < reflectionThrottle) {
            return;
        }
        reflectionTime = 0;

        if (aboveWater) {
            camLocation.set(cam.getLocation());

            double planeDistance = waterPlane.pseudoDistance(camLocation);
            calcVect.set(waterPlane.getNormal()).multiplyLocal(planeDistance * 2.0f);
            camReflectPos.set(camLocation.subtractLocal(calcVect));

            camLocation.set(cam.getLocation()).addLocal(cam.getDirection());
            planeDistance = waterPlane.pseudoDistance(camLocation);
            calcVect.set(waterPlane.getNormal()).multiplyLocal(planeDistance * 2.0f);
            camReflectDir.set(camLocation.subtractLocal(calcVect)).subtractLocal(camReflectPos).normalizeLocal();

            camLocation.set(cam.getLocation()).addLocal(cam.getUp());
            planeDistance = waterPlane.pseudoDistance(camLocation);
            calcVect.set(waterPlane.getNormal()).multiplyLocal(planeDistance * 2.0f);
            camReflectUp.set(camLocation.subtractLocal(calcVect)).subtractLocal(camReflectPos).normalizeLocal();

            camReflectLeft.set(camReflectUp).crossLocal(camReflectDir).normalizeLocal();

            tRenderer.getCamera().setLocation(camReflectPos);
            tRenderer.getCamera().setDirection(camReflectDir);
            tRenderer.getCamera().setUp(camReflectUp);
            tRenderer.getCamera().setLeft(camReflectLeft);
        } else {
            tRenderer.getCamera().setLocation(cam.getLocation());
            tRenderer.getCamera().setDirection(cam.getDirection());
            tRenderer.getCamera().setUp(cam.getUp());
            tRenderer.getCamera().setLeft(cam.getLeft());
        }

        if (skyBox != null) {
            tmpLocation.set(skyBox.getTranslation());
            skyBox.setTranslation(tRenderer.getCamera().getLocation());
            skyBox.updateGeometricState(0.0f);
        }

        texArray.clear();
        texArray.add(textureReflect);

        if (isUseFadeToFogColor()) {
            // TODO: add support for this
            // context.enforceState(noFog);
            // tRenderer.render(renderList, texArray);
            // context.clearEnforcedState(RenderState.StateType.Fog);
        } else {
            tRenderer.render(renderList, texArray, true);
        }

        if (skyBox != null) {
            skyBox.setTranslation(tmpLocation);
            skyBox.updateGeometricState(0.0f);
        }
    }

    /**
     * Render water refraction RTT
     */
    private void renderRefraction() {
        if (renderList.isEmpty()) {
            return;
        }

        refractionTime += tpf;
        if (refractionTime < refractionThrottle) {
            return;
        }
        refractionTime = 0;

        // tRenderer.getCamera().set(cam);
        tRenderer.getCamera().setLocation(cam.getLocation());
        tRenderer.getCamera().setDirection(cam.getDirection());
        tRenderer.getCamera().setUp(cam.getUp());
        tRenderer.getCamera().setLeft(cam.getLeft());

        CullHint cullMode = CullHint.Dynamic;
        if (skyBox != null) {
            cullMode = skyBox.getCullHint();
            skyBox.setCullHint(CullHint.Always);
        }

        texArray.clear();
        texArray.add(textureRefract);
        texArray.add(textureDepth);

        if (isUseFadeToFogColor()) {
            // TODO: add support for this
            // context.enforceState(noFog);
            // tRenderer.render(renderList, texArray, true);
            // context.clearEnforcedState(RenderState.StateType.Fog);
        } else {
            tRenderer.render(renderList, texArray, true);
        }

        if (skyBox != null) {
            skyBox.setCullHint(cullMode);
        }
    }

    public void removeReflectedScene(final Spatial renderNode) {
        if (renderList != null) {
            logger.info("Removed reflected scene: " + renderList.remove(renderNode));
        }
    }

    public void clearReflectedScene() {
        if (renderList != null) {
            renderList.clear();
        }
    }

    /**
     * Sets spatial to be used as reflection in the water(clears previously set)
     * 
     * @param renderNode
     *            Spatial to use as reflection in the water
     */
    public void setReflectedScene(final Spatial renderNode) {
        if (renderList == null) {
            renderList = new ArrayList<Spatial>();
        }
        renderList.clear();
        renderList.add(renderNode);
        renderNode.setRenderState(clipState);
    }

    /**
     * Adds a spatial to the list of spatials used as reflection in the water
     * 
     * @param renderNode
     *            Spatial to add to the list of objects used as reflection in the water
     */
    public void addReflectedScene(final Spatial renderNode) {
        if (renderNode == null) {
            return;
        }

        if (renderList == null) {
            renderList = new ArrayList<Spatial>();
        }
        if (!renderList.contains(renderNode)) {
            renderList.add(renderNode);
            renderNode.setRenderState(clipState);
        }
    }

    /**
     * Sets up a node to be transformed and clipped for skybox usage
     * 
     * @param skyBox
     *            Handle to a node to use as skybox
     */
    public void setSkybox(final Node skyBox) {
        if (skyBox != null) {
            final ClipState skyboxClipState = new ClipState();
            skyboxClipState.setEnabled(false);
            skyBox.setRenderState(skyboxClipState);
        }

        this.skyBox = skyBox;
    }

    public Camera getCam() {
        return cam;
    }

    public void setCam(final Camera cam) {
        this.cam = cam;
    }

    public ColorRGBA getWaterColorStart() {
        return waterColorStart;
    }

    /**
     * Color to use when the incident angle to the surface is low
     */
    public void setWaterColorStart(final ColorRGBA waterColorStart) {
        this.waterColorStart = waterColorStart;
    }

    public ColorRGBA getWaterColorEnd() {
        return waterColorEnd;
    }

    /**
     * Color to use when the incident angle to the surface is high
     */
    public void setWaterColorEnd(final ColorRGBA waterColorEnd) {
        this.waterColorEnd = waterColorEnd;
    }

    public double getHeightFalloffStart() {
        return heightFalloffStart;
    }

    /**
     * Set at what distance the waveheights should start to fade out(for projected water only)
     * 
     * @param heightFalloffStart
     */
    public void setHeightFalloffStart(final double heightFalloffStart) {
        this.heightFalloffStart = heightFalloffStart;
    }

    public double getHeightFalloffSpeed() {
        return heightFalloffSpeed;
    }

    /**
     * Set the fadeout length of the waveheights, when over falloff start(for projected water only)
     * 
     * @param heightFalloffStart
     */
    public void setHeightFalloffSpeed(final double heightFalloffSpeed) {
        this.heightFalloffSpeed = heightFalloffSpeed;
    }

    public double getWaterHeight() {
        return waterPlane.getConstant();
    }

    /**
     * Set base height of the waterplane(Used for reflecting the camera for rendering reflection)
     * 
     * @param waterHeight
     *            Waterplane height
     */
    public void setWaterHeight(final double waterHeight) {
        waterPlane.setConstant(waterHeight);
    }

    public ReadOnlyVector3 getNormal() {
        return waterPlane.getNormal();
    }

    /**
     * Set the normal of the waterplane(Used for reflecting the camera for rendering reflection)
     * 
     * @param normal
     *            Waterplane normal
     */
    public void setNormal(final Vector3 normal) {
        waterPlane.setNormal(normal);
    }

    public double getSpeedReflection() {
        return speedReflection;
    }

    /**
     * Set the movement speed of the reflectiontexture
     * 
     * @param speedReflection
     *            Speed of reflectiontexture
     */
    public void setSpeedReflection(final double speedReflection) {
        this.speedReflection = speedReflection;
    }

    public double getSpeedRefraction() {
        return speedRefraction;
    }

    /**
     * Set the movement speed of the refractiontexture
     * 
     * @param speedRefraction
     *            Speed of refractiontexture
     */
    public void setSpeedRefraction(final double speedRefraction) {
        this.speedRefraction = speedRefraction;
    }

    public double getWaterMaxAmplitude() {
        return waterMaxAmplitude;
    }

    /**
     * Maximum amplitude of the water, used for clipping correctly(projected water only)
     * 
     * @param waterMaxAmplitude
     *            Maximum amplitude
     */
    public void setWaterMaxAmplitude(final double waterMaxAmplitude) {
        this.waterMaxAmplitude = waterMaxAmplitude;
    }

    public double getClipBias() {
        return clipBias;
    }

    public void setClipBias(final double clipBias) {
        this.clipBias = clipBias;
    }

    public Plane getWaterPlane() {
        return waterPlane;
    }

    public void setWaterPlane(final Plane waterPlane) {
        this.waterPlane = waterPlane;
    }

    public Vector3 getTangent() {
        return tangent;
    }

    public void setTangent(final Vector3 tangent) {
        this.tangent = tangent;
    }

    public Vector3 getBinormal() {
        return binormal;
    }

    public void setBinormal(final Vector3 binormal) {
        this.binormal = binormal;
    }

    public Texture getTextureReflect() {
        return textureReflect;
    }

    public Texture getTextureRefract() {
        return textureRefract;
    }

    public Texture getTextureDepth() {
        return textureDepth;
    }

    /**
     * If true, fade to fogcolor. If false, fade to 100% reflective surface
     * 
     * @param value
     */
    public void useFadeToFogColor(final boolean value) {
        useFadeToFogColor = value;
    }

    public boolean isUseFadeToFogColor() {
        return useFadeToFogColor;
    }

    public boolean isUseReflection() {
        return useReflection;
    }

    /**
     * Turn reflection on and off
     * 
     * @param useReflection
     */
    public void setUseReflection(final boolean useReflection) {
        if (useReflection == this.useReflection) {
            return;
        }
        this.useReflection = useReflection;
        reloadShader();
    }

    public boolean isUseRefraction() {
        return useRefraction;
    }

    /**
     * Turn refraction on and off
     * 
     * @param useRefraction
     */
    public void setUseRefraction(final boolean useRefraction) {
        if (useRefraction == this.useRefraction) {
            return;
        }
        this.useRefraction = useRefraction;
        reloadShader();
    }

    public int getRenderScale() {
        return renderScale;
    }

    public void setRenderScale(final int renderScale) {
        this.renderScale = renderScale;
    }

    public boolean isUseProjectedShader() {
        return useProjectedShader;
    }

    public void setUseProjectedShader(final boolean useProjectedShader) {
        if (useProjectedShader == this.useProjectedShader) {
            return;
        }
        this.useProjectedShader = useProjectedShader;
        reloadShader();
    }

    public double getReflectionThrottle() {
        return reflectionThrottle;
    }

    public void setReflectionThrottle(final double reflectionThrottle) {
        this.reflectionThrottle = reflectionThrottle;
    }

    public double getRefractionThrottle() {
        return refractionThrottle;
    }

    public void setRefractionThrottle(final double refractionThrottle) {
        this.refractionThrottle = refractionThrottle;
    }

    public TextureState getTextureState() {
        return textureState;
    }

    public void setTextureState(final TextureState textureState) {
        this.textureState = textureState;
    }

    public void updateCamera() {
        if (isSupported()) {
            tRenderer.getCamera().setFrustum(cam.getFrustumNear(), cam.getFrustumFar(), cam.getFrustumLeft(),
                    cam.getFrustumRight(), cam.getFrustumTop(), cam.getFrustumBottom());
        }
    }

    public void setFallbackTexture(final Texture fallbackTexture) {
        this.fallbackTexture = fallbackTexture;
    }

    public Texture getFallbackTexture() {
        return fallbackTexture;
    }

    public void setNormalmapTexture(final Texture normalmapTexture) {
        this.normalmapTexture = normalmapTexture;
    }

    public Texture getNormalmapTexture() {
        return normalmapTexture;
    }

    public void setDudvTexture(final Texture dudvTexture) {
        this.dudvTexture = dudvTexture;
    }

    public Texture getDudvTexture() {
        return dudvTexture;
    }

    public void setFoamTexture(final Texture foamTexture) {
        this.foamTexture = foamTexture;
    }

    public Texture getFoamTexture() {
        return foamTexture;
    }

    /**
     * @return the normalMapTextureString
     */
    public String getNormalMapTextureString() {
        return normalMapTextureString;
    }

    /**
     * @param normalMapTextureString
     *            the normalMapTextureString to set
     */
    public void setNormalMapTextureString(final String normalMapTextureString) {
        this.normalMapTextureString = normalMapTextureString;
    }

    /**
     * @return the dudvMapTextureString
     */
    public String getDudvMapTextureString() {
        return dudvMapTextureString;
    }

    /**
     * @param dudvMapTextureString
     *            the dudvMapTextureString to set
     */
    public void setDudvMapTextureString(final String dudvMapTextureString) {
        this.dudvMapTextureString = dudvMapTextureString;
    }

    /**
     * @return the foamMapTextureString
     */
    public String getFoamMapTextureString() {
        return foamMapTextureString;
    }

    /**
     * @param foamMapTextureString
     *            the foamMapTextureString to set
     */
    public void setFoamMapTextureString(final String foamMapTextureString) {
        this.foamMapTextureString = foamMapTextureString;
    }

    /**
     * @return the fallbackMapTextureString
     */
    public String getFallbackMapTextureString() {
        return fallbackMapTextureString;
    }

    /**
     * @param fallbackMapTextureString
     *            the fallbackMapTextureString to set
     */
    public void setFallbackMapTextureString(final String fallbackMapTextureString) {
        this.fallbackMapTextureString = fallbackMapTextureString;
    }
}
