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
    private static final long serialVersionUID = 1L;

    private final int sizeX;
    private final int sizeY;

    // x/z step
    private static Vector3 calcVec1 = new Vector3();
    private static Vector3 calcVec2 = new Vector3();
    private static Vector3 calcVec3 = new Vector3();

    private FloatBuffer vertBuf;
    private FloatBuffer normBuf;
    private FloatBuffer texs;
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
    private final Vector4 pointTop = new Vector4();
    private final Vector4 pointBottom = new Vector4();
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

    public ProjectedGrid(final String name, final Camera cam, final int sizeX, final int sizeY,
            final float textureScale, final HeightGenerator heightGenerator, final Timer timer) {
        super(name);
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.textureScale = textureScale;
        this.heightGenerator = heightGenerator;
        camera = cam;

        this.timer = timer;

        buildVertices(sizeX * sizeY);
        buildTextureCoordinates();
        buildNormals();

        vertBufArray = new float[_meshData.getVertexCount() * 3];
        normBufArray = new float[_meshData.getVertexCount() * 3];
        texBufArray = new float[_meshData.getVertexCount() * 2];
    }

    public void switchFreeze() {
        freezeProjector = !freezeProjector;
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

        final double time = timer.getTimeInSeconds();

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

        vertBuf.rewind();
        final double du = 1.0f / (double) (sizeX - 1);
        final double dv = 1.0f / (double) (sizeY - 1);
        double u = 0, v = 0;
        int index = 0;
        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                interpolate(intersectTopLeft, intersectTopRight, u, pointTop);
                interpolate(intersectBottomLeft, intersectBottomRight, u, pointBottom);
                interpolate(pointTop, pointBottom, v, pointFinal);

                pointFinal.setX(pointFinal.getX() / pointFinal.getW());
                pointFinal.setZ(pointFinal.getZ() / pointFinal.getW());
                realPoint.set(pointFinal.getX(), heightGenerator.getHeight(pointFinal.getX(), pointFinal.getZ(), time),
                        pointFinal.getZ());

                vertBufArray[index++] = (float) realPoint.getX();
                vertBufArray[index++] = (float) realPoint.getY();
                vertBufArray[index++] = (float) realPoint.getZ();

                u += du;
            }
            v += dv;
            u = 0;
        }
        vertBuf.put(vertBufArray);

        texs.rewind();
        for (int i = 0; i < _meshData.getVertexCount(); i++) {
            texBufArray[i * 2] = vertBufArray[i * 3] * textureScale;
            texBufArray[i * 2 + 1] = vertBufArray[i * 3 + 2] * textureScale;
        }
        texs.put(texBufArray);

        normBuf.rewind();
        oppositePoint.set(0, 0, 0);
        adjacentPoint.set(0, 0, 0);
        rootPoint.set(0, 0, 0);
        tempNorm.set(0, 0, 0);
        int adj = 0, opp = 0, normalIndex = 0;
        for (int row = 0; row < sizeY; row++) {
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
        normBuf.put(normBufArray);
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
     * <code>setDetailTexture</code> copies the texture coordinates from the first texture channel to another channel
     * specified by unit, mulitplying by the factor specified by repeat so that the texture in that channel will be
     * repeated that many times across the block.
     * 
     * @param unit
     *            channel to copy coords to
     * @param repeat
     *            number of times to repeat the texture across and down the block
     */
    public void setDetailTexture(final int unit, final double repeat) {
    // TODO
    // copyTextureCoordinates(0, unit, repeat);
    }

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

        final Vector3 topLeft = store, topRight = calcVec1, bottomLeft = calcVec2, bottomRight = calcVec3;

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

    /**
     * <code>buildTextureCoordinates</code> calculates the texture coordinates of the terrain.
     */
    private void buildTextureCoordinates() {
        texs = BufferUtils.createVector2Buffer(_meshData.getVertexCount());
        _meshData.setTextureBuffer(texs, 0);
        texs.clear();

        vertBuf.rewind();
        for (int i = 0; i < _meshData.getVertexCount(); i++) {
            texs.put(vertBuf.get() * textureScale);
            vertBuf.get(); // ignore vert y coord.
            texs.put(vertBuf.get() * textureScale);
        }
    }

    /**
     * <code>buildNormals</code> calculates the normals of each vertex that makes up the block of terrain.
     */
    Vector3 oppositePoint = new Vector3();
    Vector3 adjacentPoint = new Vector3();
    Vector3 rootPoint = new Vector3();
    Vector3 tempNorm = new Vector3();

    private void buildNormals() {
        normBuf = BufferUtils.createVector3Buffer(normBuf, _meshData.getVertexCount());
        _meshData.setNormalBuffer(normBuf);

        oppositePoint.set(0, 0, 0);
        adjacentPoint.set(0, 0, 0);
        rootPoint.set(0, 0, 0);
        tempNorm.set(0, 0, 0);
        int adj = 0, opp = 0, normalIndex = 0;
        for (int row = 0; row < sizeY; row++) {
            for (int col = 0; col < sizeX; col++) {
                BufferUtils.populateFromBuffer(rootPoint, vertBuf, normalIndex);
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
                    if (col == sizeY - 1) { // last column except for last row
                        // left cross down
                        adj = normalIndex - 1;
                        opp = normalIndex + sizeX;
                    } else { // most cases
                        // down cross right
                        adj = normalIndex + sizeX;
                        opp = normalIndex + 1;
                    }
                }
                BufferUtils.populateFromBuffer(adjacentPoint, vertBuf, adj);
                BufferUtils.populateFromBuffer(oppositePoint, vertBuf, opp);
                tempNorm.set(adjacentPoint).subtractLocal(rootPoint).crossLocal(oppositePoint.subtractLocal(rootPoint))
                        .normalizeLocal();
                BufferUtils.setInBuffer(tempNorm, normBuf, normalIndex);
                normalIndex++;
            }
        }
    }
}
