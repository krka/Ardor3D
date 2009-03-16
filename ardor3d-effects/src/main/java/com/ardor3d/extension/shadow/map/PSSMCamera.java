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

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.bounding.BoundingSphere;
import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.Vector4;
import com.ardor3d.math.type.ReadOnlyMatrix4;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;

/**
 * Camera with additional pssm related functionality.
 */
public class PSSMCamera extends Camera {

    /** The storage place for calculated split distances. */
    protected double _splitDistances[] = new double[2];

    /** The lambda value used in split distance calculations. */
    protected double _lambda = 0.5;

    /** The corners of the camera frustum. */
    protected final Vector3[] _corners = new Vector3[8];

    /** The center of the camera frustum. */
    protected final Vector3 _center = new Vector3();

    /** Temporary vector used for storing extents during corner calculations. */
    protected final Vector3 _extents = new Vector3();

    /** The maximum far plane distance used when packing the frustum. */
    protected double _maxFarPlaneDistance = 2000.0;

    /**
     * Instantiates a new PSSM camera.
     */
    public PSSMCamera() {
        super(0, 0); // copy later
        init();
    }

    /**
     * Instantiates a new PSSM camera.
     * 
     * @param width
     *            the width
     * @param height
     *            the height
     */
    public PSSMCamera(final int width, final int height) {
        super(width, height);
        init();
    }

    /**
     * Instantiates a new PSSM camera.
     * 
     * @param source
     *            the source
     */
    public PSSMCamera(final Camera source) {
        super(source);
        init();
    }

    /**
     * Initialize structures.
     */
    private void init() {
        for (int i = 0; i < _corners.length; i++) {
            _corners[i] = new Vector3();
        }
    }

    /**
     * Calculates the distances from view location for view frustum splits using the "practical split scheme".
     * 
     * @param splitCount
     *            the split count
     * 
     * @see http://appsrv.cse.cuhk.edu.hk/~fzhang/pssm_vrcia/keypoint1.htm
     */
    public void calculateSplitDistances(final int splitCount) {
        // ensure correct size.
        if (_splitDistances.length != splitCount + 1) {
            _splitDistances = new double[splitCount + 1];
        }

        final double nearPlane = getFrustumNear();
        final double farPlane = getFrustumFar();

        // setup intermediate splits
        for (int i = 1; i < splitCount; i++) {
            final double part = i / (double) splitCount;
            final double logsplit = nearPlane * Math.pow((farPlane / nearPlane), part);
            final double uniformSplit = nearPlane + (farPlane - nearPlane) * part;
            _splitDistances[i] = logsplit * _lambda + uniformSplit * (1 - _lambda);
        }

        // setup first and last split (near/far planes)
        _splitDistances[0] = nearPlane;
        _splitDistances[splitCount] = farPlane;
    }

    /**
     * Compress this camera's near and far frustum planes to be smaller if possible, using the given bounds as a
     * measure.
     * 
     * @param sceneBounds
     *            the scene bounds
     */
    public void pack(final BoundingVolume sceneBounds) {
        final ReadOnlyVector3 center = sceneBounds.getCenter();
        for (int i = 0; i < _corners.length; i++) {
            _corners[i].set(center);
        }

        if (sceneBounds instanceof BoundingBox) {
            final BoundingBox bbox = (BoundingBox) sceneBounds;
            bbox.getExtent(_extents);
        } else if (sceneBounds instanceof BoundingSphere) {
            final BoundingSphere bsphere = (BoundingSphere) sceneBounds;
            _extents.set(bsphere.getRadius(), bsphere.getRadius(), bsphere.getRadius());
        }

        _corners[0].addLocal(_extents.getX(), _extents.getY(), _extents.getZ());
        _corners[1].addLocal(_extents.getX(), -_extents.getY(), _extents.getZ());
        _corners[2].addLocal(_extents.getX(), _extents.getY(), -_extents.getZ());
        _corners[3].addLocal(_extents.getX(), -_extents.getY(), -_extents.getZ());
        _corners[4].addLocal(-_extents.getX(), _extents.getY(), _extents.getZ());
        _corners[5].addLocal(-_extents.getX(), -_extents.getY(), _extents.getZ());
        _corners[6].addLocal(-_extents.getX(), _extents.getY(), -_extents.getZ());
        _corners[7].addLocal(-_extents.getX(), -_extents.getY(), -_extents.getZ());

        // final ReadOnlyMatrix4 mvMatrix = ContextManager.getCurrentContext().getCurrentCamera()
        // .getModelViewProjectionMatrix();
        final ReadOnlyMatrix4 mvMatrix = getModelViewProjectionMatrix();
        double optimalCameraNear = Double.MAX_VALUE;
        double optimalCameraFar = -Double.MAX_VALUE;
        final Vector4 position = Vector4.fetchTempInstance();
        for (int i = 0; i < _corners.length; i++) {
            position.set(_corners[i].getX(), _corners[i].getY(), _corners[i].getZ(), 1);
            mvMatrix.applyPre(position, position);

            optimalCameraNear = Math.min(position.getZ(), optimalCameraNear);
            optimalCameraFar = Math.max(position.getZ(), optimalCameraFar);
        }

        optimalCameraNear = Math.min(Math.max(getFrustumNear(), optimalCameraNear), getFrustumFar());
        optimalCameraFar = Math.max(optimalCameraNear, Math.min(getFrustumFar(), optimalCameraFar));

        optimalCameraFar = Math.min(_maxFarPlaneDistance, optimalCameraFar);

        setFrustumNear(optimalCameraNear);
        setFrustumFar(optimalCameraFar);

        Vector4.releaseTempInstance(position);
    }

    /**
     * Calculate frustum corners. Uses Fov version if fov is set.
     * 
     * @param fNear
     *            the near distance
     * @param fFar
     *            the far distance
     */
    public void calculateFrustum(double fNear, double fFar) {
        if (!Double.isNaN(getFovY())) {
            calculateFrustumFov(fNear, fFar);
            return;
        }

        fNear = (fNear - _frustumNear) / (_frustumFar - _frustumNear);
        fFar = (fFar - _frustumNear) / (_frustumFar - _frustumNear);

        fNear = fNear * 2.0 - 1.0;
        fFar = fFar * 2.0 - 1.0;

        _corners[0].set(-1, -1, fNear);
        _corners[1].set(1, -1, fNear);
        _corners[2].set(1, 1, fNear);
        _corners[3].set(-1, 1, fNear);

        _corners[4].set(-1, -1, fFar);
        _corners[5].set(1, -1, fFar);
        _corners[6].set(1, 1, fFar);
        _corners[7].set(-1, 1, fFar);

        final ReadOnlyMatrix4 matrix = getModelViewProjectionInverseMatrix();
        final Vector4 position = Vector4.fetchTempInstance();
        for (int i = 0; i < _corners.length; i++) {
            position.set(_corners[i].getX(), _corners[i].getY(), _corners[i].getZ(), 1);
            matrix.applyPre(position, position);
            position.divideLocal(position.getW());
            _corners[i].set(position.getX(), position.getY(), position.getZ());
        }
        Vector4.releaseTempInstance(position);

        _center.zero();
        for (int i = 0; i < _corners.length; i++) {
            _center.addLocal(_corners[i]);
        }
        _center.divideLocal(8.0);
    }

    /**
     * Calculate frustum corners using fov.
     * 
     * @param fNear
     *            the near distance
     * @param fFar
     *            the far distance
     */
    public void calculateFrustumFov(final double fNear, final double fFar) {
        final double fAspect = getWidth() / (double) getHeight();
        final double fFOV = getFovY();

        final double fNearPlaneHeight = MathUtils.tan(MathUtils.DEG_TO_RAD * fFOV * 0.5) * fNear;
        final double fNearPlaneWidth = fNearPlaneHeight * fAspect;

        final double fFarPlaneHeight = MathUtils.tan(MathUtils.DEG_TO_RAD * fFOV * 0.5) * fFar;
        final double fFarPlaneWidth = fFarPlaneHeight * fAspect;

        final Vector3 vNearPlaneCenter = Vector3.fetchTempInstance();
        final Vector3 vFarPlaneCenter = Vector3.fetchTempInstance();
        final Vector3 direction = Vector3.fetchTempInstance();
        final Vector3 left = Vector3.fetchTempInstance();
        final Vector3 up = Vector3.fetchTempInstance();

        direction.set(getDirection()).multiplyLocal(fNear);
        vNearPlaneCenter.set(getLocation()).addLocal(direction);
        direction.set(getDirection()).multiplyLocal(fFar);
        vFarPlaneCenter.set(getLocation()).addLocal(direction);

        left.set(getLeft()).multiplyLocal(fNearPlaneWidth);
        up.set(getUp()).multiplyLocal(fNearPlaneHeight);
        _corners[0].set(vNearPlaneCenter).subtractLocal(left).subtractLocal(up);
        _corners[1].set(vNearPlaneCenter).subtractLocal(left).addLocal(up);
        _corners[2].set(vNearPlaneCenter).addLocal(left).addLocal(up);
        _corners[3].set(vNearPlaneCenter).addLocal(left).subtractLocal(up);

        left.set(getLeft()).multiplyLocal(fFarPlaneWidth);
        up.set(getUp()).multiplyLocal(fFarPlaneHeight);
        _corners[4].set(vFarPlaneCenter).subtractLocal(left).subtractLocal(up);
        _corners[5].set(vFarPlaneCenter).subtractLocal(left).addLocal(up);
        _corners[6].set(vFarPlaneCenter).addLocal(left).addLocal(up);
        _corners[7].set(vFarPlaneCenter).addLocal(left).subtractLocal(up);

        _center.zero();
        for (int i = 0; i < _corners.length; i++) {
            _center.addLocal(_corners[i]);
        }
        _center.divideLocal(8.0);

        Vector3.releaseTempInstance(vNearPlaneCenter);
        Vector3.releaseTempInstance(vFarPlaneCenter);
        Vector3.releaseTempInstance(direction);
        Vector3.releaseTempInstance(left);
        Vector3.releaseTempInstance(up);
    }

    /**
     * Gets the lambda.
     * 
     * @return the lambda
     */
    public double getLambda() {
        return _lambda;
    }

    /**
     * Sets the lambda.
     * 
     * @param lambda
     *            the new lambda
     */
    public void setLambda(final double lambda) {
        _lambda = lambda;
    }

    /**
     * Gets the split distances.
     * 
     * @return the split distances
     */
    public double[] getSplitDistances() {
        return _splitDistances;
    }

    /**
     * Gets the max far plane distance.
     * 
     * @return the max far plane distance
     */
    public double getMaxFarPlaneDistance() {
        return _maxFarPlaneDistance;
    }

    /**
     * Sets the max far plane distance.
     * 
     * @param maxFarPlaneDistance
     *            the new max far plane distance
     */
    public void setMaxFarPlaneDistance(final double maxFarPlaneDistance) {
        _maxFarPlaneDistance = maxFarPlaneDistance;
    }
}
