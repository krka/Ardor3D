/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.DoubleBuffer;
import java.util.logging.Logger;

import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Plane;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.Vector4;
import com.ardor3d.math.type.ReadOnlyMatrix3;
import com.ardor3d.math.type.ReadOnlyMatrix4;
import com.ardor3d.math.type.ReadOnlyVector2;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.geom.BufferUtils;

/**
 * This class represents a view into a 3d scene and how that view should map to a 2D rendering surface.
 */
public class Camera implements Savable, Externalizable, Cloneable {

    private static final long serialVersionUID = 1L;

    private static final Logger _logger = Logger.getLogger(Camera.class.getName());

    public enum FrustumIntersect {
        Outside, Inside, Intersects;
    }

    // planes of the frustum
    /**
     * LEFT_PLANE represents the left plane of the camera frustum.
     */
    public static final int LEFT_PLANE = 0;

    /**
     * RIGHT_PLANE represents the right plane of the camera frustum.
     */
    public static final int RIGHT_PLANE = 1;

    /**
     * BOTTOM_PLANE represents the bottom plane of the camera frustum.
     */
    public static final int BOTTOM_PLANE = 2;

    /**
     * TOP_PLANE represents the top plane of the camera frustum.
     */
    public static final int TOP_PLANE = 3;

    /**
     * FAR_PLANE represents the far plane of the camera frustum.
     */
    public static final int FAR_PLANE = 4;

    /**
     * NEAR_PLANE represents the near plane of the camera frustum.
     */
    public static final int NEAR_PLANE = 5;

    /**
     * FRUSTUM_PLANES represents the number of planes of the camera frustum.
     */
    public static final int FRUSTUM_PLANES = 6;

    /**
     * MAX_WORLD_PLANES holds the maximum planes allowed by the system.
     */
    public static final int MAX_WORLD_PLANES = 32;

    // the location and orientation of the camera.
    /**
     * Camera's location
     */
    protected final Vector3 _location = new Vector3();

    /**
     * Direction of camera's 'left'
     */
    protected final Vector3 _left = new Vector3();

    /**
     * Direction of 'up' for camera.
     */
    protected final Vector3 _up = new Vector3();

    /**
     * Direction the camera is facing.
     */
    protected final Vector3 _direction = new Vector3();

    /**
     * The near range for mapping depth values from normalized device coordinates to window coordinates.
     */
    protected double _depthRangeNear;

    /**
     * The far range for mapping depth values from normalized device coordinates to window coordinates.
     */
    protected double _depthRangeFar;

    /**
     * Distance from camera to near frustum plane.
     */
    protected double _frustumNear;

    /**
     * Distance from camera to far frustum plane.
     */
    protected double _frustumFar;

    /**
     * Distance from camera to left frustum plane.
     */
    protected double _frustumLeft;

    /**
     * Distance from camera to right frustum plane.
     */
    protected double _frustumRight;

    /**
     * Distance from camera to top frustum plane.
     */
    protected double _frustumTop;

    /**
     * Distance from camera to bottom frustum plane.
     */
    protected double _frustumBottom;

    /**
     * Convenience store for fovY. Only set during setFrustumPerspective and never used. Retrieve by getFovY(). Default
     * is NaN.
     */
    protected double _fovY = Double.NaN;

    // Temporary values computed in onFrustumChange that are needed if a
    // call is made to onFrameChange.
    protected double _coeffLeft[];

    protected double _coeffRight[];

    protected double _coeffBottom[];

    protected double _coeffTop[];

    protected int _planeQuantity;

    // view port coordinates
    /**
     * Percent value on display where horizontal viewing starts for this camera. Default is 0.
     */
    protected double _viewPortLeft;

    /**
     * Percent value on display where horizontal viewing ends for this camera. Default is 1.
     */
    protected double _viewPortRight;

    /**
     * Percent value on display where vertical viewing ends for this camera. Default is 1.
     */
    protected double _viewPortTop;

    /**
     * Percent value on display where vertical viewing begins for this camera. Default is 0.
     */
    protected double _viewPortBottom;

    /**
     * Array holding the planes that this camera will check for culling.
     */
    protected Plane[] _worldPlane;

    protected final DoubleBuffer _matrixBuffer = BufferUtils.createDoubleBuffer(16);

    /**
     * Computation vector used in lookAt operations.
     */
    protected Vector3 _newDirection = new Vector3();

    /**
     * store the value for field parallelProjection
     */
    private boolean _parallelProjection;

    private boolean _updateMVMatrix = true;
    private boolean _updatePMatrix = true;
    private boolean _updateMVPMatrix = true;
    private boolean _updateInverseMVPMatrix = true;

    protected final Matrix4 _modelView = new Matrix4();
    protected final Matrix4 _projection = new Matrix4();
    private final Matrix4 _modelViewProjection = new Matrix4();
    private final Matrix4 _modelViewProjectionInverse = new Matrix4();

    protected final Matrix4 _transMatrix = new Matrix4();

    protected boolean _depthRangeDirty;
    protected boolean _frustumDirty;
    protected boolean _viewPortDirty;
    protected boolean _frameDirty;

    /**
     * A mask value set during contains() that allows fast culling of a Node's children.
     */
    private int _planeState;

    protected int _width;
    protected int _height;

    public Camera(final int width, final int height) {
        _width = width;
        _height = height;

        _location.set(0, 0, 0);
        _left.set(1, 0, 0);
        _up.set(0, 1, 0);
        _direction.set(0, 0, 1);

        _depthRangeNear = 0.0;
        _depthRangeFar = 1.0;
        _depthRangeDirty = true;

        _frustumNear = 1.0;
        _frustumFar = 2.0;
        _frustumLeft = -0.5;
        _frustumRight = 0.5;
        _frustumTop = 0.5;
        _frustumBottom = -0.5;

        _coeffLeft = new double[2];
        _coeffRight = new double[2];
        _coeffBottom = new double[2];
        _coeffTop = new double[2];

        _viewPortLeft = 0.0;
        _viewPortRight = 1.0;
        _viewPortTop = 1.0;
        _viewPortBottom = 0.0;

        _planeQuantity = 6;

        _worldPlane = new Plane[MAX_WORLD_PLANES];
        for (int i = 0; i < MAX_WORLD_PLANES; i++) {
            _worldPlane[i] = new Plane();
        }

        onFrustumChange();
        onViewPortChange();
        onFrameChange();

        _logger.fine("Camera created. W: " + width + "  H: " + height);
    }

    public Camera(final Camera source) {
        set(source);

        _logger.fine("Camera created. W: " + getWidth() + "  H: " + getHeight());
    }

    /**
     * Copy the source camera's fields to this camera
     * 
     * @param source
     *            the camera to copy from
     */
    public void set(final Camera source) {
        _width = source.getWidth();
        _height = source.getHeight();

        _location.set(source.getLocation());
        _left.set(source.getLeft());
        _up.set(source.getUp());
        _direction.set(source.getDirection());

        _depthRangeNear = source.getDepthRangeNear();
        _depthRangeFar = source.getDepthRangeFar();
        _depthRangeDirty = true;

        _frustumNear = source.getFrustumNear();
        _frustumFar = source.getFrustumFar();
        _frustumLeft = source.getFrustumLeft();
        _frustumRight = source.getFrustumRight();
        _frustumTop = source.getFrustumTop();
        _frustumBottom = source.getFrustumBottom();

        _coeffLeft = new double[2];
        _coeffRight = new double[2];
        _coeffBottom = new double[2];
        _coeffTop = new double[2];

        _viewPortLeft = source.getViewPortLeft();
        _viewPortRight = source.getViewPortRight();
        _viewPortTop = source.getViewPortTop();
        _viewPortBottom = source.getViewPortBottom();

        _planeQuantity = 6;

        _worldPlane = new Plane[MAX_WORLD_PLANES];
        for (int i = 0; i < MAX_WORLD_PLANES; i++) {
            _worldPlane[i] = new Plane();
        }

        onFrustumChange();
        onViewPortChange();
        onFrameChange();
    }

    public double getDepthRangeFar() {
        return _depthRangeFar;
    }

    /**
     * @param depthRangeNear
     *            the far clipping plane for window coordinates. Should be in the range [0, 1]. Default is 1.
     */
    public void setDepthRangeFar(final double depthRangeFar) {
        _depthRangeFar = depthRangeFar;
        _depthRangeDirty = true;
    }

    public double getDepthRangeNear() {
        return _depthRangeNear;
    }

    /**
     * @param depthRangeNear
     *            the near clipping plane for window coordinates. Should be in the range [0, 1]. Default is 0.
     */
    public void setDepthRangeNear(final double depthRangeNear) {
        _depthRangeNear = depthRangeNear;
        _depthRangeDirty = true;
    }

    /**
     * <code>getFrustumBottom</code> returns the value of the bottom frustum plane.
     * 
     * @return the value of the bottom frustum plane.
     */
    public double getFrustumBottom() {
        return _frustumBottom;
    }

    /**
     * <code>setFrustumBottom</code> sets the value of the bottom frustum plane.
     * 
     * @param frustumBottom
     *            the value of the bottom frustum plane.
     */
    public void setFrustumBottom(final double frustumBottom) {
        _frustumBottom = frustumBottom;
        onFrustumChange();
    }

    /**
     * <code>getFrustumFar</code> gets the value of the far frustum plane.
     * 
     * @return the value of the far frustum plane.
     */
    public double getFrustumFar() {
        return _frustumFar;
    }

    /**
     * <code>setFrustumFar</code> sets the value of the far frustum plane.
     * 
     * @param frustumFar
     *            the value of the far frustum plane.
     */
    public void setFrustumFar(final double frustumFar) {
        _frustumFar = frustumFar;
        onFrustumChange();
    }

    /**
     * <code>getFrustumLeft</code> gets the value of the left frustum plane.
     * 
     * @return the value of the left frustum plane.
     */
    public double getFrustumLeft() {
        return _frustumLeft;
    }

    /**
     * <code>setFrustumLeft</code> sets the value of the left frustum plane.
     * 
     * @param frustumLeft
     *            the value of the left frustum plane.
     */
    public void setFrustumLeft(final double frustumLeft) {
        _frustumLeft = frustumLeft;
        onFrustumChange();
    }

    /**
     * <code>getFrustumNear</code> gets the value of the near frustum plane.
     * 
     * @return the value of the near frustum plane.
     */
    public double getFrustumNear() {
        return _frustumNear;
    }

    /**
     * <code>setFrustumNear</code> sets the value of the near frustum plane.
     * 
     * @param frustumNear
     *            the value of the near frustum plane.
     */
    public void setFrustumNear(final double frustumNear) {
        _frustumNear = frustumNear;
        onFrustumChange();
    }

    /**
     * <code>getFrustumRight</code> gets the value of the right frustum plane.
     * 
     * @return frustumRight the value of the right frustum plane.
     */
    public double getFrustumRight() {
        return _frustumRight;
    }

    /**
     * <code>setFrustumRight</code> sets the value of the right frustum plane.
     * 
     * @param frustumRight
     *            the value of the right frustum plane.
     */
    public void setFrustumRight(final double frustumRight) {
        _frustumRight = frustumRight;
        onFrustumChange();
    }

    /**
     * <code>getFrustumTop</code> gets the value of the top frustum plane.
     * 
     * @return the value of the top frustum plane.
     */
    public double getFrustumTop() {
        return _frustumTop;
    }

    /**
     * <code>setFrustumTop</code> sets the value of the top frustum plane.
     * 
     * @param frustumTop
     *            the value of the top frustum plane.
     */
    public void setFrustumTop(final double frustumTop) {
        _frustumTop = frustumTop;
        onFrustumChange();
    }

    /**
     * <code>getLocation</code> retrieves the location vector of the camera.
     * 
     * @return the position of the camera.
     */
    public ReadOnlyVector3 getLocation() {
        return _location;
    }

    /**
     * <code>getDirection</code> retrieves the direction vector the camera is facing.
     * 
     * @return the direction the camera is facing.
     */
    public ReadOnlyVector3 getDirection() {
        return _direction;
    }

    /**
     * <code>getLeft</code> retrieves the left axis of the camera.
     * 
     * @return the left axis of the camera.
     */
    public ReadOnlyVector3 getLeft() {
        return _left;
    }

    /**
     * <code>getUp</code> retrieves the up axis of the camera.
     * 
     * @return the up axis of the camera.
     */
    public ReadOnlyVector3 getUp() {
        return _up;
    }

    /**
     * <code>setLocation</code> sets the position of the camera.
     * 
     * @param location
     *            the position of the camera.
     * @see Camera#setLocation(com.ardor3d.math.Vector3)
     */
    public void setLocation(final ReadOnlyVector3 location) {
        _location.set(location);
        onFrameChange();
    }

    /**
     * <code>setDirection</code> sets the direction this camera is facing. In most cases, this changes the up and left
     * vectors of the camera. If your left or up vectors change, you must updates those as well for correct culling.
     * 
     * @param direction
     *            the direction this camera is facing.
     * @see Camera#setDirection(com.ardor3d.math.Vector3)
     */
    public void setDirection(final ReadOnlyVector3 direction) {
        _direction.set(direction);
        onFrameChange();
    }

    /**
     * <code>setLeft</code> sets the left axis of this camera. In most cases, this changes the up and direction vectors
     * of the camera. If your direction or up vectors change, you must updates those as well for correct culling.
     * 
     * @param left
     *            the left axis of this camera.
     * @see Camera#setLeft(com.ardor3d.math.Vector3)
     */
    public void setLeft(final ReadOnlyVector3 left) {
        _left.set(left);
        onFrameChange();
    }

    /**
     * <code>setUp</code> sets the up axis of this camera. In most cases, this changes the direction and left vectors of
     * the camera. If your left or up vectors change, you must updates those as well for correct culling.
     * 
     * @param up
     *            the up axis of this camera.
     * @see Camera#setUp(com.ardor3d.math.Vector3)
     */
    public void setUp(final ReadOnlyVector3 up) {
        _up.set(up);
        onFrameChange();
    }

    /**
     * <code>setAxes</code> sets the axes (left, up and direction) for this camera.
     * 
     * @param left
     *            the left axis of the camera.
     * @param up
     *            the up axis of the camera.
     * @param direction
     *            the direction the camera is facing.
     * @see Camera#setAxes(com.ardor3d.math.Vector3,com.ardor3d.math.Vector3,com.ardor3d.math.Vector3)
     */
    public void setAxes(final ReadOnlyVector3 left, final ReadOnlyVector3 up, final ReadOnlyVector3 direction) {
        _left.set(left);
        _up.set(up);
        _direction.set(direction);
        onFrameChange();
    }

    /**
     * <code>setAxes</code> uses a rotational matrix to set the axes of the camera.
     * 
     * @param axes
     *            the matrix that defines the orientation of the camera.
     */
    public void setAxes(final ReadOnlyMatrix3 axes) {
        axes.getColumn(0, _left);
        axes.getColumn(1, _up);
        axes.getColumn(2, _direction);
        onFrameChange();
    }

    /**
     * normalize normalizes the camera vectors.
     */
    public void normalize() {
        _left.normalizeLocal();
        _up.normalizeLocal();
        _direction.normalizeLocal();
        onFrameChange();
    }

    /**
     * <code>setFrustum</code> sets the frustum of this camera object.
     * 
     * @param near
     *            the near plane.
     * @param far
     *            the far plane.
     * @param left
     *            the left plane.
     * @param right
     *            the right plane.
     * @param top
     *            the top plane.
     * @param bottom
     *            the bottom plane.
     */
    public void setFrustum(final double near, final double far, final double left, final double right,
            final double top, final double bottom) {

        _frustumNear = near;
        _frustumFar = far;
        _frustumLeft = left;
        _frustumRight = right;
        _frustumTop = top;
        _frustumBottom = bottom;
        onFrustumChange();
    }

    public void setFrustumPerspective(final double fovY, final double aspect, final double near, final double far) {
        if (Double.isNaN(aspect) || Double.isInfinite(aspect)) {
            // ignore.
            _logger.warning("Invalid aspect given to setFrustumPerspective: " + aspect);
            return;
        }
        _fovY = fovY;
        final double h = Math.tan(_fovY * MathUtils.DEG_TO_RAD * .5) * near;
        final double w = h * aspect;
        _frustumLeft = -w;
        _frustumRight = w;
        _frustumBottom = -h;
        _frustumTop = h;
        _frustumNear = near;
        _frustumFar = far;
        onFrustumChange();
    }

    public double getFovY() {
        return _fovY;
    }

    /**
     * <code>setFrame</code> sets the orientation and location of the camera.
     * 
     * @param location
     *            the point position of the camera.
     * @param left
     *            the left axis of the camera.
     * @param up
     *            the up axis of the camera.
     * @param direction
     *            the facing of the camera.
     * @see Camera#setFrame(com.ardor3d.math.Vector3, com.ardor3d.math.Vector3, com.ardor3d.math.Vector3,
     *      com.ardor3d.math.Vector3)
     */
    public void setFrame(final ReadOnlyVector3 location, final ReadOnlyVector3 left, final ReadOnlyVector3 up,
            final ReadOnlyVector3 direction) {

        _location.set(location);
        _left.set(left);
        _up.set(up);
        _direction.set(direction);
        onFrameChange();

    }

    /**
     * <code>lookAt</code> is a convenience method for auto-setting the frame based on a world position the user desires
     * the camera to look at. It repoints the camera towards the given position using the difference between the
     * position and the current camera location as a direction vector and the worldUpVector to compute up and left
     * camera vectors.
     * 
     * @param pos
     *            where to look at in terms of world coordinates
     * @param worldUpVector
     *            a normalized vector indicating the up direction of the world. (typically {0, 1, 0} in ardor3d.)
     */
    public void lookAt(final ReadOnlyVector3 pos, final ReadOnlyVector3 worldUpVector) {
        _newDirection.set(pos).subtractLocal(_location).normalizeLocal();

        // check to see if we haven't really updated camera -- no need to call
        // sets.
        if (_newDirection.equals(_direction)) {
            return;
        }
        _direction.set(_newDirection);

        _up.set(worldUpVector).normalizeLocal();
        if (_up.equals(Vector3.ZERO)) {
            _up.set(Vector3.UNIT_Y);
        }
        _left.set(_up).crossLocal(_direction).normalizeLocal();
        if (_left.equals(Vector3.ZERO)) {
            if (_direction.getX() != 0.0) {
                _left.set(_direction.getY(), -_direction.getX(), 0);
            } else {
                _left.set(0, _direction.getZ(), -_direction.getY());
            }
        }
        _up.set(_direction).crossLocal(_left).normalizeLocal();
        onFrameChange();
    }

    /**
     * <code>setFrame</code> sets the orientation and location of the camera.
     * 
     * @param location
     *            the point position of the camera.
     * @param axes
     *            the orientation of the camera.
     */
    public void setFrame(final ReadOnlyVector3 location, final ReadOnlyMatrix3 axes) {
        _location.set(location);
        axes.getColumn(0, _left);
        axes.getColumn(1, _up);
        axes.getColumn(2, _direction);
        onFrameChange();
    }

    /**
     * Forces all aspect of the camera to be updated from internal values, and sets all dirty flags to true so that the
     * next apply() call will fully set this camera to the render context.
     */
    public void update() {
        _depthRangeDirty = true;
        onFrustumChange();
        onViewPortChange();
        onFrameChange();
    }

    /**
     * <code>getPlaneState</code> returns the state of the frustum planes. So checks can be made as to which frustum
     * plane has been examined for culling thus far.
     * 
     * @return the current plane state int.
     */
    public int getPlaneState() {
        return _planeState;
    }

    /**
     * <code>setPlaneState</code> sets the state to keep track of tested planes for culling.
     * 
     * @param planeState
     *            the updated state.
     */
    public void setPlaneState(final int planeState) {
        _planeState = planeState;
    }

    /**
     * <code>getViewPortLeft</code> gets the left boundary of the viewport
     * 
     * @return the left boundary of the viewport
     */
    public double getViewPortLeft() {
        return _viewPortLeft;
    }

    /**
     * <code>setViewPortLeft</code> sets the left boundary of the viewport
     * 
     * @param left
     *            the left boundary of the viewport
     */
    public void setViewPortLeft(final double left) {
        _viewPortLeft = left;
    }

    /**
     * <code>getViewPortRight</code> gets the right boundary of the viewport
     * 
     * @return the right boundary of the viewport
     */
    public double getViewPortRight() {
        return _viewPortRight;
    }

    /**
     * <code>setViewPortRight</code> sets the right boundary of the viewport
     * 
     * @param right
     *            the right boundary of the viewport
     */
    public void setViewPortRight(final double right) {
        _viewPortRight = right;
    }

    /**
     * <code>getViewPortTop</code> gets the top boundary of the viewport
     * 
     * @return the top boundary of the viewport
     */
    public double getViewPortTop() {
        return _viewPortTop;
    }

    /**
     * <code>setViewPortTop</code> sets the top boundary of the viewport
     * 
     * @param top
     *            the top boundary of the viewport
     */
    public void setViewPortTop(final double top) {
        _viewPortTop = top;
    }

    /**
     * <code>getViewPortBottom</code> gets the bottom boundary of the viewport
     * 
     * @return the bottom boundary of the viewport
     */
    public double getViewPortBottom() {
        return _viewPortBottom;
    }

    /**
     * <code>setViewPortBottom</code> sets the bottom boundary of the viewport
     * 
     * @param bottom
     *            the bottom boundary of the viewport
     */
    public void setViewPortBottom(final double bottom) {
        _viewPortBottom = bottom;
    }

    /**
     * <code>setViewPort</code> sets the boundaries of the viewport
     * 
     * @param left
     *            the left boundary of the viewport
     * @param right
     *            the right boundary of the viewport
     * @param bottom
     *            the bottom boundary of the viewport
     * @param top
     *            the top boundary of the viewport
     */
    public void setViewPort(final double left, final double right, final double bottom, final double top) {
        setViewPortLeft(left);
        setViewPortRight(right);
        setViewPortBottom(bottom);
        setViewPortTop(top);
    }

    /**
     * <code>culled</code> tests a bounding volume against the planes of the camera's frustum. The frustums planes are
     * set such that the normals all face in towards the viewable scene. Therefore, if the bounding volume is on the
     * negative side of the plane is can be culled out. If the object should be culled (i.e. not rendered) true is
     * returned, otherwise, false is returned. If bound is null, false is returned and the object will not be culled.
     * 
     * @param bound
     *            the bound to check for culling
     * @return true if the bound should be culled, false otherwise.
     */
    public Camera.FrustumIntersect contains(final BoundingVolume bound) {
        if (bound == null) {
            return FrustumIntersect.Inside;
        }

        int mask;
        FrustumIntersect rVal = FrustumIntersect.Inside;

        for (int planeCounter = FRUSTUM_PLANES; planeCounter >= 0; planeCounter--) {
            if (planeCounter == bound.getCheckPlane()) {
                continue; // we have already checked this plane at first iteration
            }
            final int planeId = (planeCounter == FRUSTUM_PLANES) ? bound.getCheckPlane() : planeCounter;

            mask = 1 << (planeId);
            if ((_planeState & mask) == 0) {
                switch (bound.whichSide(_worldPlane[planeId])) {
                    case Inside:
                        // object is outside of frustum
                        bound.setCheckPlane(planeId);
                        return FrustumIntersect.Outside;
                    case Outside:
                        // object is visible on *this* plane, so mark this plane
                        // so that we don't check it for sub nodes.
                        _planeState |= mask;
                        break;
                    case Neither:
                        rVal = FrustumIntersect.Intersects;
                        break;
                }
            }
        }

        return rVal;
    }

    /**
     * Resizes this camera's view with the given width and height. This is similar to constructing a new camera, but
     * reusing the same Object. This method is called by an associated renderer to notify the camera of changes in the
     * display dimensions.
     * 
     * @param width
     *            the view width
     * @param height
     *            the view height
     */
    public void resize(final int width, final int height) {
        _width = width;
        _height = height;
        onViewPortChange();
    }

    /**
     * Resizes this camera's view with the given width and height. This is similar to constructing a new camera, but
     * reusing the same Object. This method is called by an associated renderer to notify the camera of changes in the
     * display dimensions. A renderer can use the forceDirty parameter for a newly associated camera to ensure that the
     * settings for a previously used camera will be part of the next rendering phase.
     * 
     * @param width
     *            the view width
     * @param height
     *            the view height
     * @param forceDirty
     *            <code>true</code> if camera settings should be treated as changed
     */
    public void resize(final int width, final int height, final boolean forceDirty) {
        // Only override dirty flags when forceDirty is true.
        if (forceDirty) {
            _frustumDirty = true;
            _viewPortDirty = true;
            _frameDirty = true;
        }

        resize(width, height);
    }

    /**
     * <code>onFrustumChange</code> updates the frustum to reflect any changes made to the planes. The new frustum
     * values are kept in a temporary location for use when calculating the new frame. It should be noted that the
     * abstract implementation of this class only updates the data, and does not make any rendering calls. As such, any
     * implementing subclass should insure to override this method call it with super and then call the rendering
     * specific code.
     */
    public void onFrustumChange() {
        if (!isParallelProjection()) {
            final double nearSquared = _frustumNear * _frustumNear;
            final double leftSquared = _frustumLeft * _frustumLeft;
            final double rightSquared = _frustumRight * _frustumRight;
            final double bottomSquared = _frustumBottom * _frustumBottom;
            final double topSquared = _frustumTop * _frustumTop;

            double inverseLength = 1.0 / Math.sqrt(nearSquared + leftSquared);
            _coeffLeft[0] = _frustumNear * inverseLength;
            _coeffLeft[1] = -_frustumLeft * inverseLength;

            inverseLength = 1.0 / Math.sqrt(nearSquared + rightSquared);
            _coeffRight[0] = -_frustumNear * inverseLength;
            _coeffRight[1] = _frustumRight * inverseLength;

            inverseLength = 1.0 / Math.sqrt(nearSquared + bottomSquared);
            _coeffBottom[0] = _frustumNear * inverseLength;
            _coeffBottom[1] = -_frustumBottom * inverseLength;

            inverseLength = 1.0 / Math.sqrt(nearSquared + topSquared);
            _coeffTop[0] = -_frustumNear * inverseLength;
            _coeffTop[1] = _frustumTop * inverseLength;
        } else {
            if (_frustumRight > _frustumLeft) {
                _coeffLeft[0] = -1;
                _coeffLeft[1] = 0;

                _coeffRight[0] = 1;
                _coeffRight[1] = 0;
            } else {
                _coeffLeft[0] = 1;
                _coeffLeft[1] = 0;

                _coeffRight[0] = -1;
                _coeffRight[1] = 0;
            }

            if (_frustumBottom > _frustumTop) {
                _coeffBottom[0] = -1;
                _coeffBottom[1] = 0;

                _coeffTop[0] = 1;
                _coeffTop[1] = 0;
            } else {
                _coeffBottom[0] = 1;
                _coeffBottom[1] = 0;

                _coeffTop[0] = -1;
                _coeffTop[1] = 0;
            }
        }

        _updatePMatrix = true;
        _updateMVPMatrix = true;
        _updateInverseMVPMatrix = true;

        _frustumDirty = true;
    }

    /**
     * <code>onFrameChange</code> updates the view frame of the camera. It should be noted that the abstract
     * implementation of this class only updates the data, and does not make any rendering calls. As such, any
     * implementing subclass should insure to override this method call it with super and then call the rendering
     * specific code.
     */
    public void onFrameChange() {
        final double dirDotLocation = _direction.dot(_location);

        final Vector3 planeNormal = Vector3.fetchTempInstance();

        // left plane
        planeNormal.setX(_left.getX() * _coeffLeft[0]);
        planeNormal.setY(_left.getY() * _coeffLeft[0]);
        planeNormal.setZ(_left.getZ() * _coeffLeft[0]);
        planeNormal.addLocal(_direction.getX() * _coeffLeft[1], _direction.getY() * _coeffLeft[1], _direction.getZ()
                * _coeffLeft[1]);
        _worldPlane[LEFT_PLANE].setNormal(planeNormal);
        _worldPlane[LEFT_PLANE].setConstant(_location.dot(planeNormal));

        // right plane
        planeNormal.setX(_left.getX() * _coeffRight[0]);
        planeNormal.setY(_left.getY() * _coeffRight[0]);
        planeNormal.setZ(_left.getZ() * _coeffRight[0]);
        planeNormal.addLocal(_direction.getX() * _coeffRight[1], _direction.getY() * _coeffRight[1], _direction.getZ()
                * _coeffRight[1]);
        _worldPlane[RIGHT_PLANE].setNormal(planeNormal);
        _worldPlane[RIGHT_PLANE].setConstant(_location.dot(planeNormal));

        // bottom plane
        planeNormal.setX(_up.getX() * _coeffBottom[0]);
        planeNormal.setY(_up.getY() * _coeffBottom[0]);
        planeNormal.setZ(_up.getZ() * _coeffBottom[0]);
        planeNormal.addLocal(_direction.getX() * _coeffBottom[1], _direction.getY() * _coeffBottom[1], _direction
                .getZ()
                * _coeffBottom[1]);
        _worldPlane[BOTTOM_PLANE].setNormal(planeNormal);
        _worldPlane[BOTTOM_PLANE].setConstant(_location.dot(planeNormal));

        // top plane
        planeNormal.setX(_up.getX() * _coeffTop[0]);
        planeNormal.setY(_up.getY() * _coeffTop[0]);
        planeNormal.setZ(_up.getZ() * _coeffTop[0]);
        planeNormal.addLocal(_direction.getX() * _coeffTop[1], _direction.getY() * _coeffTop[1], _direction.getZ()
                * _coeffTop[1]);
        _worldPlane[TOP_PLANE].setNormal(planeNormal);
        _worldPlane[TOP_PLANE].setConstant(_location.dot(planeNormal));

        if (isParallelProjection()) {
            if (_frustumRight > _frustumLeft) {
                _worldPlane[LEFT_PLANE].setConstant(_worldPlane[LEFT_PLANE].getConstant() + _frustumLeft);
                _worldPlane[RIGHT_PLANE].setConstant(_worldPlane[RIGHT_PLANE].getConstant() - _frustumRight);
            } else {
                _worldPlane[LEFT_PLANE].setConstant(_worldPlane[LEFT_PLANE].getConstant() - _frustumLeft);
                _worldPlane[RIGHT_PLANE].setConstant(_worldPlane[RIGHT_PLANE].getConstant() + _frustumRight);
            }

            if (_frustumBottom > _frustumTop) {
                _worldPlane[TOP_PLANE].setConstant(_worldPlane[TOP_PLANE].getConstant() + _frustumTop);
                _worldPlane[BOTTOM_PLANE].setConstant(_worldPlane[BOTTOM_PLANE].getConstant() - _frustumBottom);
            } else {
                _worldPlane[TOP_PLANE].setConstant(_worldPlane[TOP_PLANE].getConstant() - _frustumTop);
                _worldPlane[BOTTOM_PLANE].setConstant(_worldPlane[BOTTOM_PLANE].getConstant() + _frustumBottom);
            }
        }

        // far plane
        planeNormal.set(_direction).negateLocal();
        _worldPlane[FAR_PLANE].setNormal(planeNormal);
        _worldPlane[FAR_PLANE].setConstant(-(dirDotLocation + _frustumFar));

        // near plane
        _worldPlane[NEAR_PLANE].setNormal(_direction);
        _worldPlane[NEAR_PLANE].setConstant(dirDotLocation + _frustumNear);

        Vector3.releaseTempInstance(planeNormal);

        _updateMVMatrix = true;
        _updateMVPMatrix = true;
        _updateInverseMVPMatrix = true;

        _frameDirty = true;
    }

    /**
     * @return true if parallel projection is enable, false if in normal perspective mode
     * @see #setParallelProjection(boolean)
     */
    public boolean isParallelProjection() {
        return _parallelProjection;
    }

    /**
     * Enable/disable parallel projection.
     * 
     * @param value
     *            true to set up this camera for parallel projection is enable, false to enter normal perspective mode
     */
    public void setParallelProjection(final boolean value) {
        _parallelProjection = value;
    }

    protected void updateProjectionMatrix() {
        if (isParallelProjection()) {
            _projection.setIdentity();
            _projection.setValue(0, 0, 2.0 / (_frustumRight - _frustumLeft));
            _projection.setValue(1, 1, 2.0 / (_frustumBottom - _frustumTop));
            _projection.setValue(2, 2, -2.0 / (_frustumFar - _frustumNear));
            _projection.setValue(3, 3, 1);
            _projection.setValue(3, 0, -(_frustumRight + _frustumLeft) / (_frustumRight - _frustumLeft));
            _projection.setValue(3, 1, -(_frustumBottom + _frustumTop) / (_frustumBottom - _frustumTop));
            _projection.setValue(3, 2, -(_frustumFar + _frustumNear) / (_frustumFar - _frustumNear));
        } else {
            _projection.setIdentity();
            _projection.setValue(0, 0, (2.0 * _frustumNear) / (_frustumRight - _frustumLeft));
            _projection.setValue(1, 1, (2.0 * _frustumNear) / (_frustumTop - _frustumBottom));
            _projection.setValue(2, 0, (_frustumRight + _frustumLeft) / (_frustumRight - _frustumLeft));
            _projection.setValue(2, 1, (_frustumTop + _frustumBottom) / (_frustumTop - _frustumBottom));
            _projection.setValue(2, 2, -(_frustumFar + _frustumNear) / (_frustumFar - _frustumNear));
            _projection.setValue(2, 3, -1.0);
            _projection.setValue(3, 2, -(2.0 * _frustumFar * _frustumNear) / (_frustumFar - _frustumNear));
            _projection.setValue(3, 3, -0.0);
        }

        _updatePMatrix = false;
    }

    public ReadOnlyMatrix4 getProjectionMatrix() {
        checkProjection();

        return _projection;
    }

    protected void updateModelViewMatrix() {
        _modelView.setIdentity();
        _modelView.setValue(0, 0, -_left.getX());
        _modelView.setValue(1, 0, -_left.getY());
        _modelView.setValue(2, 0, -_left.getZ());

        _modelView.setValue(0, 1, _up.getX());
        _modelView.setValue(1, 1, _up.getY());
        _modelView.setValue(2, 1, _up.getZ());

        _modelView.setValue(0, 2, -_direction.getX());
        _modelView.setValue(1, 2, -_direction.getY());
        _modelView.setValue(2, 2, -_direction.getZ());

        _transMatrix.setIdentity();
        _transMatrix.setValue(3, 0, -_location.getX());
        _transMatrix.setValue(3, 1, -_location.getY());
        _transMatrix.setValue(3, 2, -_location.getZ());

        _transMatrix.multiplyLocal(_modelView);
        _modelView.set(_transMatrix);
    }

    public ReadOnlyMatrix4 getModelViewMatrix() {
        checkModelView();

        return _modelView;
    }

    public ReadOnlyMatrix4 getModelViewProjectionMatrix() {
        checkModelViewProjection();

        return _modelViewProjection;
    }

    public ReadOnlyMatrix4 getModelViewProjectionInverseMatrix() {
        checkInverseModelViewProjection();

        return _modelViewProjectionInverse;
    }

    public Ray3 getPickRay(final Vector2 screenPosition, final boolean flipVertical, final Ray3 store) {
        final Vector2 pos = Vector2.fetchTempInstance().set(screenPosition);
        if (flipVertical) {
            pos.setY(getHeight() - screenPosition.getY());
        }

        Ray3 result = store;
        if (result == null) {
            result = new Ray3();
        }
        final Vector3 origin = Vector3.fetchTempInstance();
        final Vector3 direction = Vector3.fetchTempInstance();
        getWorldCoordinates(screenPosition, 0, origin);
        getWorldCoordinates(screenPosition, 0.3, direction).subtractLocal(origin).normalizeLocal();
        result.setOrigin(origin);
        result.setDirection(direction);
        Vector3.releaseTempInstance(origin);
        Vector3.releaseTempInstance(direction);
        return result;
    }

    public Vector3 getWorldCoordinates(final ReadOnlyVector2 screenPos, final double zPos) {
        return getWorldCoordinates(screenPos, zPos, null);
    }

    public Vector3 getWorldCoordinates(final ReadOnlyVector2 screenPosition, final double zPos, Vector3 store) {
        if (store == null) {
            store = new Vector3();
        }
        checkInverseModelViewProjection();
        final Vector4 position = Vector4.fetchTempInstance();
        position.set((screenPosition.getX() / getWidth() - _viewPortLeft) / (_viewPortRight - _viewPortLeft) * 2 - 1,
                (screenPosition.getY() / getHeight() - _viewPortBottom) / (_viewPortTop - _viewPortBottom) * 2 - 1,
                zPos * 2 - 1, 1);
        _modelViewProjectionInverse.applyPre(position, position);
        position.multiplyLocal(1.0 / position.getW());
        store.setX(position.getX());
        store.setY(position.getY());
        store.setZ(position.getZ());

        Vector4.releaseTempInstance(position);
        return store;
    }

    /* @see Camera#getScreenCoordinates */
    public Vector3 getScreenCoordinates(final ReadOnlyVector3 worldPos) {
        return getScreenCoordinates(worldPos, null);
    }

    public Vector3 getScreenCoordinates(final ReadOnlyVector3 worldPosition, Vector3 store) {
        if (store == null) {
            store = new Vector3();
        }
        checkModelViewProjection();
        final Vector4 position = Vector4.fetchTempInstance();
        position.set(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), 1);
        _modelViewProjection.applyPre(position, position);
        position.multiplyLocal(1.0 / position.getW());
        store.setX(((position.getX() + 1) * (_viewPortRight - _viewPortLeft) / 2) * getWidth());
        store.setY(((position.getY() + 1) * (_viewPortTop - _viewPortBottom) / 2) * getHeight());
        store.setZ((position.getZ() + 1) / 2);
        Vector4.releaseTempInstance(position);

        return store;
    }

    public double distanceToCam(final ReadOnlyVector3 position) {
        Vector3 tempVector = Vector3.fetchTempInstance();
        position.subtract(_location, tempVector);

        final double retval = Math.abs(tempVector.dot(_direction) / _direction.dot(_direction));
        tempVector = _direction.multiply(retval, tempVector);

        final double distance = tempVector.length();
        Vector3.releaseTempInstance(tempVector);
        return distance;
    }

    /**
     * update modelView if necessary.
     */
    private void checkModelView() {
        if (_updateMVMatrix) {
            updateModelViewMatrix();
            _updateMVMatrix = false;
        }
    }

    /**
     * update projection if necessary.
     */
    private void checkProjection() {
        if (_updatePMatrix) {
            updateProjectionMatrix();
            _updatePMatrix = false;
        }
    }

    /**
     * update modelViewProjection if necessary.
     */
    private void checkModelViewProjection() {
        if (_updateMVPMatrix) {
            checkModelView();
            checkProjection();
            _modelViewProjection.set(getModelViewMatrix()).multiplyLocal(getProjectionMatrix());
            _updateMVPMatrix = false;
        }
    }

    /**
     * update inverse modelViewProjection if necessary.
     */
    private void checkInverseModelViewProjection() {
        if (_updateInverseMVPMatrix) {
            checkModelViewProjection();
            _modelViewProjection.invert(_modelViewProjectionInverse);
            _updateInverseMVPMatrix = false;
        }
    }

    /**
     * @return the width/resolution of the display.
     */
    public int getHeight() {
        return _height;
    }

    /**
     * @return the height/resolution of the display.
     */
    public int getWidth() {
        return _width;
    }

    public void apply(final Renderer r) {
        ContextManager.getCurrentContext().setCurrentCamera(this);
        if (_depthRangeDirty) {
            r.setDepthRange(_depthRangeNear, _depthRangeFar);
        }
        if (_frustumDirty) {
            doFrustumChange(r);
            _frustumDirty = false;
        }
        if (_viewPortDirty) {
            doViewPortChange(r);
            _viewPortDirty = false;
        }
        if (_frameDirty) {
            doFrameChange(r);
            _frameDirty = false;
        }
    }

    public void onViewPortChange() {
        _viewPortDirty = true;
    }

    protected void doFrustumChange(final Renderer r) {
        _matrixBuffer.rewind();
        getProjectionMatrix().toDoubleBuffer(_matrixBuffer);
        _matrixBuffer.rewind();
        r.setProjectionMatrix(_matrixBuffer);
    }

    protected void doViewPortChange(final Renderer r) {
        final int x = (int) (_viewPortLeft * _width);
        final int y = (int) (_viewPortBottom * _height);
        final int w = (int) ((_viewPortRight - _viewPortLeft) * _width);
        final int h = (int) ((_viewPortTop - _viewPortBottom) * _height);
        r.setViewport(x, y, w, h);
    }

    /**
     * @see com.ardor3d.renderer.Camera#onFrameChange()
     */
    protected void doFrameChange(final Renderer r) {
        _matrixBuffer.rewind();
        getModelViewMatrix().toDoubleBuffer(_matrixBuffer);
        _matrixBuffer.rewind();
        r.setModelViewMatrix(_matrixBuffer);
    }

    public void write(final Ardor3DExporter e) throws IOException {
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(_location, "location", new Vector3(Vector3.ZERO));
        capsule.write(_left, "left", new Vector3(Vector3.UNIT_X));
        capsule.write(_up, "up", new Vector3(Vector3.UNIT_Y));
        capsule.write(_direction, "direction", new Vector3(Vector3.UNIT_Z));
        capsule.write(_frustumNear, "frustumNear", 1);
        capsule.write(_frustumFar, "frustumFar", 2);
        capsule.write(_frustumLeft, "frustumLeft", -0.5);
        capsule.write(_frustumRight, "frustumRight", 0.5);
        capsule.write(_frustumTop, "frustumTop", 0.5);
        capsule.write(_frustumBottom, "frustumBottom", -0.5);
        capsule.write(_coeffLeft, "coeffLeft", new double[2]);
        capsule.write(_coeffRight, "coeffRight", new double[2]);
        capsule.write(_coeffBottom, "coeffBottom", new double[2]);
        capsule.write(_coeffTop, "coeffTop", new double[2]);
        capsule.write(_planeQuantity, "planeQuantity", 6);
        capsule.write(_viewPortLeft, "viewPortLeft", 0);
        capsule.write(_viewPortRight, "viewPortRight", 1);
        capsule.write(_viewPortTop, "viewPortTop", 1);
        capsule.write(_viewPortBottom, "viewPortBottom", 0);
        capsule.write(_width, "width", 0);
        capsule.write(_height, "height", 0);
        capsule.write(_depthRangeNear, "depthRangeNear", 0.0);
        capsule.write(_depthRangeFar, "depthRangeFar", 1.0);
    }

    public void read(final Ardor3DImporter e) throws IOException {
        final InputCapsule capsule = e.getCapsule(this);
        _location.set((Vector3) capsule.readSavable("location", new Vector3(Vector3.ZERO)));
        _left.set((Vector3) capsule.readSavable("left", new Vector3(Vector3.UNIT_X)));
        _up.set((Vector3) capsule.readSavable("up", new Vector3(Vector3.UNIT_Y)));
        _direction.set((Vector3) capsule.readSavable("direction", new Vector3(Vector3.UNIT_Z)));
        _frustumNear = capsule.readDouble("frustumNear", 1);
        _frustumFar = capsule.readDouble("frustumFar", 2);
        _frustumLeft = capsule.readDouble("frustumLeft", -0.5);
        _frustumRight = capsule.readDouble("frustumRight", 0.5);
        _frustumTop = capsule.readDouble("frustumTop", 0.5);
        _frustumBottom = capsule.readDouble("frustumBottom", -0.5);
        _coeffLeft = capsule.readDoubleArray("coeffLeft", new double[2]);
        _coeffRight = capsule.readDoubleArray("coeffRight", new double[2]);
        _coeffBottom = capsule.readDoubleArray("coeffBottom", new double[2]);
        _coeffTop = capsule.readDoubleArray("coeffTop", new double[2]);
        _planeQuantity = capsule.readInt("planeQuantity", 6);
        _viewPortLeft = capsule.readDouble("viewPortLeft", 0);
        _viewPortRight = capsule.readDouble("viewPortRight", 1);
        _viewPortTop = capsule.readDouble("viewPortTop", 1);
        _viewPortBottom = capsule.readDouble("viewPortBottom", 0);
        _width = capsule.readInt("width", 0);
        _height = capsule.readInt("height", 0);
        _depthRangeNear = capsule.readDouble("depthRangeNear", 0.0);
        _depthRangeFar = capsule.readDouble("depthRangeFar", 1.0);
    }

    @Override
    protected Camera clone() throws CloneNotSupportedException {
        try {
            return (Camera) super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new AssertionError(); // can not happen
        }
    }

    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        _location.set((Vector3) in.readObject());
        _left.set((Vector3) in.readObject());
        _up.set((Vector3) in.readObject());
        _direction.set((Vector3) in.readObject());
        _frustumNear = in.readDouble();
        _frustumFar = in.readDouble();
        _frustumLeft = in.readDouble();
        _frustumRight = in.readDouble();
        _frustumTop = in.readDouble();
        _frustumBottom = in.readDouble();
        _coeffLeft = (double[]) in.readObject();
        _coeffRight = (double[]) in.readObject();
        _coeffBottom = (double[]) in.readObject();
        _coeffTop = (double[]) in.readObject();
        _planeQuantity = in.readInt();
        _viewPortLeft = in.readDouble();
        _viewPortRight = in.readDouble();
        _viewPortTop = in.readDouble();
        _viewPortBottom = in.readDouble();
        _width = in.readInt();
        _height = in.readInt();
        _depthRangeNear = in.readDouble();
        _depthRangeFar = in.readDouble();
    }

    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeObject(_location);
        out.writeObject(_left);
        out.writeObject(_up);
        out.writeObject(_direction);
        out.writeDouble(_frustumNear);
        out.writeDouble(_frustumFar);
        out.writeDouble(_frustumLeft);
        out.writeDouble(_frustumRight);
        out.writeDouble(_frustumTop);
        out.writeDouble(_frustumBottom);
        out.writeObject(_coeffLeft);
        out.writeObject(_coeffRight);
        out.writeObject(_coeffBottom);
        out.writeObject(_coeffTop);
        out.writeInt(_planeQuantity);
        out.writeDouble(_viewPortLeft);
        out.writeDouble(_viewPortRight);
        out.writeDouble(_viewPortTop);
        out.writeDouble(_viewPortBottom);
        out.writeInt(_width);
        out.writeInt(_height);
        out.writeDouble(_depthRangeNear);
        out.writeDouble(_depthRangeFar);
    }

    public Class<? extends Camera> getClassTag() {
        return getClass();
    }

    public static Camera getCurrentCamera() {
        if (ContextManager.getCurrentContext() == null) {
            return null;
        }
        return ContextManager.getCurrentContext().getCurrentCamera();
    }
}
