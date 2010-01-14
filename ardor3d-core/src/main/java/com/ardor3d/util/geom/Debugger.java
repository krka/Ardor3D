/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.geom;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.bounding.BoundingSphere;
import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.bounding.OrientedBoundingBox;
import com.ardor3d.image.Image;
import com.ardor3d.image.Texture2D;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.TextureRenderer;
import com.ardor3d.renderer.TextureRendererFactory;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.WireframeState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.shape.AxisRods;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.scenegraph.shape.OrientedBox;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.scenegraph.shape.Sphere;

/**
 * Debugger provides tools for viewing scene data such as boundings and normals.
 * 
 * Make sure you set the RenderStateFactory before using this class.
 * 
 * @see Debugger#setRenderStateFactory(RenderStateFactory)
 */
public final class Debugger {

    // -- **** METHODS FOR DRAWING BOUNDING VOLUMES **** -- //

    private static final Sphere boundingSphere = new Sphere("bsphere", 10, 10, 1);
    static {
        boundingSphere.getSceneHints().setRenderBucketType(RenderBucketType.Skip);
        boundingSphere.setRenderState(new WireframeState());
        boundingSphere.setRenderState(new ZBufferState());
        boundingSphere.updateWorldRenderStates(false);
    }
    private static final Box boundingBox = new Box("bbox", new Vector3(), 1, 1, 1);
    static {
        boundingBox.getSceneHints().setRenderBucketType(RenderBucketType.Skip);
        boundingBox.setRenderState(new WireframeState());
        boundingBox.setRenderState(new ZBufferState());
        boundingBox.updateWorldRenderStates(false);
    }
    private static final OrientedBox boundingOB = new OrientedBox("bobox");
    static {
        boundingOB.getSceneHints().setRenderBucketType(RenderBucketType.Skip);
        boundingOB.setRenderState(new WireframeState());
        boundingOB.setRenderState(new ZBufferState());
        boundingOB.updateWorldRenderStates(false);
    }

    /**
     * <code>drawBounds</code> draws the bounding volume for a given Spatial and its children.
     * 
     * @param se
     *            the Spatial to draw boundings for.
     * @param r
     *            the Renderer to use to draw the bounding.
     */
    public static void drawBounds(final Spatial se, final Renderer r) {
        drawBounds(se, r, true);
    }

    /**
     * <code>drawBounds</code> draws the bounding volume for a given Spatial and optionally its children.
     * 
     * @param se
     *            the Spatial to draw boundings for.
     * @param r
     *            the Renderer to use to draw the bounding.
     * @param doChildren
     *            if true, boundings for any children will also be drawn
     */
    public static void drawBounds(final Spatial se, final Renderer r, boolean doChildren) {
        if (se == null) {
            return;
        }

        if (se.getWorldBound() != null && se.getSceneHints().getCullHint() != CullHint.Always) {
            final Camera cam = Camera.getCurrentCamera();
            final int state = cam.getPlaneState();
            if (cam.contains(se.getWorldBound()) != Camera.FrustumIntersect.Outside) {
                drawBounds(se.getWorldBound(), r);
            } else {
                doChildren = false;
            }
            cam.setPlaneState(state);
        }
        if (doChildren && se instanceof Node) {
            final Node n = (Node) se;
            if (n.getNumberOfChildren() != 0) {
                for (int i = n.getNumberOfChildren(); --i >= 0;) {
                    drawBounds(n.getChild(i), r, true);
                }
            }
        }
    }

    public static void drawBounds(final BoundingVolume bv, final Renderer r) {

        switch (bv.getType()) {
            case AABB:
                drawBoundingBox((BoundingBox) bv, r);
                break;
            case Sphere:
                drawBoundingSphere((BoundingSphere) bv, r);
                break;
            case OBB:
                drawOBB((OrientedBoundingBox) bv, r);
                break;
            default:
                break;
        }
    }

    public static void setBoundsColor(final ColorRGBA color) {
        boundingBox.setSolidColor(color);
        boundingOB.setSolidColor(color);
        boundingSphere.setSolidColor(color);
    }

    public static void drawBoundingSphere(final BoundingSphere sphere, final Renderer r) {
        boundingSphere.setData(sphere.getCenter(), 10, 10, sphere.getRadius());
        boundingSphere.draw(r);
    }

    public static void drawBoundingBox(final BoundingBox box, final Renderer r) {
        boundingBox.setData(box.getCenter(), box.getXExtent(), box.getYExtent(), box.getZExtent());
        boundingBox.draw(r);
    }

    public static void drawOBB(final OrientedBoundingBox box, final Renderer r) {
        boundingOB.getCenter().set(box.getCenter());
        boundingOB.getxAxis().set(box.getXAxis());
        boundingOB.getYAxis().set(box.getYAxis());
        boundingOB.getZAxis().set(box.getZAxis());
        boundingOB.getExtent().set(box.getExtent());
        boundingOB.computeInformation();
        boundingOB.draw(r);
    }

    // -- **** METHODS FOR DRAWING NORMALS **** -- //

    private static final Line normalLines = new Line("normLine");
    static {
        normalLines.getSceneHints().setRenderBucketType(RenderBucketType.Skip);
        normalLines.setRenderState(new ZBufferState());
        normalLines.setLineWidth(3.0f);
        normalLines.getMeshData().setIndexMode(IndexMode.Lines);
        normalLines.getMeshData().setVertexBuffer(BufferUtils.createVector3Buffer(500));
        normalLines.getMeshData().setColorBuffer(BufferUtils.createColorBuffer(500));
        normalLines.generateIndices();
        normalLines.updateWorldRenderStates(false);
    }
    private static final Vector3 _normalVect = new Vector3(), _normalVect2 = new Vector3();
    public static final ColorRGBA NORMAL_COLOR_BASE = new ColorRGBA(ColorRGBA.RED);
    public static final ColorRGBA NORMAL_COLOR_TIP = new ColorRGBA(ColorRGBA.PINK);
    public static final ColorRGBA TANGENT_COLOR_BASE = new ColorRGBA(ColorRGBA.ORANGE);
    public static final ColorRGBA TANGENT_COLOR_TIP = new ColorRGBA(ColorRGBA.YELLOW);
    protected static final BoundingBox measureBox = new BoundingBox();
    public static double AUTO_NORMAL_RATIO = .05;

    /**
     * <code>drawNormals</code> draws lines representing normals for a given Spatial and its children.
     * 
     * @param element
     *            the Spatial to draw normals for.
     * @param r
     *            the Renderer to use to draw the normals.
     */
    public static void drawNormals(final Spatial element, final Renderer r) {
        drawNormals(element, r, -1f, true);
    }

    public static void drawTangents(final Spatial element, final Renderer r) {
        drawTangents(element, r, -1f, true);
    }

    /**
     * <code>drawNormals</code> draws the normals for a given Spatial and optionally its children.
     * 
     * @param element
     *            the Spatial to draw normals for.
     * @param r
     *            the Renderer to use to draw the normals.
     * @param size
     *            the length of the drawn normal (default is -1.0 which means autocalc based on boundings - if any).
     * @param doChildren
     *            if true, normals for any children will also be drawn
     */
    public static void drawNormals(final Spatial element, final Renderer r, final double size, final boolean doChildren) {
        if (element == null) {
            return;
        }

        final Camera cam = Camera.getCurrentCamera();
        final int state = cam.getPlaneState();
        if (element.getWorldBound() != null && cam.contains(element.getWorldBound()) == Camera.FrustumIntersect.Outside) {
            cam.setPlaneState(state);
            return;
        }
        cam.setPlaneState(state);
        if (element instanceof Mesh && element.getSceneHints().getCullHint() != CullHint.Always) {
            final Mesh mesh = (Mesh) element;

            double rSize = size;
            if (rSize == -1) {
                final BoundingVolume vol = element.getWorldBound();
                if (vol != null) {
                    measureBox.setCenter(vol.getCenter());
                    measureBox.setXExtent(0);
                    measureBox.setYExtent(0);
                    measureBox.setZExtent(0);
                    measureBox.mergeLocal(vol);
                    rSize = AUTO_NORMAL_RATIO
                            * ((measureBox.getXExtent() + measureBox.getYExtent() + measureBox.getZExtent()) / 3);
                } else {
                    rSize = 1.0;
                }
            }

            final FloatBuffer norms = mesh.getMeshData().getNormalBuffer();
            final FloatBuffer verts = mesh.getMeshData().getVertexBuffer();
            if (norms != null && verts != null && norms.limit() == verts.limit()) {
                FloatBuffer lineVerts = normalLines.getMeshData().getVertexBuffer();
                if (lineVerts.capacity() < (3 * (2 * mesh.getMeshData().getVertexCount()))) {
                    normalLines.getMeshData().setVertexBuffer(null);
                    lineVerts = BufferUtils.createVector3Buffer(mesh.getMeshData().getVertexCount() * 2);
                    normalLines.getMeshData().setVertexBuffer(lineVerts);
                } else {
                    lineVerts.clear();
                    lineVerts.limit(3 * 2 * mesh.getMeshData().getVertexCount());
                    normalLines.getMeshData().setVertexBuffer(lineVerts);
                }

                FloatBuffer lineColors = normalLines.getMeshData().getColorBuffer();
                if (lineColors.capacity() < (4 * (2 * mesh.getMeshData().getVertexCount()))) {
                    normalLines.getMeshData().setColorBuffer(null);
                    lineColors = BufferUtils.createColorBuffer(mesh.getMeshData().getVertexCount() * 2);
                    normalLines.getMeshData().setColorBuffer(lineColors);
                } else {
                    lineColors.clear();
                }

                IntBuffer lineInds = normalLines.getMeshData().getIndexBuffer();
                if (lineInds == null || lineInds.capacity() < (normalLines.getMeshData().getVertexCount())) {
                    normalLines.getMeshData().setIndexBuffer(null);
                    lineInds = BufferUtils.createIntBuffer(mesh.getMeshData().getVertexCount() * 2);
                    normalLines.getMeshData().setIndexBuffer(lineInds);
                } else {
                    lineInds.clear();
                    lineInds.limit(normalLines.getMeshData().getVertexCount());
                }

                verts.rewind();
                norms.rewind();
                lineVerts.rewind();
                lineInds.rewind();

                for (int x = 0; x < mesh.getMeshData().getVertexCount(); x++) {
                    _normalVect.set(verts.get(), verts.get(), verts.get());
                    mesh.getWorldTransform().applyForward(_normalVect);
                    lineVerts.put(_normalVect.getXf());
                    lineVerts.put(_normalVect.getYf());
                    lineVerts.put(_normalVect.getZf());

                    lineColors.put(NORMAL_COLOR_BASE.getRed());
                    lineColors.put(NORMAL_COLOR_BASE.getGreen());
                    lineColors.put(NORMAL_COLOR_BASE.getBlue());
                    lineColors.put(NORMAL_COLOR_BASE.getAlpha());

                    lineInds.put(x * 2);

                    _normalVect2.set(norms.get(), norms.get(), norms.get());
                    mesh.getWorldTransform().applyForwardVector(_normalVect2).normalizeLocal().multiplyLocal(rSize);
                    _normalVect.addLocal(_normalVect2);
                    lineVerts.put(_normalVect.getXf());
                    lineVerts.put(_normalVect.getYf());
                    lineVerts.put(_normalVect.getZf());

                    lineColors.put(NORMAL_COLOR_TIP.getRed());
                    lineColors.put(NORMAL_COLOR_TIP.getGreen());
                    lineColors.put(NORMAL_COLOR_TIP.getBlue());
                    lineColors.put(NORMAL_COLOR_TIP.getAlpha());

                    lineInds.put((x * 2) + 1);
                }

                normalLines.onDraw(r);
            }

        }

        if (doChildren && element instanceof Node) {
            final Node n = (Node) element;
            if (n.getNumberOfChildren() != 0) {
                for (int i = n.getNumberOfChildren(); --i >= 0;) {
                    drawNormals(n.getChild(i), r, size, true);
                }
            }
        }
    }

    public static void drawTangents(final Spatial element, final Renderer r, final double size, final boolean doChildren) {
        if (element == null) {
            return;
        }

        final Camera cam = Camera.getCurrentCamera();
        final int state = cam.getPlaneState();
        if (element.getWorldBound() != null && cam.contains(element.getWorldBound()) == Camera.FrustumIntersect.Outside) {
            cam.setPlaneState(state);
            return;
        }
        cam.setPlaneState(state);
        if (element instanceof Mesh && element.getSceneHints().getCullHint() != CullHint.Always) {
            final Mesh mesh = (Mesh) element;

            double rSize = size;
            if (rSize == -1) {
                final BoundingVolume vol = element.getWorldBound();
                if (vol != null) {
                    measureBox.setCenter(vol.getCenter());
                    measureBox.setXExtent(0);
                    measureBox.setYExtent(0);
                    measureBox.setZExtent(0);
                    measureBox.mergeLocal(vol);
                    rSize = AUTO_NORMAL_RATIO
                            * ((measureBox.getXExtent() + measureBox.getYExtent() + measureBox.getZExtent()) / 3f);
                } else {
                    rSize = 1.0;
                }
            }

            final FloatBuffer norms = mesh.getMeshData().getTangentBuffer();
            final FloatBuffer verts = mesh.getMeshData().getVertexBuffer();
            if (norms != null && verts != null && norms.limit() == verts.limit()) {
                FloatBuffer lineVerts = normalLines.getMeshData().getVertexBuffer();
                if (lineVerts.capacity() < (3 * (2 * mesh.getMeshData().getVertexCount()))) {
                    normalLines.getMeshData().setVertexBuffer(null);
                    lineVerts = BufferUtils.createVector3Buffer(mesh.getMeshData().getVertexCount() * 2);
                    normalLines.getMeshData().setVertexBuffer(lineVerts);
                } else {
                    lineVerts.clear();
                    lineVerts.limit(3 * 2 * mesh.getMeshData().getVertexCount());
                    normalLines.getMeshData().setVertexBuffer(lineVerts);
                }

                FloatBuffer lineColors = normalLines.getMeshData().getColorBuffer();
                if (lineColors.capacity() < (4 * (2 * mesh.getMeshData().getVertexCount()))) {
                    normalLines.getMeshData().setColorBuffer(null);
                    lineColors = BufferUtils.createColorBuffer(mesh.getMeshData().getVertexCount() * 2);
                    normalLines.getMeshData().setColorBuffer(lineColors);
                } else {
                    lineColors.clear();
                }

                IntBuffer lineInds = normalLines.getMeshData().getIndexBuffer();
                if (lineInds == null || lineInds.capacity() < (normalLines.getMeshData().getVertexCount())) {
                    normalLines.getMeshData().setIndexBuffer(null);
                    lineInds = BufferUtils.createIntBuffer(mesh.getMeshData().getVertexCount() * 2);
                    normalLines.getMeshData().setIndexBuffer(lineInds);
                } else {
                    lineInds.clear();
                    lineInds.limit(normalLines.getMeshData().getVertexCount());
                }

                verts.rewind();
                norms.rewind();
                lineVerts.rewind();
                lineInds.rewind();

                for (int x = 0; x < mesh.getMeshData().getVertexCount(); x++) {
                    _normalVect.set(verts.get(), verts.get(), verts.get());
                    _normalVect.multiplyLocal(mesh.getWorldScale());
                    lineVerts.put(_normalVect.getXf());
                    lineVerts.put(_normalVect.getYf());
                    lineVerts.put(_normalVect.getZf());

                    lineColors.put(TANGENT_COLOR_BASE.getRed());
                    lineColors.put(TANGENT_COLOR_BASE.getGreen());
                    lineColors.put(TANGENT_COLOR_BASE.getBlue());
                    lineColors.put(TANGENT_COLOR_BASE.getAlpha());

                    lineInds.put(x * 2);

                    _normalVect.addLocal(norms.get() * rSize, norms.get() * rSize, norms.get() * rSize);
                    lineVerts.put(_normalVect.getXf());
                    lineVerts.put(_normalVect.getYf());
                    lineVerts.put(_normalVect.getZf());

                    lineColors.put(TANGENT_COLOR_TIP.getRed());
                    lineColors.put(TANGENT_COLOR_TIP.getGreen());
                    lineColors.put(TANGENT_COLOR_TIP.getBlue());
                    lineColors.put(TANGENT_COLOR_TIP.getAlpha());

                    lineInds.put((x * 2) + 1);
                }

                if (mesh != null) {
                    normalLines.setWorldTranslation(mesh.getWorldTranslation());
                    normalLines.setWorldRotation(mesh.getWorldRotation());
                    normalLines.onDraw(r);
                }
            }

        }

        if (doChildren && element instanceof Node) {
            final Node n = (Node) element;
            if (n.getNumberOfChildren() != 0) {
                for (int i = n.getNumberOfChildren(); --i >= 0;) {
                    drawTangents(n.getChild(i), r, size, true);
                }
            }
        }
    }

    // -- **** METHODS FOR DRAWING AXIS **** -- //

    private static final AxisRods rods = new AxisRods("debug_rods", true, 1);
    static {
        rods.getSceneHints().setRenderBucketType(RenderBucketType.Skip);
    }
    private static boolean axisInited = false;

    public static void drawAxis(final Spatial spat, final Renderer r) {
        drawAxis(spat, r, true, false);
    }

    public static void drawAxis(final Spatial spat, final Renderer r, final boolean drawChildren, final boolean drawAll) {
        if (!axisInited) {
            final BlendState blendState = new BlendState();
            blendState.setBlendEnabled(true);
            blendState.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
            blendState.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
            rods.setRenderState(blendState);
            rods.updateGeometricState(0, false);
            axisInited = true;
        }

        if (drawAll || (spat instanceof Mesh)) {
            if (spat.getWorldBound() != null) {
                double rSize;
                final BoundingVolume vol = spat.getWorldBound();
                if (vol != null) {
                    measureBox.setCenter(vol.getCenter());
                    measureBox.setXExtent(0);
                    measureBox.setYExtent(0);
                    measureBox.setZExtent(0);
                    measureBox.mergeLocal(vol);
                    rSize = 1 * ((measureBox.getXExtent() + measureBox.getYExtent() + measureBox.getZExtent()) / 3);
                } else {
                    rSize = 1.0;
                }

                rods.setTranslation(spat.getWorldBound().getCenter());
                rods.setScale(rSize);
            } else {
                rods.setTranslation(spat.getWorldTranslation());
                rods.setScale(spat.getWorldScale());
            }
            rods.setRotation(spat.getWorldRotation());
            rods.updateGeometricState(0, false);

            rods.draw(r);
        }

        if ((spat instanceof Node) && drawChildren) {
            final Node n = (Node) spat;
            if (n.getNumberOfChildren() == 0) {
                return;
            }
            for (int x = 0, count = n.getNumberOfChildren(); x < count; x++) {
                drawAxis(n.getChild(x), r, drawChildren, drawAll);
            }
        }
    }

    // -- **** METHODS FOR DISPLAYING BUFFERS **** -- //
    public static final int NORTHWEST = 0;
    public static final int NORTHEAST = 1;
    public static final int SOUTHEAST = 2;
    public static final int SOUTHWEST = 3;

    private static final Quad bQuad = new Quad("", 128, 128);
    private static Texture2D bufTexture;
    private static TextureRenderer bufTexRend;

    static {
        bQuad.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);
        bQuad.getSceneHints().setCullHint(CullHint.Never);
    }

    public static void drawBuffer(final Image.Format rttFormat, final int location, final Renderer r) {
        final Camera cam = Camera.getCurrentCamera();
        drawBuffer(rttFormat, location, r, cam.getWidth() / 6.25);
    }

    public static void drawBuffer(final Image.Format rttFormat, final int location, final Renderer r, final double size) {
        final Camera cam = Camera.getCurrentCamera();
        r.flushGraphics();
        double locationX = cam.getWidth(), locationY = cam.getHeight();
        bQuad.resize(size, (cam.getHeight() / (double) cam.getWidth()) * size);
        if (bQuad.getLocalRenderState(RenderState.StateType.Texture) == null) {
            final TextureState ts = new TextureState();
            bufTexture = new Texture2D();
            ts.setTexture(bufTexture);
            bQuad.setRenderState(ts);
        }

        bufTexture.setRenderToTextureFormat(rttFormat);

        int width = cam.getWidth();
        if (!MathUtils.isPowerOfTwo(width)) {
            int newWidth = 2;
            do {
                newWidth <<= 1;

            } while (newWidth < width);
            bQuad.getMeshData().getTextureBuffer(0).put(4, width / (float) newWidth);
            bQuad.getMeshData().getTextureBuffer(0).put(6, width / (float) newWidth);
            width = newWidth;
        }

        int height = cam.getHeight();
        if (!MathUtils.isPowerOfTwo(height)) {
            int newHeight = 2;
            do {
                newHeight <<= 1;

            } while (newHeight < height);
            bQuad.getMeshData().getTextureBuffer(0).put(1, height / (float) newHeight);
            bQuad.getMeshData().getTextureBuffer(0).put(7, height / (float) newHeight);
            height = newHeight;
        }
        if (bufTexRend == null) {
            bufTexRend = TextureRendererFactory.INSTANCE.createTextureRenderer(width, height, r, ContextManager
                    .getCurrentContext().getCapabilities());
            bufTexRend.setupTexture(bufTexture);
        }
        bufTexRend.copyToTexture(bufTexture, 0, 0, width, height, 0, 0);

        final double loc = size * .75;
        switch (location) {
            case NORTHWEST:
                locationX = loc;
                locationY -= loc;
                break;
            case NORTHEAST:
                locationX -= loc;
                locationY -= loc;
                break;
            case SOUTHEAST:
                locationX -= loc;
                locationY = loc;
                break;
            case SOUTHWEST:
            default:
                locationX = loc;
                locationY = loc;
                break;
        }

        bQuad.setWorldTranslation(locationX, locationY, 0);

        bQuad.updateGeometricState(0);
        bQuad.onDraw(r);
        r.flushGraphics();
    }
}