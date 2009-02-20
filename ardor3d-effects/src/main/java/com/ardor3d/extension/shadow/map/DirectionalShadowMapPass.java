/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.shadow.map;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.Texture.DepthTextureCompareFunc;
import com.ardor3d.image.Texture.DepthTextureCompareMode;
import com.ardor3d.image.Texture.DepthTextureMode;
import com.ardor3d.light.DirectionalLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyMatrix4;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.TextureRenderer;
import com.ardor3d.renderer.TextureRendererFactory;
import com.ardor3d.renderer.pass.Pass;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.ClipState;
import com.ardor3d.renderer.state.ColorMaskState;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.GLSLShaderObjectsState;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.OffsetState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.OffsetState.OffsetType;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.Spatial.CullHint;

/**
 * A pass providing a shadow mapping layer across the top of an existing scene.
 */
public class DirectionalShadowMapPass extends Pass {
    private static final Logger logger = Logger.getLogger(DirectionalShadowMapPass.class.getName());

    private static final long serialVersionUID = 1L;

    /** Bias matrix borrowed from the projected texture utility */
    private static Matrix4 biasMatrix = new Matrix4(0.5f, 0.0f, 0.0f, 0.0f, 0.0f, 0.5f, 0.0f, 0.0f, 0.0f, 0.0f, 0.5f,
            0.0f, 0.5f, 0.5f, 0.5f, 1.0f); // bias from [-1, 1] to [0, 1]

    /** The renderer used to produce the shadow map */
    private TextureRenderer _shadowMapRenderer;
    /** The texture storing the shadow map */
    private Texture2D _shadowMapTexture;
    /** The near plane when rendering the shadow map */
    private final float _nearPlane = 1f;
    /**
     * The far plane when rendering the shadow map - currently tuned for the test
     */
    private final float _farPlane = 3000.0f;
    /**
     * The location the shadow light source is looking at - must point at the focus of the scene
     */
    private Vector3 _shadowCameraLookAt;
    /**
     * The effective location of the light source - derived based on the distance of casting, look at and direction
     */
    private Vector3 _shadowCameraLocation;

    /** The list of occluding nodes */
    private final List<Spatial> _occluderNodes = new ArrayList<Spatial>();

    /** Culling front faces when rendering shadow maps */
    private CullState _cullFrontFace;
    /** Turn off textures when rendering shadow maps */
    private TextureState _noTexture;
    /** Turn off colors when rendering shadow maps - depth only */
    private ColorMaskState _colorDisabled;
    /** Turn off lighting when rendering shadow maps - depth only */
    private LightState _noLights;

    /**
     * The blending to both discard the fragments that have been determined to be free of shadows and to blend into the
     * background scene
     */
    private BlendState _discardShadowFragments;
    /** The state applying the depth offset for the scene */
    private OffsetState _sceneOffsetState;
    /** The state applying the depth offset for the shadow */
    private OffsetState _shadowOffsetState;
    /** The state applying the shadow map */
    private TextureState _shadowTextureState;
    /** The bright light used to blend the shadows version into the scene */
    private LightState _brightLights;
    /** The dark material used to blend the shadows into the scene */
    private MaterialState _darkMaterial;
    /** Don't perform any plane clipping when rendering the shadowed scene */
    private ClipState _noClip;

    /** True once the pass has been initialized */
    protected boolean _initialised = false;
    /** The direction shadows are being cast from - directional light? */
    protected Vector3 _direction;
    /** The size of the shadow map texture */
    private final int _shadowMapSize;
    /**
     * The scaling applied to the shadow map when rendered to - lower number means higher res but less ara covered by
     * the shadow map
     */
    protected float _shadowMapScale = 0.4f;
    /**
     * The distance we're modeling the direction light source as being away from the focal point, again the higher the
     * number the more of the scene is covered but at lower resolution
     */
    protected float _distance = 500;

    /** The color of shadows cast */
    private final ColorRGBA _shadowCol = new ColorRGBA(0, 0, 0, 0.3f);
    /** The optional shader for smoothing */
    private GLSLShaderObjectsState _shader;

    /** True if we should cull occluders - if not we'll draw all occluders */
    private boolean _cullOccluders = true;
    /** Node providing easy rendering of all occluders added to the pass */
    private final OccludersRenderNode _occludersRenderNode = new OccludersRenderNode();

    /**
     * Create a shadow map pass casting shadows from a light with the direction given.
     * 
     * @param direction
     *            The direction of the light casting the shadows
     */
    public DirectionalShadowMapPass(final Vector3 direction) {
        this(direction, 2048);
    }

    /**
     * Create a shadow map pass casting shadows from a light with the direction given.
     * 
     * @param shadowMapSize
     *            The size of the shadow map texture
     * @param direction
     *            The direction of the light casting the shadows
     */
    public DirectionalShadowMapPass(final Vector3 direction, final int shadowMapSize) {
        _shadowMapSize = shadowMapSize;
        _direction = direction;

        setViewTarget(new Vector3(0, 0, 0));
    }

    /**
     * Set the colour of the shadows to be cast
     * 
     * @param col
     *            The colour of the shadows to be cast
     */
    public void setShadowAlpha(final float alpha) {
        _shadowCol.setAlpha(alpha);
        if (_darkMaterial != null) {
            _darkMaterial.setDiffuse(_shadowCol);
        }
    }

    /**
     * Set the distance of the camera representing the directional light. The further away the more of the scene will be
     * shadowed but at a lower resolution
     * 
     * @param dis
     *            The distance to be used for the shadow map camera (default = 500)
     */
    public void setViewDistance(final float dis) {
        _distance = dis;
    }

    /**
     * Indicate whether occluders should be culled against shadow map frustrum
     * 
     * @param cullOccluders
     *            True if occluders should be culled against shadow map frustrum
     */
    public void setCullOccluders(final boolean cullOccluders) {
        _cullOccluders = cullOccluders;
    }

    /**
     * Set the scale factor thats used to stretch the shadow map texture across the scene.
     * 
     * Higher the number the more of the scene will be convered but at a lower resolution.
     * 
     * @param scale
     *            The scale used to stretch the shadow map across the scene.
     */
    public void setShadowMapScale(final float scale) {
        _shadowMapScale = scale;
    }

    /**
     * Set the target of the view. This will be where the camera points when generating the shadow map and should be the
     * centre of the scene
     * 
     * @param target
     *            The target of the view
     */
    public void setViewTarget(final Vector3 target) {
        if (target.equals(_shadowCameraLookAt)) {
            return;
        }

        _shadowCameraLookAt = new Vector3().set(target);
        final Vector3 temp = new Vector3().set(_direction);
        temp.normalizeLocal();
        temp.multiplyLocal(-_distance);
        _shadowCameraLocation = new Vector3().set(target);
        _shadowCameraLocation.addLocal(temp);

        if (_shadowMapRenderer != null) {
            updateShadowCamera();
        }
    }

    /**
     * Add a spatial that will occlude light and hence cast a shadow
     * 
     * @param occluder
     *            The spatial to add as an occluder
     */
    public void addOccluder(final Spatial occluder) {
        _occluderNodes.add(occluder);
    }

    /**
     * Initialise the pass render states
     * 
     * @param r
     */
    public void init(final Renderer r) {
        if (_initialised) {
            return;
        }

        _initialised = true; // now it's initialized

        final ContextCapabilities caps = ContextManager.getCurrentContext().getCapabilities();

        // the texture that the shadow map will be rendered into. Modulated so
        // that it can be blended over the scene.
        _shadowMapTexture = new Texture2D();
        _shadowMapTexture.setApply(Texture.ApplyMode.Modulate);
        _shadowMapTexture.setMinificationFilter(Texture.MinificationFilter.NearestNeighborNoMipMaps);
        _shadowMapTexture.setWrap(Texture.WrapMode.Clamp);
        _shadowMapTexture.setMagnificationFilter(Texture.MagnificationFilter.Bilinear);
        _shadowMapTexture.setRenderToTextureType(Texture.RenderToTextureType.Depth);
        _shadowMapTexture.setEnvironmentalMapMode(Texture.EnvironmentalMapMode.EyeLinear);

        _shadowMapTexture.setDepthCompareMode(DepthTextureCompareMode.RtoTexture);
        _shadowMapTexture.setDepthCompareFunc(DepthTextureCompareFunc.GreaterThanEqual);
        _shadowMapTexture.setDepthMode(DepthTextureMode.Intensity);

        // configure the texture renderer to output to the texture
        final DisplaySettings settings = new DisplaySettings(_shadowMapSize, _shadowMapSize, 0, 0, 0, 8, 0, 0, false,
                false);
        _shadowMapRenderer = TextureRendererFactory.INSTANCE.createTextureRenderer(settings, r, caps,
                TextureRenderer.Target.Texture2D);
        _shadowMapRenderer.setupTexture(_shadowMapTexture);

        // render state to apply the shadow map texture
        _shadowTextureState = new TextureState();
        _shadowTextureState.setTexture(_shadowMapTexture, 0);

        _sceneOffsetState = new OffsetState();
        _sceneOffsetState.setTypeEnabled(OffsetType.Fill, true);
        _sceneOffsetState.setUnits(-5);

        _shadowOffsetState = new OffsetState();
        _shadowOffsetState.setTypeEnabled(OffsetType.Fill, true);
        _shadowOffsetState.setUnits(5);

        _noClip = new ClipState();
        _noClip.setEnabled(false);

        // render states to use when rendering into the shadow map, no textures or colors are required since we're only
        // interested in recording depth. Also only need back faces when rendering the shadow maps
        _noTexture = new TextureState();
        _noTexture.setEnabled(false);
        _colorDisabled = new ColorMaskState();
        _colorDisabled.setAll(false);
        _cullFrontFace = new CullState();
        _cullFrontFace.setEnabled(true);
        _cullFrontFace.setCullFace(CullState.Face.Front);
        _noLights = new LightState();
        _noLights.setEnabled(false);

        // Then rendering and comparing the shadow map with the current
        // depth the result will be set to alpha 1 if not in shadow and
        // to 0 if it's is in shadow. However, we're going to blend it into the
        // scene
        // so the alpha will be zero if there is no shadow at this location but
        // > 0 on shadows.
        _discardShadowFragments = new BlendState();
        _discardShadowFragments.setEnabled(true);
        _discardShadowFragments.setBlendEnabled(true);
        _discardShadowFragments.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
        _discardShadowFragments.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);

        // light used to uniformly light the scene when rendering the shadows
        // themselfs
        // this is so the geometry colour can be used as the source for blending
        // - i.e.
        // transparent shadows rather than matte black
        _brightLights = new LightState();
        _brightLights.setEnabled(true);
        final DirectionalLight light = new DirectionalLight();
        light.setDiffuse(new ColorRGBA(1, 1, 1, 1f));
        light.setEnabled(true);
        _brightLights.attach(light);

        _darkMaterial = new MaterialState();
        _darkMaterial.setEnabled(true);
        _darkMaterial.setDiffuse(_shadowCol);
        _darkMaterial.setAmbient(new ColorRGBA(0, 0, 0, 0f));
        _darkMaterial.setShininess(0);
        _darkMaterial.setSpecular(new ColorRGBA(0, 0, 0, 0));
        _darkMaterial.setEmissive(new ColorRGBA(0, 0, 0, 0));
        _darkMaterial.setMaterialFace(MaterialState.MaterialFace.Front);

        if (caps.isGLSLSupported()) {
            _shader = new GLSLShaderObjectsState();
            try {
                _shader.setVertexShader(getResource("shadowMap.vert"));
                _shader.setFragmentShader(prefixStream("const float OFFSET = 0.5 / " + _shadowMapSize + ".0;",
                        getResource("shadowMap.frag")));
            } catch (final IOException ex) {
                logger.logp(Level.SEVERE, getClass().getName(), "init(Renderer)", "Could not load shaders.", ex);
            }
            _shader.setUniform("shadowMap", 0);
            _shader.setUniform("offset", 0.0002f);
            _shader.setEnabled(true);
        }

        updateShadowCamera();

        // Setup the states that will be enforced whenever we use the texture renderer.
        _shadowMapRenderer.enforceState(_noClip);
        _shadowMapRenderer.enforceState(_noTexture);
        _shadowMapRenderer.enforceState(_colorDisabled);
        _shadowMapRenderer.enforceState(_cullFrontFace);
        _shadowMapRenderer.enforceState(_noLights);
        _shadowMapRenderer.enforceState(_shadowOffsetState);
    }

    public GLSLShaderObjectsState getShader() {
        return _shader;
    }

    private InputStream prefixStream(final String text, final InputStream in) {
        try {
            final ByteArrayOutputStream bout = new ByteArrayOutputStream();

            final DataInputStream dataStream = new DataInputStream(in);
            final byte shaderCode[] = new byte[in.available()];
            dataStream.readFully(shaderCode);
            in.close();
            dataStream.close();

            bout.write(text.getBytes());
            bout.write(shaderCode);
            bout.close();

            return new ByteArrayInputStream(bout.toByteArray());
        } catch (final IOException e) {
            throw new RuntimeException("Failed to load shadow map shader:", e);
        }
    }

    private InputStream getResource(final String ref) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "com/ardor3d/extension/shadow/map/" + ref);
    }

    /**
     * @see com.ardor3d.renderer.pass.Pass#doRender(com.ardor3d.renderer.Renderer)
     */
    @Override
    public void doRender(final Renderer r) {
        if (_occluderNodes.size() == 0) {
            return;
        }

        init(r);
        updateShadowMap(r);
        renderShadowedScene(r);
    }

    /**
     * Render the scene with shadows
     * 
     * @param r
     *            The renderer to use
     */
    protected void renderShadowedScene(final Renderer r) {
        _context.pushEnforcedStates();
        _context.enforceState(_shadowTextureState);
        _context.enforceState(_discardShadowFragments);
        _context.enforceState(_sceneOffsetState);

        if (_context.getCapabilities().isGLSLSupported()) {
            final ReadOnlyMatrix4 view = Camera.getCurrentCamera().getModelViewMatrix();
            final Matrix4 temp = Matrix4.fetchTempInstance();
            _shader.setUniform("inverseView", view.invert(temp), false);
            Matrix4.releaseTempInstance(temp);
            _context.enforceState(_shader);
        } else {
            _context.enforceState(_brightLights);
            _context.enforceState(_darkMaterial);
        }

        // draw the scene, only the shadowed bits will be drawn and blended
        // with the shadow coloured geometry
        for (final Spatial spat : _spatials) {
            spat.onDraw(r);
        }
        r.renderBuckets();

        _context.popEnforcedStates();
    }

    /**
     * Update the shadow map
     * 
     * @param r
     *            The renderer to being use to display this map
     */
    protected void updateShadowMap(final Renderer r) {
        CullHint cullModeBefore = CullHint.Never;

        if (!_cullOccluders) {
            cullModeBefore = _occludersRenderNode.getCullHint();
            _occluderNodes.get(0).setCullHint(CullHint.Never);
        }

        _shadowMapRenderer.render(_occludersRenderNode, _shadowMapTexture, true);

        if (!_cullOccluders) {
            _occludersRenderNode.setCullHint(cullModeBefore);
        }
    }

    /**
     * Update the direction from which the shadows are cast
     */
    protected void updateShadowCamera() {
        // render the shadow map, use the texture renderer to render anything
        // thats been added as occluder
        final float scale = _shadowMapSize * _shadowMapScale;

        _shadowMapRenderer.getCamera().setLocation(_shadowCameraLocation);
        _shadowMapRenderer.getCamera().setFrustum(_nearPlane, _farPlane, -scale, scale, -scale, scale);
        _shadowMapRenderer.getCamera().lookAt(_shadowCameraLookAt, Vector3.UNIT_Y);
        _shadowMapRenderer.getCamera().setParallelProjection(true);
        _shadowMapRenderer.getCamera().update();

        final Matrix4 proj = new Matrix4();
        final Matrix4 view = new Matrix4();
        proj.set(_shadowMapRenderer.getCamera().getProjectionMatrix());
        view.set(_shadowMapRenderer.getCamera().getModelViewMatrix());

        final Matrix4 transform = Matrix4.fetchTempInstance().set(view.multiplyLocal(proj).multiplyLocal(biasMatrix))
                .transposeLocal();
        _shadowMapTexture.setTextureMatrix(transform);
        Matrix4.releaseTempInstance(transform);
    }

    /**
     * @see com.ardor3d.renderer.pass.Pass#cleanUp()
     */
    @Override
    public void cleanUp() {
        super.cleanUp();

        if (_shadowMapRenderer != null) {
            _shadowMapRenderer.cleanup();
        }
    }

    /**
     * Remove the contents of the pass
     */
    public void clear() {
        _occluderNodes.clear();
        _spatials.clear();
    }

    /**
     * Helper class to get all spatials rendered in one TextureRenderer.render() call.
     */
    private class OccludersRenderNode extends Node {
        private static final long serialVersionUID = 7367501683137581101L;

        @Override
        public void draw(final Renderer r) {
            Spatial child;
            for (int i = 0, cSize = _occluderNodes.size(); i < cSize; i++) {
                child = _occluderNodes.get(i);
                if (child != null) {
                    child.onDraw(r);
                }
            }
        }

        @Override
        public void onDraw(final Renderer r) {
            draw(r);
        }
    }
}