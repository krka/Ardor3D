/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph.extension;

import java.io.IOException;
import java.nio.FloatBuffer;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.bounding.BoundingSphere;
import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture2D;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.TextureRenderer;
import com.ardor3d.renderer.TextureRendererFactory;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.util.Timer;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

/**
 * QuadImposterNode
 */
public class QuadImposterNode extends Node {
    private static final long serialVersionUID = 1L;

    protected TextureRenderer tRenderer;

    protected Texture2D texture;

    protected Node targetScene;

    protected Quad imposterQuad;

    protected double redrawRate;
    protected double elapsed;
    protected double cameraAngleThreshold;
    protected double cameraDistanceThreshold = Double.MAX_VALUE;
    protected boolean haveDrawn;

    protected Vector3 worldUpVector = new Vector3(0, 1, 0);

    protected boolean doUpdate = true;

    protected Camera cam;

    protected int twidth, theight;

    protected final Vector3 lastCamDir = new Vector3();
    protected double lastCamDist;

    protected Vector3[] corners = new Vector3[8];
    protected final Vector3 center = new Vector3();
    protected final Vector3 extents = new Vector3();
    protected final Vector2 minScreenPos = new Vector2();
    protected final Vector2 maxScreenPos = new Vector2();
    protected final Vector2 minMaxScreenPos = new Vector2();
    protected final Vector2 maxMinScreenPos = new Vector2();
    protected final Vector3 tempVec = new Vector3();
    protected double minZ;
    protected double nearPlane;
    protected double farPlane;
    protected Timer timer;

    public QuadImposterNode() {
        super();
    }

    public QuadImposterNode(final String name, final int twidth, final int theight) {
        this(name, twidth, theight, null);
    }

    public QuadImposterNode(final String name, final int twidth, final int theight, final Timer timer) {
        super(name);

        this.twidth = twidth;
        this.theight = theight;

        this.timer = timer;

        texture = new Texture2D();

        imposterQuad = new Quad("ImposterQuad");
        imposterQuad.initialize(1, 1);
        imposterQuad.setModelBound(new BoundingBox());
        imposterQuad.updateModelBound();
        imposterQuad.setTextureCombineMode(Spatial.TextureCombineMode.Replace);
        imposterQuad.setLightCombineMode(Spatial.LightCombineMode.Off);
        super.attachChild(imposterQuad);

        targetScene = new Node();
        super.attachChild(targetScene);

        for (int i = 0; i < corners.length; i++) {
            corners[i] = new Vector3();
        }

        if (timer != null) {
            redrawRate = elapsed = 0.05; // 20x per sec
        } else {
            setCameraAngleThreshold(10.0);
            setCameraDistanceThreshold(0.2);
        }
        haveDrawn = false;
    }

    @Override
    public int attachChild(final Spatial child) {
        return targetScene.attachChild(child);
    }

    @Override
    public int attachChildAt(final Spatial child, final int index) {
        return targetScene.attachChildAt(child, index);
    }

    @Override
    public void detachAllChildren() {
        targetScene.detachAllChildren();
    }

    @Override
    public int detachChild(final Spatial child) {
        return targetScene.detachChild(child);
    }

    @Override
    public Spatial detachChildAt(final int index) {
        return targetScene.detachChildAt(index);
    }

    @Override
    public int detachChildNamed(final String childName) {
        return targetScene.detachChildNamed(childName);
    }

    private void init(final Renderer renderer) {
        final DisplaySettings settings = new DisplaySettings(twidth, theight, 0, 0, 0, 8, 0, 0, false, false);
        tRenderer = TextureRendererFactory.INSTANCE.createTextureRenderer(settings, renderer, ContextManager
                .getCurrentContext().getCapabilities(), TextureRenderer.Target.Texture2D);

        tRenderer.setBackgroundColor(new ColorRGBA(0, 0, 0, 0));
        resetTexture();
    }

    @Override
    public void draw(final Renderer r) {
        if (timer != null && redrawRate > 0) {
            elapsed += timer.getTimePerFrame();
        }

        if (tRenderer == null) {
            init(r);
        }
        if (cam == null) {
            cam = ContextManager.getCurrentContext().getCurrentCamera();

            tRenderer.getCamera().setFrustum(cam.getFrustumNear(), cam.getFrustumFar(), cam.getFrustumLeft(),
                    cam.getFrustumRight(), cam.getFrustumTop(), cam.getFrustumBottom());
            tRenderer.getCamera().setFrame(cam.getLocation(), cam.getLeft(), cam.getUp(), cam.getDirection());
        }

        if (doUpdate && (!haveDrawn || shouldDoUpdate(cam)) && targetScene.getWorldBound() != null) {
            final BoundingVolume b = targetScene.getWorldBound();
            center.set(b.getCenter());

            updateCameraLookat();

            calculateImposter();

            updateCameraLookat();
            updateCameraFrustum();

            renderImposter();

            haveDrawn = true;
        }

        imposterQuad.draw(r);
    }

    @Override
    protected void updateChildren(final double time) {
        imposterQuad.updateGeometricState(time, false);
        if (doUpdate && (!haveDrawn || shouldDoUpdate(cam))) {
            targetScene.updateGeometricState(time, false);
        }
    }

    private void calculateImposter() {
        final BoundingVolume worldBound = targetScene.getWorldBound();
        center.set(worldBound.getCenter());

        for (int i = 0; i < corners.length; i++) {
            corners[i].set(center);
        }

        if (worldBound instanceof BoundingBox) {
            final BoundingBox bbox = (BoundingBox) worldBound;
            bbox.getExtent(extents);
        } else if (worldBound instanceof BoundingSphere) {
            final BoundingSphere bsphere = (BoundingSphere) worldBound;
            extents.set(bsphere.getRadius(), bsphere.getRadius(), bsphere.getRadius());
        }

        corners[0].addLocal(extents.getX(), extents.getY(), -extents.getZ());
        corners[1].addLocal(-extents.getX(), extents.getY(), -extents.getZ());
        corners[2].addLocal(extents.getX(), -extents.getY(), -extents.getZ());
        corners[3].addLocal(-extents.getX(), -extents.getY(), -extents.getZ());
        corners[4].addLocal(extents.getX(), extents.getY(), extents.getZ());
        corners[5].addLocal(-extents.getX(), extents.getY(), extents.getZ());
        corners[6].addLocal(extents.getX(), -extents.getY(), extents.getZ());
        corners[7].addLocal(-extents.getX(), -extents.getY(), extents.getZ());

        for (int i = 0; i < corners.length; i++) {
            tRenderer.getCamera().getScreenCoordinates(corners[i], corners[i]);
        }

        minScreenPos.set(Double.MAX_VALUE, Double.MAX_VALUE);
        maxScreenPos.set(-Double.MAX_VALUE, -Double.MAX_VALUE);
        minZ = Double.MAX_VALUE;
        for (int i = 0; i < corners.length; i++) {
            minScreenPos.setX(Math.min(corners[i].getX(), minScreenPos.getX()));
            minScreenPos.setY(Math.min(corners[i].getY(), minScreenPos.getY()));

            maxScreenPos.setX(Math.max(corners[i].getX(), maxScreenPos.getX()));
            maxScreenPos.setY(Math.max(corners[i].getY(), maxScreenPos.getY()));

            minZ = Math.min(corners[i].getZ(), minZ);
        }
        maxMinScreenPos.set(maxScreenPos.getX(), minScreenPos.getY());
        minMaxScreenPos.set(minScreenPos.getX(), maxScreenPos.getY());

        tRenderer.getCamera().getWorldCoordinates(maxScreenPos, minZ, corners[0]);
        tRenderer.getCamera().getWorldCoordinates(maxMinScreenPos, minZ, corners[1]);
        tRenderer.getCamera().getWorldCoordinates(minScreenPos, minZ, corners[2]);
        tRenderer.getCamera().getWorldCoordinates(minMaxScreenPos, minZ, corners[3]);
        center.set(corners[0]).addLocal(corners[1]).addLocal(corners[2]).addLocal(corners[3]).multiplyLocal(0.25);

        lastCamDir.set(center).subtractLocal(tRenderer.getCamera().getLocation());
        lastCamDist = nearPlane = lastCamDir.length();
        farPlane = nearPlane + extents.length() * 2.0;
        lastCamDir.normalizeLocal();

        final FloatBuffer vertexBuffer = imposterQuad.getMeshData().getVertexBuffer();
        BufferUtils.setInBuffer(corners[0], vertexBuffer, 3);
        BufferUtils.setInBuffer(corners[1], vertexBuffer, 2);
        BufferUtils.setInBuffer(corners[2], vertexBuffer, 1);
        BufferUtils.setInBuffer(corners[3], vertexBuffer, 0);

        imposterQuad.updateModelBound();
    }

    private void updateCameraLookat() {
        tRenderer.getCamera().setLocation(cam.getLocation());
        tRenderer.getCamera().lookAt(center, worldUpVector);
    }

    private void updateCameraFrustum() {
        final double width = corners[2].subtractLocal(corners[1]).length() / 2.0;
        final double height = corners[1].subtractLocal(corners[0]).length() / 2.0;

        tRenderer.getCamera().setFrustum(nearPlane, farPlane, -width, width, height, -height);
    }

    private boolean shouldDoUpdate(final Camera cam) {
        if (redrawRate > 0 && elapsed >= redrawRate) {
            elapsed = elapsed % redrawRate;
            return true;
        }

        if (cameraAngleThreshold > 0) {
            tempVec.set(center).subtractLocal(cam.getLocation());

            final double currentDist = tempVec.length();
            if (lastCamDist != 0 && Math.abs(currentDist - lastCamDist) / lastCamDist > cameraDistanceThreshold) {
                return true;
            }

            tempVec.normalizeLocal();
            final double angle = tempVec.smallestAngleBetween(lastCamDir);
            if (angle > cameraAngleThreshold) {
                return true;
            }
        }
        return false;
    }

    public void setRedrawRate(final double rate) {
        redrawRate = elapsed = rate;
    }

    public double getCameraDistanceThreshold() {
        return cameraDistanceThreshold;
    }

    public void setCameraDistanceThreshold(final double cameraDistanceThreshold) {
        this.cameraDistanceThreshold = cameraDistanceThreshold;
    }

    public double getCameraAngleThreshold() {
        return cameraAngleThreshold;
    }

    public void setCameraAngleThreshold(final double cameraAngleThreshold) {
        this.cameraAngleThreshold = cameraAngleThreshold;
    }

    public void resetTexture() {
        texture.setWrap(Texture.WrapMode.EdgeClamp);
        texture.setMinificationFilter(Texture.MinificationFilter.BilinearNoMipMaps);
        texture.setMagnificationFilter(Texture.MagnificationFilter.Bilinear);
        tRenderer.setupTexture(texture);
        final TextureState ts = new TextureState();
        ts.setEnabled(true);
        ts.setTexture(texture, 0);
        imposterQuad.setRenderState(ts);

        // Add a blending mode... This is so the background of the texture is
        // transparent.
        final BlendState as1 = new BlendState();
        as1.setBlendEnabled(true);
        as1.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
        as1.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
        as1.setTestEnabled(true);
        as1.setTestFunction(BlendState.TestFunction.GreaterThan);
        as1.setEnabled(true);
        imposterQuad.setRenderState(as1);
    }

    public void renderImposter() {
        tRenderer.render(targetScene, texture);
    }

    public Vector3 getWorldUpVector() {
        return worldUpVector;
    }

    public void setWorldUpVector(final Vector3 worldUpVector) {
        this.worldUpVector = worldUpVector;
    }

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(texture, "texture", null);
        capsule.write(targetScene, "targetScene", null);
        capsule.write(imposterQuad, "standIn", new Quad("ImposterQuad"));
        capsule.write(redrawRate, "redrawRate", 0.05f);
        capsule.write(cameraAngleThreshold, "cameraThreshold", 0);
        capsule.write(worldUpVector, "worldUpVector", new Vector3(Vector3.UNIT_Y));
    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);
        texture = (Texture2D) capsule.readSavable("texture", null);
        targetScene = (Node) capsule.readSavable("targetScene", null);
        imposterQuad = (Quad) capsule.readSavable("standIn", new Quad("ImposterQuad"));
        redrawRate = capsule.readFloat("redrawRate", 0.05f);
        cameraAngleThreshold = capsule.readFloat("cameraThreshold", 0);
        worldUpVector = (Vector3) capsule.readSavable("worldUpVector", new Vector3(Vector3.UNIT_Y));
    }

    public Texture getTexture() {
        return texture;
    }

    public void setDoUpdate(final boolean doUpdate) {
        this.doUpdate = doUpdate;
    }

    public boolean isDoUpdate() {
        return doUpdate;
    }
}
