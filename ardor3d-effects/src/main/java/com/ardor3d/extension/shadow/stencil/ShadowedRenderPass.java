/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.shadow.stencil;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import com.ardor3d.light.Light;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.pass.Pass;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.ColorMaskState;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.renderer.state.OffsetState;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.StencilState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.renderer.state.OffsetState.OffsetType;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.StencilState.StencilFunction;
import com.ardor3d.renderer.state.StencilState.StencilOperation;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.shape.Quad;

/**
 * <code>ShadowedRenderPass</code> is a render pass that renders the added spatials along with shadows cast by givens
 * occluders and lights flagged as casting shadows.
 */
public class ShadowedRenderPass extends Pass {

    private static final long serialVersionUID = 1L;

    public enum LightingMethod {
        /**
         * value for lightingMethod indicating that a scene should be rendered first with ambient lighting and then
         * multiple passes per light done to illuminate unshadowed areas (resulting in shadows.) More costly but more
         * accurate than Modulative.
         */
        Additive,

        /**
         * value for lightingMethod indicating that a scene should be rendered first with full lighting and then
         * multiple screens applied per light to darken shadowed areas. More prone to artifacts than Additive, but
         * faster.
         */
        Modulative;
    }

    /** list of occluders registered with this pass. */
    protected List<Spatial> _occluders = new ArrayList<Spatial>();

    /** node used to gather and hold shadow volumes for rendering. */
    protected Node _volumeNode = new Node("Volumes");

    /** whether or not the renderstates for this pass have been init'd yet. */
    protected boolean _initialised = false;

    /**
     * A quad to use with MODULATIVE lightMethod for full screen darkening against the shadow stencil.
     */
    protected Quad _shadowQuad = new Quad("RenderForeground", 10, 10);

    /**
     * Used with MODULATIVE lightMethod. Defines the base color of the shadow - the alpha value is replaced with 1 - the
     * alpha of the light's alpha.
     */
    protected ColorRGBA _shadowColor = new ColorRGBA(.2f, .2f, .2f, .1f);

    /** Whether shadow volumes are visible */
    protected boolean _renderVolume = false;

    /** Whether to render shadows (true) or act like a normal RenderPass (false) */
    protected boolean _renderShadows = true;

    /** Sets the type of pass to do to show shadows - ADDITIVE or MODULATIVE */
    protected LightingMethod _lightingMethod = LightingMethod.Additive;

    /** collection of Mesh to MeshShadows mappings */
    protected IdentityHashMap<Mesh, MeshShadows> _meshes = new IdentityHashMap<Mesh, MeshShadows>();

    /**
     * list of occluders that will be casting shadows in this pass. If no occluders set, pass acts like normal
     * RenderPass.
     */
    protected List<Mesh> _occluderMeshes = new ArrayList<Mesh>();

    /**
     * list of lights that will be used to calculate shadows in this pass. Constructed dynamically by searching through
     * the scene for lights with shadowCaster set to true.
     */
    protected List<Light> _shadowLights = new ArrayList<Light>();

    protected int _quadWidth = -1, _quadHeight = -1;

    private ShadowGate _shadowGate = new DefaultShadowGate();

    public static boolean _rTexture = true;

    protected ZBufferState _zbufferWriteLE;
    protected ZBufferState _zbufferAlways;
    protected ZBufferState _forTesting;
    protected ZBufferState _forColorPassTesting;

    protected StencilState _noStencil;
    protected StencilState _stencilFrontFaces;
    protected StencilState _stencilBothFaces;
    protected StencilState _stencilBackFaces;
    protected StencilState _stencilDrawOnlyWhenSet;
    protected StencilState _stencilDrawWhenNotSet;

    protected CullState _cullFrontFace;
    protected CullState _cullBackFace;
    protected CullState _noCull;

    protected TextureState _noTexture;

    protected LightState _lights;
    protected LightState _noLights;

    protected OffsetState _additiveOffset;

    protected BlendState _blended;
    protected BlendState _alphaBlended;
    protected BlendState _modblended;
    protected BlendState _blendTex;

    protected ColorMaskState _colorEnabled;
    protected ColorMaskState _colorDisabled;

    public ShadowedRenderPass() {

        _zbufferWriteLE = new ZBufferState();
        _zbufferWriteLE.setWritable(true);
        _zbufferWriteLE.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
        _zbufferWriteLE.setEnabled(true);

        _zbufferAlways = new ZBufferState();
        _zbufferAlways.setEnabled(false);
        _zbufferAlways.setWritable(false);

        _forTesting = new ZBufferState();
        _forTesting.setWritable(false);
        _forTesting.setFunction(ZBufferState.TestFunction.LessThan);
        _forTesting.setEnabled(true);

        _forColorPassTesting = new ZBufferState();
        _forColorPassTesting.setWritable(false);
        _forColorPassTesting.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
        _forColorPassTesting.setEnabled(true);

        _noStencil = new StencilState();
        _noStencil.setEnabled(false);

        _stencilBothFaces = new StencilState();
        _stencilBothFaces.setEnabled(true);
        _stencilBothFaces.setUseTwoSided(true);
        _stencilBothFaces.setStencilMaskFront(~0);
        _stencilBothFaces.setStencilFunctionFront(StencilFunction.Always);
        _stencilBothFaces.setStencilOpFailFront(StencilOperation.Keep);
        _stencilBothFaces.setStencilOpZFailFront(StencilOperation.Keep);
        _stencilBothFaces.setStencilOpZPassFront(StencilOperation.IncrementWrap);
        _stencilBothFaces.setStencilMaskBack(~0);
        _stencilBothFaces.setStencilFunctionBack(StencilFunction.Always);
        _stencilBothFaces.setStencilOpFailBack(StencilOperation.Keep);
        _stencilBothFaces.setStencilOpZFailBack(StencilOperation.Keep);
        _stencilBothFaces.setStencilOpZPassBack(StencilOperation.DecrementWrap);

        _stencilFrontFaces = new StencilState();
        _stencilFrontFaces.setEnabled(true);
        _stencilFrontFaces.setStencilMask(~0);
        _stencilFrontFaces.setStencilFunction(StencilFunction.Always);
        _stencilFrontFaces.setStencilOpFail(StencilOperation.Keep);
        _stencilFrontFaces.setStencilOpZFail(StencilOperation.Keep);
        _stencilFrontFaces.setStencilOpZPass(StencilOperation.IncrementWrap);

        _stencilBackFaces = new StencilState();
        _stencilBackFaces.setEnabled(true);
        _stencilBackFaces.setStencilMask(~0);
        _stencilBackFaces.setStencilFunction(StencilFunction.Always);
        _stencilBackFaces.setStencilOpFail(StencilOperation.Keep);
        _stencilBackFaces.setStencilOpZFail(StencilOperation.Keep);
        _stencilBackFaces.setStencilOpZPass(StencilOperation.DecrementWrap);

        _stencilDrawOnlyWhenSet = new StencilState();
        _stencilDrawOnlyWhenSet.setEnabled(true);
        _stencilDrawOnlyWhenSet.setStencilMask(~0);
        _stencilDrawOnlyWhenSet.setStencilFunction(StencilFunction.NotEqualTo);
        _stencilDrawOnlyWhenSet.setStencilOpFail(StencilOperation.Keep);
        _stencilDrawOnlyWhenSet.setStencilOpZFail(StencilOperation.Keep);
        _stencilDrawOnlyWhenSet.setStencilOpZPass(StencilOperation.Keep);
        _stencilDrawOnlyWhenSet.setStencilReference(0);

        _stencilDrawWhenNotSet = new StencilState();
        _stencilDrawWhenNotSet.setEnabled(true);
        _stencilDrawWhenNotSet.setStencilMask(~0);
        _stencilDrawWhenNotSet.setStencilFunction(StencilFunction.EqualTo);
        _stencilDrawWhenNotSet.setStencilOpFail(StencilOperation.Keep);
        _stencilDrawWhenNotSet.setStencilOpZFail(StencilOperation.Keep);
        _stencilDrawWhenNotSet.setStencilOpZPass(StencilOperation.Keep);
        _stencilDrawWhenNotSet.setStencilReference(0);

        _cullFrontFace = new CullState();
        _cullFrontFace.setEnabled(true);
        _cullFrontFace.setCullFace(CullState.Face.Front);

        _noCull = new CullState();
        _noCull.setEnabled(false);

        _noLights = new LightState();
        _noLights.setEnabled(false);

        _cullBackFace = new CullState();
        _cullBackFace.setEnabled(true);
        _cullBackFace.setCullFace(CullState.Face.Back);

        _blended = new BlendState();
        _blended.setEnabled(true);
        _blended.setBlendEnabled(true);
        _blended.setDestinationFunction(BlendState.DestinationFunction.One);
        _blended.setSourceFunction(BlendState.SourceFunction.DestinationColor);

        _alphaBlended = new BlendState();
        _alphaBlended.setEnabled(true);
        _alphaBlended.setBlendEnabled(true);
        _alphaBlended.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
        _alphaBlended.setSourceFunction(BlendState.SourceFunction.One);

        _modblended = new BlendState();
        _modblended.setEnabled(true);
        _modblended.setBlendEnabled(true);
        _modblended.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
        _modblended.setSourceFunction(BlendState.SourceFunction.DestinationColor);

        _blendTex = new BlendState();
        _blendTex.setEnabled(true);
        _blendTex.setBlendEnabled(true);
        _blendTex.setDestinationFunction(BlendState.DestinationFunction.Zero);
        _blendTex.setSourceFunction(BlendState.SourceFunction.DestinationColor);

        _colorEnabled = new ColorMaskState();
        _colorEnabled.setAll(true);

        _colorDisabled = new ColorMaskState();
        _colorDisabled.setAll(false);

        _additiveOffset = new OffsetState();
        _additiveOffset.setTypeEnabled(OffsetType.Fill, true);
        _additiveOffset.setTypeEnabled(OffsetType.Line, true);
        _additiveOffset.setTypeEnabled(OffsetType.Point, true);
        _additiveOffset.setUnits(-5);

        _volumeNode.setRenderBucketType(RenderBucketType.Skip);
        _volumeNode.attachChild(new Node());

        _noTexture = new TextureState();
        _noTexture.setEnabled(false);

        _lights = new LightState();
        _lights.setEnabled(true);
        _lights.setLightMask(LightState.MASK_AMBIENT | LightState.MASK_GLOBALAMBIENT);
    }

    /**
     * <code>addOccluder</code> adds an occluder to this pass.
     * 
     * @param toAdd
     *            Occluder Spatial to add to this pass.
     */
    public void addOccluder(final Spatial toAdd) {
        _occluders.add(toAdd);
    }

    /**
     * <code>clearOccluders</code> removes all occluders from this pass.
     */
    public void clearOccluders() {
        _occluders.clear();
    }

    /**
     * <code>containsOccluder</code>
     * 
     * @param s
     * @return
     */
    public boolean containsOccluder(final Spatial s) {
        return _occluders.contains(s);
    }

    /**
     * <code>removeOccluder</code>
     * 
     * @param toRemove
     *            the Occluder Spatial to remove from this pass.
     * @return true if the Spatial was found and removed.
     */
    public boolean removeOccluder(final Spatial toRemove) {
        return _occluders.remove(toRemove);
    }

    /**
     * @return the number of occluders registered with this pass
     */
    public int occludersSize() {
        return _occluders.size();
    }

    /**
     * @return Returns whether shadow volumes will be rendered to the display.
     */
    public boolean getRenderVolume() {
        return _renderVolume;
    }

    /**
     * @param renderVolume
     *            sets whether shadow volumes will be rendered to the display
     */
    public void setRenderVolume(final boolean renderVolume) {
        _renderVolume = renderVolume;
    }

    /**
     * @return whether shadow volumes will be rendered to the display.
     */
    public boolean getRenderShadows() {
        return _renderShadows;
    }

    /**
     * @param renderShadows
     *            whether shadows will be rendered by this pass.
     */
    public void setRenderShadows(final boolean renderShadows) {
        _renderShadows = renderShadows;
    }

    /**
     * @return the shadowColor used by MODULATIVE lightMethod.
     */
    public ColorRGBA getShadowColor() {
        return _shadowColor;
    }

    /**
     * @param shadowColor
     *            the shadowColor used by MODULATIVE lightMethod.
     */
    public void setShadowColor(final ColorRGBA shadowColor) {
        if (shadowColor == null) {
            throw new IllegalArgumentException("shadowColor must not be null!");
        }
        _shadowColor = shadowColor;
    }

    /**
     * @return the lightingMethod currently in use.
     */
    public LightingMethod getLightingMethod() {
        return _lightingMethod;
    }

    /**
     * Sets which method to use with the shadow volume stencils in order to generate shadows in the scene. See javadoc
     * descriptions in the enum LightingMethod for more info.
     * 
     * @param method
     *            method to use
     * @throws IllegalArgumentException
     *             if method is null
     */
    public void setLightingMethod(final LightingMethod method) {
        if (method == null) {
            throw new IllegalArgumentException("method can not be null.");
        }
        _lightingMethod = method;
    }

    /**
     * <code>doRender</code> renders this pass to the framebuffer
     * 
     * @param r
     *            Renderer to use for drawing.
     * @see com.ardor3d.renderer.pass.Pass#doRender(com.ardor3d.renderer.Renderer)
     */
    @Override
    public void doRender(final Renderer r) {
        // init states
        init(_context.getCurrentCamera());

        if (!_renderShadows) {
            renderScene(r);
            if (_renderVolume) {
                getShadowLights();
                setupOccluderMeshes();
                generateVolumes();
                drawVolumes(r);
            }
            return;
        }

        // grab the shadowcasting lights
        getShadowLights();

        // grab the occluders
        setupOccluderMeshes();

        // if no occluders or no shadow casting lights, just render the scene normally and return.
        if (_occluderMeshes.size() == 0 || _shadowLights.size() == 0) {
            // render normal
            renderScene(r);
            cleanup();
            return;
        }

        // otherwise render an ambient pass by masking the diffuse and specular of shadowcasting lights.
        if (_lightingMethod == LightingMethod.Additive) {
            maskShadowLights(LightState.MASK_DIFFUSE | LightState.MASK_SPECULAR);
            _context.pushEnforcedStates();
            _context.enforceState(_noTexture);
            renderScene(r);
            _context.popEnforcedStates();
            unmaskShadowLights();
        } else {
            renderScene(r);
        }

        generateVolumes();

        for (int l = _shadowLights.size(); --l >= 0;) {
            final Light light = _shadowLights.get(l);
            light.setEnabled(false);
        }
        for (int l = _shadowLights.size(); --l >= 0;) {
            final Light light = _shadowLights.get(l);
            // Clear out the stencil buffer
            r.clearStencilBuffer();
            light.setEnabled(true);

            _context.pushEnforcedStates();
            _context.enforceState(_noTexture);
            _context.enforceState(_forTesting);
            _context.enforceState(_colorDisabled);

            if (_context.getCapabilities().isTwoSidedStencilSupported()) {
                _context.enforceState(_noCull);
                _context.enforceState(_stencilBothFaces);
                _volumeNode.getChildren().clear();
                addShadowVolumes(light);
                _volumeNode.updateGeometricState(0);
                _volumeNode.onDraw(r);
            } else {
                _context.enforceState(_stencilFrontFaces);
                _context.enforceState(_cullBackFace);

                _volumeNode.getChildren().clear();
                addShadowVolumes(light);
                _volumeNode.updateGeometricState(0);
                _volumeNode.onDraw(r);

                _context.enforceState(_stencilBackFaces);
                _context.enforceState(_cullFrontFace);
                _volumeNode.onDraw(r);
            }

            _context.enforceState(_colorEnabled);
            _context.enforceState(_forColorPassTesting);
            _context.enforceState(_cullBackFace);
            if (_lightingMethod == LightingMethod.Additive) {
                _context.enforceState(_lights);
                _context.enforceState(_blended);
                _context.enforceState(_additiveOffset);
                _lights.detachAll();
                _lights.attach(light);
                _context.enforceState(_stencilDrawWhenNotSet);
                renderScene(r);
            } else {
                if (_rTexture) {
                    _context.enforceState(_modblended);
                    _context.enforceState(_zbufferAlways);
                    _context.enforceState(_cullBackFace);
                    _context.enforceState(_noLights);
                    _context.enforceState(_stencilDrawOnlyWhenSet);

                    _shadowColor.setAlpha(1 - light.getAmbient().getAlpha());
                    _shadowQuad.setDefaultColor(_shadowColor);
                    r.setOrtho();
                    resetShadowQuad(_context.getCurrentCamera());
                    _shadowQuad.draw(r);
                    r.unsetOrtho();
                }
            }
            light.setEnabled(false);
            _context.popEnforcedStates();
        }

        for (int l = _shadowLights.size(); --l >= 0;) {
            final Light light = _shadowLights.get(l);
            light.setEnabled(true);
        }

        if (_lightingMethod == LightingMethod.Additive && _rTexture) {
            _context.pushEnforcedStates();
            _context.enforceState(_noStencil);
            _context.enforceState(_colorEnabled);
            _context.enforceState(_cullBackFace);
            _context.enforceState(_blendTex);
            _context.enforceState(_additiveOffset);
            renderScene(r);
            _context.popEnforcedStates();
        }

        if (_renderVolume) {
            drawVolumes(r);
        }

        cleanup();
    }

    protected void cleanup() {
        _occluderMeshes.clear();
        _shadowLights.clear();
    }

    protected void maskShadowLights(final int mask) {
        for (int x = _shadowLights.size(); --x >= 0;) {
            final Light l = _shadowLights.get(x);
            l.pushLightMask();
            l.setLightMask(mask);
        }
    }

    protected void unmaskShadowLights() {
        for (int x = _shadowLights.size(); --x >= 0;) {
            final Light l = _shadowLights.get(x);
            l.popLightMask();
        }
    }

    protected void renderScene(final Renderer r) {
        for (int i = 0, sSize = _spatials.size(); i < sSize; i++) {
            final Spatial s = _spatials.get(i);
            s.onDraw(r);
        }
        r.renderBuckets();
    }

    protected void getShadowLights() {
        if (_shadowLights == null) {
            _shadowLights = new ArrayList<Light>();
        }
        for (int x = _occluders.size(); --x >= 0;) {
            getShadowLights(_occluders.get(x));
        }
    }

    protected void getShadowLights(final Spatial s) {
        if (s instanceof Mesh) {
            final Mesh g = (Mesh) s;
            final LightState ls = (LightState) g._getWorldRenderState(StateType.Light);
            if (ls != null) {
                for (int q = ls.getNumberOfChildren(); --q >= 0;) {
                    final Light l = ls.get(q);
                    if (l.isShadowCaster()
                            && (l.getType() == Light.Type.Directional || l.getType() == Light.Type.Point)
                            && !_shadowLights.contains(l)) {
                        _shadowLights.add(l);
                    }
                }
            }
        }
        if (s instanceof Node) {
            final Node n = (Node) s;
            if (n.getChildren() != null) {
                final List<Spatial> children = n.getChildren();
                for (int i = children.size(); --i >= 0;) {
                    final Spatial child = children.get(i);
                    getShadowLights(child);
                }
            }
        }

    }

    protected void setupOccluderMeshes() {
        if (_occluderMeshes == null) {
            _occluderMeshes = new ArrayList<Mesh>();
        }
        _occluderMeshes.clear();
        for (int x = _occluders.size(); --x >= 0;) {
            setupOccluderMeshes(_occluders.get(x));
        }

        _meshes.keySet().retainAll(_occluderMeshes);
    }

    protected void setupOccluderMeshes(final Spatial spat) {
        if (spat instanceof Mesh) {
            addOccluderMeshes((Mesh) spat);
        } else if (spat instanceof Node) {
            final Node node = (Node) spat;
            for (int c = 0, nQ = node.getNumberOfChildren(); c < nQ; c++) {
                final Spatial child = node.getChild(c);
                setupOccluderMeshes(child);
            }
        }
    }

    private void addOccluderMeshes(final Mesh mesh) {
        if (mesh.isCastsShadows()) {
            _occluderMeshes.add(mesh);
        }
    }

    protected void generateVolumes() {

        for (int c = 0; c < _occluderMeshes.size(); c++) {
            final Mesh mesh = _occluderMeshes.get(c);
            if (!_shadowGate.shouldUpdateShadows(mesh)) {
                continue;
            }
            if (!_meshes.containsKey(mesh)) {
                _meshes.put(mesh, new MeshShadows(mesh));
            }

            final MeshShadows sv = _meshes.get(mesh);

            // Create the geometry for the shadow volume
            final LightState state = (LightState) mesh._getWorldRenderState(RenderState.StateType.Light);
            if (state != null) {
                sv.createGeometry(state);
            }
        }
    }

    /**
     * <code>addShadowVolumes</code> adds the shadow volumes for a given light to volumeNode
     * 
     * @param light
     *            the light whose volumes should be added
     */
    protected void addShadowVolumes(final Light light) {
        if (_enabled) {
            for (int i = _occluderMeshes.size(); --i >= 0;) {
                final Mesh key = _occluderMeshes.get(i);
                if (!_shadowGate.shouldDrawShadows(key)) {
                    continue;
                }
                final MeshShadows ms = _meshes.get(key);
                final ShadowVolume lv = ms.getShadowVolume(light);
                if (lv != null) {
                    _volumeNode.getChildren().add(lv);
                }
            }
        }

    }

    /**
     * <code>drawVolumes</code> is a debug method used to draw the shadow volumes currently in use in the pass.
     * 
     * @param r
     *            Renderer to draw with.
     */
    protected void drawVolumes(final Renderer r) {

        final Node renderNode = new Node("renderVolume");
        renderNode.setRenderState(_cullBackFace);
        renderNode.setRenderState(_forTesting);
        renderNode.setRenderState(_colorEnabled);
        renderNode.setRenderState(_noStencil);
        renderNode.setRenderState(_alphaBlended);

        for (int i = _occluderMeshes.size(); --i >= 0;) {
            final Object key = _occluderMeshes.get(i);
            final MeshShadows ms = _meshes.get(key);
            if (ms != null) {
                final List<ShadowVolume> volumes = ms.getVolumes();
                for (int v = 0, vSize = volumes.size(); v < vSize; v++) {
                    final ShadowVolume vol = volumes.get(v);
                    renderNode.attachChild(vol);
                    vol.setDefaultColor(new ColorRGBA(0, 1, 0, .075f));
                }
            }
        }

        renderNode.updateGeometricState(0, true);
        renderNode.onDraw(r);
    }

    protected void init(final Camera cam) {
        if (_initialised) {
            return;
        }

        resetShadowQuad(cam);

        _initialised = true;

    }

    public void resetShadowQuad(final Camera cam) {
        if (cam.getWidth() == _quadWidth && cam.getHeight() == _quadHeight) {
            return;
        }
        _quadWidth = cam.getWidth();
        _quadHeight = cam.getHeight();
        _shadowQuad.resize(_quadWidth, cam.getHeight());
        _shadowQuad.setTranslation(new Vector3(_quadWidth >> 1, _quadHeight >> 1, 0));
        _shadowQuad.setRenderBucketType(RenderBucketType.Skip);
        _shadowQuad.updateGeometricState(0, true);

    }

    public ShadowGate getShadowGate() {
        return _shadowGate;
    }

    public void setShadowGate(final ShadowGate shadowCheck) {
        _shadowGate = shadowCheck;
    }
}
