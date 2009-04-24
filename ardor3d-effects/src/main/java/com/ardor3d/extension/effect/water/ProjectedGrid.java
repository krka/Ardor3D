/**
 * Copyright (c) 2008 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.effect.water;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.Vector4;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.Timer;
import com.ardor3d.util.geom.BufferUtils;

/**
 * <code>ProjectedGrid</code> Projected grid mesh
 */
public class ProjectedGrid extends Mesh {
    /** The Constant logger. */
    private static final Logger logger = Logger.getLogger(ProjectedGrid.class.getName());

    private static final long serialVersionUID = 1L;

    private final int sizeX;
    private final int sizeY;

    private FloatBuffer vertBuf;
    private final FloatBuffer normBuf;
    private final FloatBuffer texs;
    private IntBuffer indexBuffer;

    private double viewPortWidth = 0;
    private double viewPortHeight = 0;
    private double viewPortLeft = 0;
    private double viewPortBottom = 0;

    private final Vector4 origin = new Vector4();
    private final Vector4 direction = new Vector4();
    private final Vector2 source = new Vector2();

    private final Matrix4 modelViewMatrix = new Matrix4();
    private final Matrix4 projectionMatrix = new Matrix4();
    private final Matrix4 modelViewProjectionInverse = new Matrix4();
    private final Vector4 intersectBottomLeft = new Vector4();
    private final Vector4 intersectTopLeft = new Vector4();
    private final Vector4 intersectTopRight = new Vector4();
    private final Vector4 intersectBottomRight = new Vector4();

    private final Matrix4 modelViewMatrix1 = new Matrix4();
    private final Matrix4 projectionMatrix1 = new Matrix4();
    private final Matrix4 modelViewProjection1 = new Matrix4();
    private final Matrix4 modelViewProjectionInverse1 = new Matrix4();
    private final Vector4 intersectBottomLeft1 = new Vector4();
    private final Vector4 intersectTopLeft1 = new Vector4();
    private final Vector4 intersectTopRight1 = new Vector4();
    private final Vector4 intersectBottomRight1 = new Vector4();

    private final Vector3 camloc = new Vector3();
    private final Vector3 camdir = new Vector3();
    private final Vector4 pointFinal = new Vector4();
    private final Vector3 realPoint = new Vector3();

    public boolean freezeProjector = false;
    public boolean useReal = false;
    private final Vector3 projectorLoc = new Vector3();
    private final Timer timer;
    private final Camera camera;

    private final HeightGenerator heightGenerator;
    private final float textureScale;

    private final float[] vertBufArray;
    private final float[] normBufArray;
    private final float[] texBufArray;

    private int nrUpdateThreads = 1;
    private final ExecutorService executorService = Executors.newCachedThreadPool(new DeamonThreadFactory());
    private final Stack<Future<?>> futureStack = new Stack<Future<?>>();

    public ProjectedGrid(final String name, final Camera camera, final int sizeX, final int sizeY,
            final float textureScale, final HeightGenerator heightGenerator, final Timer timer) {
        super(name);
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.textureScale = textureScale;
        this.heightGenerator = heightGenerator;
        this.camera = camera;
        this.timer = timer;

        buildVertices(sizeX * sizeY);
        texs = BufferUtils.createVector2Buffer(_meshData.getVertexCount());
        _meshData.setTextureBuffer(texs, 0);
        normBuf = BufferUtils.createVector3Buffer(_meshData.getVertexCount());
        _meshData.setNormalBuffer(normBuf);

        vertBufArray = new float[_meshData.getVertexCount() * 3];
        normBufArray = new float[_meshData.getVertexCount() * 3];
        texBufArray = new float[_meshData.getVertexCount() * 2];
    }

    public void setNrUpdateThreads(final int nrUpdateThreads) {
        this.nrUpdateThreads = nrUpdateThreads;
        if (this.nrUpdateThreads < 1) {
            this.nrUpdateThreads = 1;
        }
    }

    public int getNrUpdateThreads() {
        return nrUpdateThreads;
    }

    public void setFreezeUpdate(final boolean freeze) {
        freezeProjector = freeze;
    }

    public boolean isFreezeUpdate() {
        return freezeProjector;
    }

    @Override
    public void draw(final Renderer r) {
        update();
        super.draw(r);
    }

    public void update() {
        if (freezeProjector) {
            return;
        }

        camloc.set(camera.getLocation());
        camdir.set(camera.getDirection());

        viewPortWidth = camera.getWidth();
        viewPortHeight = camera.getHeight();
        viewPortLeft = camera.getViewPortLeft();
        viewPortBottom = camera.getViewPortBottom();
        modelViewMatrix.set(camera.getModelViewMatrix());
        projectionMatrix.set(camera.getProjectionMatrix());
        modelViewProjectionInverse.set(modelViewMatrix).multiplyLocal(projectionMatrix);
        modelViewProjectionInverse.invertLocal();

        source.set(0.5, 0.5);
        getWorldIntersection(source, modelViewProjectionInverse, pointFinal);
        pointFinal.multiplyLocal(1.0 / pointFinal.getW());
        realPoint.set(pointFinal.getX(), pointFinal.getY(), pointFinal.getZ());
        projectorLoc.set(camera.getLocation());
        realPoint.set(projectorLoc).addLocal(camera.getDirection());

        Matrix4 rangeMatrix = null;
        if (useReal) {
            final Vector3 fakeLoc = new Vector3(projectorLoc);
            final Vector3 fakePoint = new Vector3(realPoint);
            fakeLoc.addLocal(0, 1000, 0);

            rangeMatrix = getMinMax(fakeLoc, fakePoint, camera);
        }

        MathUtils.matrixLookAt(projectorLoc, realPoint, Vector3.UNIT_Y, modelViewMatrix);
        MathUtils.matrixPerspective(camera.getFovY() + 10.0f, viewPortWidth / viewPortHeight, camera.getFrustumNear(),
                camera.getFrustumFar(), projectionMatrix);
        modelViewProjectionInverse.set(modelViewMatrix).multiplyLocal(projectionMatrix);
        modelViewProjectionInverse.invertLocal();

        if (useReal && rangeMatrix != null) {
            rangeMatrix.multiplyLocal(modelViewProjectionInverse);
            modelViewProjectionInverse.set(rangeMatrix);
        }

        source.set(0, 0);
        getWorldIntersection(source, modelViewProjectionInverse, intersectBottomLeft);
        source.set(0, 1);
        getWorldIntersection(source, modelViewProjectionInverse, intersectTopLeft);
        source.set(1, 1);
        getWorldIntersection(source, modelViewProjectionInverse, intersectTopRight);
        source.set(1, 0);
        getWorldIntersection(source, modelViewProjectionInverse, intersectBottomRight);

        if (nrUpdateThreads <= 1) {
            updateGrid(0, sizeY);
        } else {
            for (int i = 0; i < nrUpdateThreads; i++) {
                final int from = sizeY * i / (nrUpdateThreads);
                final int to = sizeY * (i + 1) / (nrUpdateThreads);
                final Future<?> future = executorService.submit(new Runnable() {
                    public void run() {
                        updateGrid(from, to);
                    }
                });
                futureStack.push(future);
            }
            try {
                while (!futureStack.isEmpty()) {
                    futureStack.pop().get();
                }
            } catch (final InterruptedException ex) {
                logger.log(Level.SEVERE, "InterruptedException in thread execution", ex);
            } catch (final ExecutionException ex) {
                logger.log(Level.SEVERE, "ExecutionException in thread execution", ex);
            }
        }

        vertBuf.rewind();
        vertBuf.put(vertBufArray);

        texs.rewind();
        texs.put(texBufArray);

        normBuf.rewind();
        normBuf.put(normBufArray);
    }

    private void updateGrid(final int from, final int to) {
        final double time = timer.getTimeInSeconds();
        final double du = 1.0f / (double) (sizeX - 1);
        final double dv = 1.0f / (double) (sizeY - 1);

        final Vector4 pointTop = Vector4.fetchTempInstance();
        final Vector4 pointFinal = Vector4.fetchTempInstance();
        final Vector4 pointBottom = Vector4.fetchTempInstance();

        int smallerFrom = from;
        if (smallerFrom > 0) {
            smallerFrom--;
        }
        int biggerTo = to;
        if (biggerTo < sizeY) {
            biggerTo++;
        }
        double u = 0, v = smallerFrom * dv;
        int index = smallerFrom * sizeX * 3;
        for (int y = smallerFrom; y < biggerTo; y++) {
            for (int x = 0; x < sizeX; x++) {
                interpolate(intersectTopLeft, intersectTopRight, u, pointTop);
                interpolate(intersectBottomLeft, intersectBottomRight, u, pointBottom);
                interpolate(pointTop, pointBottom, v, pointFinal);

                pointFinal.setX(pointFinal.getX() / pointFinal.getW());
                pointFinal.setZ(pointFinal.getZ() / pointFinal.getW());
                pointFinal.setY(heightGenerator.getHeight(pointFinal.getX(), pointFinal.getZ(), time));

                vertBufArray[index++] = (float) pointFinal.getX();
                vertBufArray[index++] = (float) pointFinal.getY();
                vertBufArray[index++] = (float) pointFinal.getZ();

                u += du;
            }
            v += dv;
            u = 0;
        }

        Vector4.releaseTempInstance(pointTop);
        Vector4.releaseTempInstance(pointFinal);
        Vector4.releaseTempInstance(pointBottom);

        index = from * sizeX;
        for (int y = from; y < to; y++) {
            for (int x = 0; x < sizeX; x++) {
                texBufArray[index * 2] = vertBufArray[index * 3] * textureScale;
                texBufArray[index * 2 + 1] = vertBufArray[index * 3 + 2] * textureScale;
                index++;
            }
        }

        final Vector3 oppositePoint = Vector3.fetchTempInstance();
        final Vector3 adjacentPoint = Vector3.fetchTempInstance();
        final Vector3 rootPoint = Vector3.fetchTempInstance();
        final Vector3 tempNorm = Vector3.fetchTempInstance();

        int adj = 0, opp = 0;
        int normalIndex = from * sizeX;
        for (int row = from; row < to; row++) {
            for (int col = 0; col < sizeX; col++) {
                if (row == sizeY - 1) {
                    if (col == sizeX - 1) { // last row, last col
                        // up cross left
                        adj = normalIndex - sizeX;
                        opp = normalIndex - 1;
                    } else { // last row, except for last col
                        // right cross up
                        adj = normalIndex + 1;
                        opp = normalIndex - sizeX;
                    }
                } else {
                    if (col == sizeX - 1) { // last column except for last row
                        // left cross down
                        adj = normalIndex - 1;
                        opp = normalIndex + sizeX;
                    } else { // most cases
                        // down cross right
                        adj = normalIndex + sizeX;
                        opp = normalIndex + 1;
                    }
                }
                rootPoint.set(vertBufArray[normalIndex * 3], vertBufArray[normalIndex * 3 + 1],
                        vertBufArray[normalIndex * 3 + 2]);
                adjacentPoint.set(vertBufArray[adj * 3], vertBufArray[adj * 3 + 1], vertBufArray[adj * 3 + 2]);
                oppositePoint.set(vertBufArray[opp * 3], vertBufArray[opp * 3 + 1], vertBufArray[opp * 3 + 2]);
                tempNorm.set(adjacentPoint).subtractLocal(rootPoint).crossLocal(oppositePoint.subtractLocal(rootPoint))
                        .normalizeLocal();

                normBufArray[normalIndex * 3] = (float) tempNorm.getX();
                normBufArray[normalIndex * 3 + 1] = (float) tempNorm.getY();
                normBufArray[normalIndex * 3 + 2] = (float) tempNorm.getZ();

                normalIndex++;
            }
        }

        Vector3.releaseTempInstance(oppositePoint);
        Vector3.releaseTempInstance(adjacentPoint);
        Vector3.releaseTempInstance(rootPoint);
        Vector3.releaseTempInstance(tempNorm);
    }

    private Matrix4 getMinMax(final Vector3 fakeLoc, final Vector3 fakePoint, final Camera cam) {
        Matrix4 rangeMatrix;
        MathUtils.matrixLookAt(fakeLoc, fakePoint, Vector3.UNIT_Y, modelViewMatrix1);
        MathUtils.matrixPerspective(camera.getFovY(), viewPortWidth / viewPortHeight, cam.getFrustumNear(), cam
                .getFrustumFar(), projectionMatrix1);
        modelViewProjection1.set(modelViewMatrix1).multiplyLocal(projectionMatrix1);
        modelViewProjectionInverse1.set(modelViewProjection1).invertLocal();

        source.set(0, 0);
        getWorldIntersection(source, modelViewProjectionInverse, intersectBottomLeft1);
        source.set(0, 1);
        getWorldIntersection(source, modelViewProjectionInverse, intersectTopLeft1);
        source.set(1, 1);
        getWorldIntersection(source, modelViewProjectionInverse, intersectTopRight1);
        source.set(1, 0);
        getWorldIntersection(source, modelViewProjectionInverse, intersectBottomRight1);

        modelViewProjection1.applyPre(intersectBottomLeft1, intersectBottomLeft1);
        modelViewProjection1.applyPre(intersectTopLeft1, intersectTopLeft1);
        modelViewProjection1.applyPre(intersectTopRight1, intersectTopRight1);
        modelViewProjection1.applyPre(intersectBottomRight1, intersectBottomRight1);

        double minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;
        minX = Math.min(intersectBottomLeft1.getX(), minX);
        minX = Math.min(intersectTopLeft1.getX(), minX);
        minX = Math.min(intersectTopRight1.getX(), minX);
        minX = Math.min(intersectBottomRight1.getX(), minX);

        maxX = Math.max(intersectBottomLeft1.getX(), maxX);
        maxX = Math.max(intersectTopLeft1.getX(), maxX);
        maxX = Math.max(intersectTopRight1.getX(), maxX);
        maxX = Math.max(intersectBottomRight1.getX(), maxX);

        minY = Math.min(intersectBottomLeft1.getY(), minY);
        minY = Math.min(intersectTopLeft1.getY(), minY);
        minY = Math.min(intersectTopRight1.getY(), minY);
        minY = Math.min(intersectBottomRight1.getY(), minY);

        maxY = Math.max(intersectBottomLeft1.getY(), maxY);
        maxY = Math.max(intersectTopLeft1.getY(), maxY);
        maxY = Math.max(intersectTopRight1.getY(), maxY);
        maxY = Math.max(intersectBottomRight1.getY(), maxY);

        rangeMatrix = new Matrix4(maxX - minX, 0, 0, minX, 0, maxY - minY, 0, minY, 0, 0, 1, 0, 0, 0, 0, 1);
        rangeMatrix.transposeLocal();
        return rangeMatrix;
    }

    private void interpolate(final Vector4 beginVec, final Vector4 finalVec, final double changeAmnt,
            final Vector4 resultVec) {
        resultVec.lerpLocal(beginVec, finalVec, changeAmnt);

        // resultVec.x = (1 - changeAmnt) * beginVec.x + changeAmnt * finalVec.x;
        // // resultVec.y = (1 - changeAmnt) * beginVec.y + changeAmnt * finalVec.y;
        // resultVec.z = (1 - changeAmnt) * beginVec.z + changeAmnt * finalVec.z;
        // resultVec.w = (1 - changeAmnt) * beginVec.w + changeAmnt * finalVec.w;
    }

    private void interpolate(final Vector3 beginVec, final Vector3 finalVec, final double changeAmnt,
            final Vector3 resultVec) {
        resultVec.lerpLocal(beginVec, finalVec, changeAmnt);
    }

    private void getWorldIntersection(final Vector2 screenPosition, final Matrix4 viewProjectionMatrix,
            final Vector4 store) {
        origin.set(screenPosition.getX() * 2 - 1, screenPosition.getY() * 2 - 1, -1, 1);
        direction.set(screenPosition.getX() * 2 - 1, screenPosition.getY() * 2 - 1, 1, 1);

        viewProjectionMatrix.applyPre(origin, origin);
        viewProjectionMatrix.applyPre(direction, direction);

        if (camera.getLocation().getY() > 0) {
            if (direction.getY() > 0) {
                direction.setY(0);
            }
        } else {
            if (direction.getY() < 0) {
                direction.setY(0);
            }
        }

        direction.subtractLocal(origin);

        final double t = -origin.getY() / direction.getY();

        direction.multiplyLocal(t);
        store.set(origin);
        store.addLocal(direction);
    }

    private double homogenousIntersect(final Quaternion a, final Quaternion xa, final Quaternion xb) {
        // double tx = -xb.w*(dotXYZ(a.xyz,xa.xyz)+xa.w*a.w);
        // double tw = dotXYZ(a,xa.w*xb.xyz-xb.w*xa.xyz);
        // return tx/tw;
        return 0;
    }

    // private double dotXYZ(final Quaternion a, final Quaternion b) {
    // return a.x * b.x + a.y * b.y + a.z * b.z;
    // }

    /**
     * <code>getSurfaceNormal</code> returns the normal of an arbitrary point on the terrain. The normal is linearly
     * interpreted from the normals of the 4 nearest defined points. If the point provided is not within the bounds of
     * the height map, null is returned.
     * 
     * @param position
     *            the vector representing the location to find a normal at.
     * @param store
     *            the Vector3 object to store the result in. If null, a new one is created.
     * @return the normal vector at the provided location.
     */
    public Vector3 getSurfaceNormal(final Vector2 position, final Vector3 store) {
        return getSurfaceNormal(position.getX(), position.getY(), store);
    }

    /**
     * <code>getSurfaceNormal</code> returns the normal of an arbitrary point on the terrain. The normal is linearly
     * interpreted from the normals of the 4 nearest defined points. If the point provided is not within the bounds of
     * the height map, null is returned.
     * 
     * @param position
     *            the vector representing the location to find a normal at. Only the x and z values are used.
     * @param store
     *            the Vector3 object to store the result in. If null, a new one is created.
     * @return the normal vector at the provided location.
     */
    public Vector3 getSurfaceNormal(final Vector3 position, final Vector3 store) {
        return getSurfaceNormal(position.getX(), position.getZ(), store);
    }

    /**
     * <code>getSurfaceNormal</code> returns the normal of an arbitrary point on the terrain. The normal is linearly
     * interpreted from the normals of the 4 nearest defined points. If the point provided is not within the bounds of
     * the height map, null is returned.
     * 
     * @param x
     *            the x coordinate to check.
     * @param z
     *            the z coordinate to check.
     * @param store
     *            the Vector3 object to store the result in. If null, a new one is created.
     * @return the normal unit vector at the provided location.
     */
    public Vector3 getSurfaceNormal(final double x, final double z, Vector3 store) {
        // x /= stepScale.x;
        // z /= stepScale.z;
        final double col = Math.floor(x);
        final double row = Math.floor(z);

        if (col < 0 || row < 0 || col >= sizeX - 1 || row >= sizeY - 1) {
            return null;
        }
        final double intOnX = x - col, intOnZ = z - row;

        if (store == null) {
            store = new Vector3();
        }

        final Vector3 topLeft = store, topRight = new Vector3(), bottomLeft = new Vector3(), bottomRight = new Vector3();

        final int focalSpot = (int) (col + row * sizeX);

        // find the heightmap point closest to this position (but will always
        // be to the left ( < x) and above (< z) of the spot.
        BufferUtils.populateFromBuffer(topLeft, normBuf, focalSpot);

        // now find the next point to the right of topLeft's position...
        BufferUtils.populateFromBuffer(topRight, normBuf, focalSpot + 1);

        // now find the next point below topLeft's position...
        BufferUtils.populateFromBuffer(bottomLeft, normBuf, focalSpot + sizeX);

        // now find the next point below and to the right of topLeft's
        // position...
        BufferUtils.populateFromBuffer(bottomRight, normBuf, focalSpot + sizeX + 1);

        // Use linear interpolation to find the height.
        topLeft.lerpLocal(topRight, intOnX);
        bottomLeft.lerpLocal(bottomRight, intOnX);
        topLeft.lerpLocal(bottomLeft, intOnZ);
        return topLeft.normalizeLocal();
    }

    /**
     * <code>buildVertices</code> sets up the vertex and index arrays of the TriMesh.
     */
    private void buildVertices(final int vertexCount) {
        vertBuf = BufferUtils.createVector3Buffer(vertBuf, vertexCount);
        _meshData.setVertexBuffer(vertBuf);

        final Vector3 point = new Vector3();
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                point.set(x, 0, y);
                BufferUtils.setInBuffer(point, vertBuf, (x + (y * sizeX)));
            }
        }

        // set up the indices
        final int triangleQuantity = ((sizeX - 1) * (sizeY - 1)) * 2;
        indexBuffer = BufferUtils.createIntBuffer(triangleQuantity * 3);
        _meshData.setIndexBuffer(indexBuffer);

        // go through entire array up to the second to last column.
        for (int i = 0; i < (sizeX * (sizeY - 1)); i++) {
            // we want to skip the top row.
            if (i % ((sizeX * (i / sizeX + 1)) - 1) == 0 && i != 0) {
                // logger.info("skip row: "+i+" cause: "+((sizeY * (i / sizeX + 1)) - 1));
                continue;
            } else {
                // logger.info("i: "+i);
            }
            // set the top left corner.
            indexBuffer.put(i);
            // set the bottom right corner.
            indexBuffer.put((1 + sizeX) + i);
            // set the top right corner.
            indexBuffer.put(1 + i);
            // set the top left corner
            indexBuffer.put(i);
            // set the bottom left corner
            indexBuffer.put(sizeX + i);
            // set the bottom right corner
            indexBuffer.put((1 + sizeX) + i);
        }
    }

    static class DeamonThreadFactory implements ThreadFactory {
        static final AtomicInteger poolNumber = new AtomicInteger(1);
        final ThreadGroup group;
        final AtomicInteger threadNumber = new AtomicInteger(1);
        final String namePrefix;

        DeamonThreadFactory() {
            final SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = "ProjectedGrid Pool-" + poolNumber.getAndIncrement() + "-thread-";
        }

        public Thread newThread(final Runnable r) {
            final Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (!t.isDaemon()) {
                t.setDaemon(true);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
}
