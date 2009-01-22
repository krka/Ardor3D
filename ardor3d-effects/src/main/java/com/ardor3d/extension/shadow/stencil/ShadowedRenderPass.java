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
    protected List<Spatial> occluders = new ArrayList<Spatial>();

    /** node used to gather and hold shadow volumes for rendering. */
    protected Node volumeNode = new Node("Volumes");

    /** whether or not the renderstates for this pass have been init'd yet. */
    protected boolean initialised = false;

    /**
     * A quad to use with MODULATIVE lightMethod for full screen darkening against the shadow stencil.
     */
    protected Quad shadowQuad = new Quad("RenderForeground", 10, 10);

    /**
     * Used with MODULATIVE lightMethod. Defines the base color of the shadow - the alpha value is replaced with 1 - the
     * alpha of the light's alpha.
     */
    protected ColorRGBA shadowColor = new ColorRGBA(.2f, .2f, .2f, .1f);

    /** Whether shadow volumes are visible */
    protected boolean renderVolume = false;

    /** Whether to render shadows (true) or act like a normal RenderPass (false) */
    protected boolean renderShadows = true;

    /** Sets the type of pass to do to show shadows - ADDITIVE or MODULATIVE */
    protected LightingMethod lightingMethod = LightingMethod.Additive;

    /** collection of Mesh to MeshShadows mappings */
    protected IdentityHashMap<Mesh, MeshShadows> meshes = new IdentityHashMap<Mesh, MeshShadows>();

    /**
     * list of occluders that will be casting shadows in this pass. If no occluders set, pass acts like normal
     * RenderPass.
     */
    protected List<Mesh> occluderMeshes = new ArrayList<Mesh>();

    /**
     * list of lights that will be used to calculate shadows in this pass. Constructed dynamically by searching through
     * the scene for lights with shadowCaster set to true.
     */
    protected List<Light> shadowLights = new ArrayList<Light>();

    protected int quadWidth = -1, quadHeight = -1;

    private ShadowGate shadowGate = new DefaultShadowGate();

    public static boolean rTexture = true;

    protected ZBufferState zbufferWriteLE;
    protected ZBufferState zbufferAlways;
    protected ZBufferState forTesting;
    protected ZBufferState forColorPassTesting;

    protected StencilState noStencil;
    protected StencilState stencilFrontFaces;
    protected StencilState stencilBothFaces;
    protected StencilState stencilBackFaces;
    protected StencilState stencilDrawOnlyWhenSet;
    protected StencilState stencilDrawWhenNotSet;

    protected CullState cullFrontFace;
    protected CullState cullBackFace;
    protected CullState noCull;

    protected TextureState noTexture;

    protected LightState lights;
    protected LightState noLights;

    protected OffsetState additiveOffset;

    protected BlendState blended;
    protected BlendState alphaBlended;
    protected BlendState modblended;
    protected BlendState blendTex;

    protected ColorMaskState colorEnabled;
    protected ColorMaskState colorDisabled;

    public ShadowedRenderPass() {

        zbufferWriteLE = new ZBufferState();
        zbufferWriteLE.setWritable(true);
        zbufferWriteLE.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
        zbufferWriteLE.setEnabled(true);

        zbufferAlways = new ZBufferState();
        zbufferAlways.setEnabled(false);
        zbufferAlways.setWritable(false);

        forTesting = new ZBufferState();
        forTesting.setWritable(false);
        forTesting.setFunction(ZBufferState.TestFunction.LessThan);
        forTesting.setEnabled(true);

        forColorPassTesting = new ZBufferState();
        forColorPassTesting.setWritable(false);
        forColorPassTesting.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
        forColorPassTesting.setEnabled(true);

        noStencil = new StencilState();
        noStencil.setEnabled(false);

        stencilBothFaces = new StencilState();
        stencilBothFaces.setEnabled(true);
        stencilBothFaces.setUseTwoSided(true);
        stencilBothFaces.setStencilMaskFront(~0);
        stencilBothFaces.setStencilFunctionFront(StencilFunction.Always);
        stencilBothFaces.setStencilOpFailFront(StencilOperation.Keep);
        stencilBothFaces.setStencilOpZFailFront(StencilOperation.Keep);
        stencilBothFaces.setStencilOpZPassFront(StencilOperation.IncrementWrap);
        stencilBothFaces.setStencilMaskBack(~0);
        stencilBothFaces.setStencilFunctionBack(StencilFunction.Always);
        stencilBothFaces.setStencilOpFailBack(StencilOperation.Keep);
        stencilBothFaces.setStencilOpZFailBack(StencilOperation.Keep);
        stencilBothFaces.setStencilOpZPassBack(StencilOperation.DecrementWrap);

        stencilFrontFaces = new StencilState();
        stencilFrontFaces.setEnabled(true);
        stencilFrontFaces.setStencilMask(~0);
        stencilFrontFaces.setStencilFunction(StencilFunction.Always);
        stencilFrontFaces.setStencilOpFail(StencilOperation.Keep);
        stencilFrontFaces.setStencilOpZFail(StencilOperation.Keep);
        stencilFrontFaces.setStencilOpZPass(StencilOperation.IncrementWrap);

        stencilBackFaces = new StencilState();
        stencilBackFaces.setEnabled(true);
        stencilBackFaces.setStencilMask(~0);
        stencilBackFaces.setStencilFunction(StencilFunction.Always);
        stencilBackFaces.setStencilOpFail(StencilOperation.Keep);
        stencilBackFaces.setStencilOpZFail(StencilOperation.Keep);
        stencilBackFaces.setStencilOpZPass(StencilOperation.DecrementWrap);

        stencilDrawOnlyWhenSet = new StencilState();
        stencilDrawOnlyWhenSet.setEnabled(true);
        stencilDrawOnlyWhenSet.setStencilMask(~0);
        stencilDrawOnlyWhenSet.setStencilFunction(StencilFunction.NotEqualTo);
        stencilDrawOnlyWhenSet.setStencilOpFail(StencilOperation.Keep);
        stencilDrawOnlyWhenSet.setStencilOpZFail(StencilOperation.Keep);
        stencilDrawOnlyWhenSet.setStencilOpZPass(StencilOperation.Keep);
        stencilDrawOnlyWhenSet.setStencilReference(0);

        stencilDrawWhenNotSet = new StencilState();
        stencilDrawWhenNotSet.setEnabled(true);
        stencilDrawWhenNotSet.setStencilMask(~0);
        stencilDrawWhenNotSet.setStencilFunction(StencilFunction.EqualTo);
        stencilDrawWhenNotSet.setStencilOpFail(StencilOperation.Keep);
        stencilDrawWhenNotSet.setStencilOpZFail(StencilOperation.Keep);
        stencilDrawWhenNotSet.setStencilOpZPass(StencilOperation.Keep);
        stencilDrawWhenNotSet.setStencilReference(0);

        cullFrontFace = new CullState();
        cullFrontFace.setEnabled(true);
        cullFrontFace.setCullFace(CullState.Face.Front);

        noCull = new CullState();
        noCull.setEnabled(false);

        noLights = new LightState();
        noLights.setEnabled(false);

        cullBackFace = new CullState();
        cullBackFace.setEnabled(true);
        cullBackFace.setCullFace(CullState.Face.Back);

        blended = new BlendState();
        blended.setEnabled(true);
        blended.setBlendEnabled(true);
        blended.setDestinationFunction(BlendState.DestinationFunction.One);
        blended.setSourceFunction(BlendState.SourceFunction.DestinationColor);

        alphaBlended = new BlendState();
        alphaBlended.setEnabled(true);
        alphaBlended.setBlendEnabled(true);
        alphaBlended.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
        alphaBlended.setSourceFunction(BlendState.SourceFunction.One);

        modblended = new BlendState();
        modblended.setEnabled(true);
        modblended.setBlendEnabled(true);
        modblended.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
        modblended.setSourceFunction(BlendState.SourceFunction.DestinationColor);

        blendTex = new BlendState();
        blendTex.setEnabled(true);
        blendTex.setBlendEnabled(true);
        blendTex.setDestinationFunction(BlendState.DestinationFunction.Zero);
        blendTex.setSourceFunction(BlendState.SourceFunction.DestinationColor);

        colorEnabled = new ColorMaskState();
        colorEnabled.setAll(true);

        colorDisabled = new ColorMaskState();
        colorDisabled.setAll(false);

        additiveOffset = new OffsetState();
        additiveOffset.setTypeEnabled(OffsetType.Fill, true);
        additiveOffset.setTypeEnabled(OffsetType.Line, true);
        additiveOffset.setTypeEnabled(OffsetType.Point, true);
        additiveOffset.setUnits(-5);

        volumeNode.setRenderBucketType(RenderBucketType.Skip);
        volumeNode.attachChild(new Node());

        noTexture = new TextureState();
        noTexture.setEnabled(false);

        lights = new LightState();
        lights.setEnabled(true);
        lights.setLightMask(LightState.MASK_AMBIENT | LightState.MASK_GLOBALAMBIENT);
    }

    /**
     * <code>addOccluder</code> adds an occluder to this pass.
     * 
     * @param toAdd
     *            Occluder Spatial to add to this pass.
     */
    public void addOccluder(final Spatial toAdd) {
        occluders.add(toAdd);
    }

    /**
     * <code>clearOccluders</code> removes all occluders from this pass.
     */
    public void clearOccluders() {
        occluders.clear();
    }

    /**
     * <code>containsOccluder</code>
     * 
     * @param s
     * @return
     */
    public boolean containsOccluder(final Spatial s) {
        return occluders.contains(s);
    }

    /**
     * <code>removeOccluder</code>
     * 
     * @param toRemove
     *            the Occluder Spatial to remove from this pass.
     * @return true if the Spatial was found and removed.
     */
    public boolean removeOccluder(final Spatial toRemove) {
        return occluders.remove(toRemove);
    }

    /**
     * @return the number of occluders registered with this pass
     */
    public int occludersSize() {
        return occluders.size();
    }

    /**
     * @return Returns whether shadow volumes will be rendered to the display.
     */
    public boolean getRenderVolume() {
        return renderVolume;
    }

    /**
     * @param renderVolume
     *            sets whether shadow volumes will be rendered to the display
     */
    public void setRenderVolume(final boolean renderVolume) {
        this.renderVolume = renderVolume;
    }

    /**
     * @return whether shadow volumes will be rendered to the display.
     */
    public boolean getRenderShadows() {
        return renderShadows;
    }

    /**
     * @param renderShadows
     *            whether shadows will be rendered by this pass.
     */
    public void setRenderShadows(final boolean renderShadows) {
        this.renderShadows = renderShadows;
    }

    /**
     * @return the shadowColor used by MODULATIVE lightMethod.
     */
    public ColorRGBA getShadowColor() {
        return shadowColor;
    }

    /**
     * @param shadowColor
     *            the shadowColor used by MODULATIVE lightMethod.
     */
    public void setShadowColor(final ColorRGBA shadowColor) {
        if (shadowColor == null) {
            throw new IllegalArgumentException("shadowColor must not be null!");
        }
        this.shadowColor = shadowColor;
    }

    /**
     * @return the lightingMethod currently in use.
     */
    public LightingMethod getLightingMethod() {
        return lightingMethod;
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
        lightingMethod = method;
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

        if (!renderShadows) {
            renderScene(r);
            if (renderVolume) {
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
        if (occluderMeshes.size() == 0 || shadowLights.size() == 0) {
            // render normal
            renderScene(r);
            cleanup();
            return;
        }

        // otherwise render an ambient pass by masking the diffuse and specular of shadowcasting lights.
        if (lightingMethod == LightingMethod.Additive) {
            maskShadowLights(LightState.MASK_DIFFUSE | LightState.MASK_SPECULAR);
            _context.pushEnforcedStates();
            _context.enforceState(noTexture);
            renderScene(r);
            _context.popEnforcedStates();
            unmaskShadowLights();
        } else {
            renderScene(r);
        }

        generateVolumes();

        for (int l = shadowLights.size(); --l >= 0;) {
            final Light light = shadowLights.get(l);
            light.setEnabled(false);
        }
        for (int l = shadowLights.size(); --l >= 0;) {
            final Light light = shadowLights.get(l);
            // Clear out the stencil buffer
            r.clearStencilBuffer();
            light.setEnabled(true);

            _context.pushEnforcedStates();
            _context.enforceState(noTexture);
            _context.enforceState(forTesting);
            _context.enforceState(colorDisabled);

            if (_context.getCapabilities().isTwoSidedStencilSupported()) {
                _context.enforceState(noCull);
                _context.enforceState(stencilBothFaces);
                volumeNode.getChildren().clear();
                addShadowVolumes(light);
                volumeNode.updateGeometricState(0);
                volumeNode.onDraw(r);
            } else {
                _context.enforceState(stencilFrontFaces);
                _context.enforceState(cullBackFace);

                volumeNode.getChildren().clear();
                addShadowVolumes(light);
                volumeNode.updateGeometricState(0);
                volumeNode.onDraw(r);

                _context.enforceState(stencilBackFaces);
                _context.enforceState(cullFrontFace);
                volumeNode.onDraw(r);
            }

            _context.enforceState(colorEnabled);
            _context.enforceState(forColorPassTesting);
            _context.enforceState(cullBackFace);
            if (lightingMethod == LightingMethod.Additive) {
                _context.enforceState(lights);
                _context.enforceState(blended);
                _context.enforceState(additiveOffset);
                lights.detachAll();
                lights.attach(light);
                _context.enforceState(stencilDrawWhenNotSet);
                renderScene(r);
            } else {
                if (rTexture) {
                    _context.enforceState(modblended);
                    _context.enforceState(zbufferAlways);
                    _context.enforceState(cullBackFace);
                    _context.enforceState(noLights);
                    _context.enforceState(stencilDrawOnlyWhenSet);

                    shadowColor.setAlpha(1 - light.getAmbient().getAlpha());
                    shadowQuad.setDefaultColor(shadowColor);
                    r.setOrtho();
                    resetShadowQuad(_context.getCurrentCamera());
                    shadowQuad.draw(r);
                    r.unsetOrtho();
                }
            }
            light.setEnabled(false);
            _context.popEnforcedStates();
        }

        for (int l = shadowLights.size(); --l >= 0;) {
            final Light light = shadowLights.get(l);
            light.setEnabled(true);
        }

        if (lightingMethod == LightingMethod.Additive && rTexture) {
            _context.pushEnforcedStates();
            _context.enforceState(noStencil);
            _context.enforceState(colorEnabled);
            _context.enforceState(cullBackFace);
            _context.enforceState(blendTex);
            _context.enforceState(additiveOffset);
            renderScene(r);
            _context.popEnforcedStates();
        }

        if (renderVolume) {
            drawVolumes(r);
        }

        cleanup();
    }

    protected void cleanup() {
        occluderMeshes.clear();
        shadowLights.clear();
    }

    protected void maskShadowLights(final int mask) {
        for (int x = shadowLights.size(); --x >= 0;) {
            final Light l = shadowLights.get(x);
            l.pushLightMask();
            l.setLightMask(mask);
        }
    }

    protected void unmaskShadowLights() {
        for (int x = shadowLights.size(); --x >= 0;) {
            final Light l = shadowLights.get(x);
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
        if (shadowLights == null) {
            shadowLights = new ArrayList<Light>();
        }
        for (int x = occluders.size(); --x >= 0;) {
            getShadowLights(occluders.get(x));
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
                            && !shadowLights.contains(l)) {
                        shadowLights.add(l);
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
        if (occluderMeshes == null) {
            occluderMeshes = new ArrayList<Mesh>();
        }
        occluderMeshes.clear();
        for (int x = occluders.size(); --x >= 0;) {
            setupOccluderMeshes(occluders.get(x));
        }

        meshes.keySet().retainAll(occluderMeshes);
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
            occluderMeshes.add(mesh);
        }
    }

    protected void generateVolumes() {

        for (int c = 0; c < occluderMeshes.size(); c++) {
            final Mesh mesh = occluderMeshes.get(c);
            if (!shadowGate.shouldUpdateShadows(mesh)) {
                continue;
            }
            if (!meshes.containsKey(mesh)) {
                meshes.put(mesh, new MeshShadows(mesh));
            }

            final MeshShadows sv = meshes.get(mesh);

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
            for (int i = occluderMeshes.size(); --i >= 0;) {
                final Mesh key = occluderMeshes.get(i);
                if (!shadowGate.shouldDrawShadows(key)) {
                    continue;
                }
                final MeshShadows ms = meshes.get(key);
                final ShadowVolume lv = ms.getShadowVolume(light);
                if (lv != null) {
                    volumeNode.getChildren().add(lv);
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
        renderNode.setRenderState(cullBackFace);
        renderNode.setRenderState(forTesting);
        renderNode.setRenderState(colorEnabled);
        renderNode.setRenderState(noStencil);
        renderNode.setRenderState(alphaBlended);

        for (int i = occluderMeshes.size(); --i >= 0;) {
            final Object key = occluderMeshes.get(i);
            final MeshShadows ms = meshes.get(key);
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
        if (initialised) {
            return;
        }

        resetShadowQuad(cam);

        initialised = true;

    }

    public void resetShadowQuad(final Camera cam) {
        if (cam.getWidth() == quadWidth && cam.getHeight() == quadHeight) {
            return;
        }
        quadWidth = cam.getWidth();
        quadHeight = cam.getHeight();
        shadowQuad.resize(quadWidth, cam.getHeight());
        shadowQuad.setTranslation(new Vector3(quadWidth >> 1, quadHeight >> 1, 0));
        shadowQuad.setRenderBucketType(RenderBucketType.Skip);
        shadowQuad.updateGeometricState(0, true);

    }

    public ShadowGate getShadowGate() {
        return shadowGate;
    }

    public void setShadowGate(final ShadowGate shadowCheck) {
        shadowGate = shadowCheck;
    }
}
