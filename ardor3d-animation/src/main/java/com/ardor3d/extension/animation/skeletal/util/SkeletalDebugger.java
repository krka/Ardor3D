/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal.util;

import com.ardor3d.extension.animation.skeletal.Joint;
import com.ardor3d.extension.animation.skeletal.SkeletonPose;
import com.ardor3d.extension.animation.skeletal.SkinnedMesh;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.WireframeState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.hint.TextureCombineMode;
import com.ardor3d.scenegraph.shape.Pyramid;
import com.ardor3d.scenegraph.shape.Sphere;

/**
 * Utility useful for drawing Skeletons found in a scene.
 */
public class SkeletalDebugger {

    /**
     * Traverse the given scene and draw the currently posed Skeleton of any SkinnedMesh we encounter.
     * 
     * @param scene
     *            the scene
     * @param renderer
     *            the Renderer to draw with.
     */
    public static void drawSkeletons(final Spatial scene, final Renderer renderer) {
        assert scene != null : "scene must not be null.";

        // Check if we are a skinned mesh
        boolean doChildren = true;
        if (scene instanceof SkinnedMesh) {
            // If we're in view, go ahead and draw our associated skeleton pose
            final Camera cam = Camera.getCurrentCamera();
            final int state = cam.getPlaneState();
            if (cam.contains(scene.getWorldBound()) != Camera.FrustumIntersect.Outside) {
                SkeletalDebugger.drawSkeleton(((SkinnedMesh) scene).getCurrentPose(), renderer);
            } else {
                doChildren = false;
            }
            cam.setPlaneState(state);
        }

        // Recurse down the scene if we're a Node and we were not flagged to ignore children.
        if (doChildren && scene instanceof Node) {
            final Node n = (Node) scene;
            if (n.getNumberOfChildren() != 0) {
                for (int i = n.getNumberOfChildren(); --i >= 0;) {
                    SkeletalDebugger.drawSkeletons(n.getChild(i), renderer);
                }
            }
        }
    }

    /**
     * Draw a skeleton in a specific pose.
     * 
     * @param pose
     *            the posed skeleton to draw
     * @param renderer
     *            the Renderer to draw with.
     */
    private static void drawSkeleton(final SkeletonPose pose, final Renderer renderer) {
        final Joint[] joints = pose.getSkeleton().getJoints();
        final Transform[] globals = pose.getGlobalJointTransforms();

        for (int i = 0, max = joints.length; i < max; i++) {
            SkeletalDebugger.drawJoint(globals[i], renderer);
            final short parentIndex = joints[i].getParentIndex();
            if (parentIndex != Joint.NO_PARENT) {
                SkeletalDebugger.drawBone(globals[parentIndex], globals[i], renderer);
            }
        }
    }

    /** Our bone shape. */
    private static final Pyramid bone = new Pyramid("bone", 1, 1);
    static {
        // Alter the primitive to better represent our bone.
        // Set color to white
        SkeletalDebugger.setBoneColor(ColorRGBA.WHITE);
        // Rotate the vertices of our bone to point along the Z axis instead of the Y.
        SkeletalDebugger.bone.getMeshData().rotatePoints(
                new Quaternion().fromAngleAxis(90 * MathUtils.DEG_TO_RAD, Vector3.UNIT_X));
        // Drop the normals
        SkeletalDebugger.bone.getMeshData().setNormalBuffer(null);

        // No lighting or texturing
        SkeletalDebugger.bone.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        SkeletalDebugger.bone.getSceneHints().setTextureCombineMode(TextureCombineMode.Off);
        // Do not queue... draw right away.
        SkeletalDebugger.bone.getSceneHints().setRenderBucketType(RenderBucketType.Skip);
        // Draw in wire frame mode.
        SkeletalDebugger.bone.setRenderState(new WireframeState());
        // Respect existing zbuffer, and write into it
        SkeletalDebugger.bone.setRenderState(new ZBufferState());
        // Update our bone and make it ready for use.
        SkeletalDebugger.bone.updateGeometricState(0);
    }

    /**
     * Draw a single bone using the given world-space joint transformations.
     * 
     * @param start
     *            our parent joint transform
     * @param end
     *            our child joint transform
     * @param renderer
     *            the Renderer to draw with.
     */
    private static void drawBone(final Transform start, final Transform end, final Renderer renderer) {
        // Determine our start and end points
        final Vector3 stPnt = Vector3.fetchTempInstance();
        final Vector3 endPnt = Vector3.fetchTempInstance();
        start.applyForward(Vector3.ZERO, stPnt);
        end.applyForward(Vector3.ZERO, endPnt);

        // determine distance and use as a scale to elongate the bone
        // XXX: perhaps instead of 1 we should use some kind of average scale between start and end bone?
        final double scale = stPnt.distance(endPnt);
        SkeletalDebugger.bone.setWorldScale(1, 1, scale);

        // determine center point of bone (translation).
        final Vector3 store = Vector3.fetchTempInstance();
        SkeletalDebugger.bone.setWorldTranslation(stPnt.add(endPnt, store).divideLocal(2.0));
        Vector3.releaseTempInstance(store);

        // Orient bone to point along axis formed by start and end points.
        final Matrix3 orient = Matrix3.fetchTempInstance();
        orient.lookAt(endPnt.subtractLocal(stPnt).normalizeLocal(), Vector3.UNIT_Y);
        SkeletalDebugger.bone.setWorldRotation(orient);

        // Release some temp vars.
        Matrix3.releaseTempInstance(orient);
        Vector3.releaseTempInstance(stPnt);
        Vector3.releaseTempInstance(endPnt);

        // Draw our bone!
        SkeletalDebugger.bone.draw(renderer);
    }

    /**
     * Set the color of the bone object used in skeleton drawing.
     * 
     * @param color
     *            the new color to use for skeleton bones.
     */
    public static void setBoneColor(final ReadOnlyColorRGBA color) {
        SkeletalDebugger.bone.setSolidColor(color);
    }

    /** Our joint shape. */
    private static final Sphere joint = new Sphere("joint", 3, 4, 0.5);
    static {
        // Alter the primitive to better represent our joint.
        // Set color to cyan
        SkeletalDebugger.setJointColor(ColorRGBA.RED);
        // Drop the normals
        SkeletalDebugger.joint.getMeshData().setNormalBuffer(null);

        // No lighting or texturing
        SkeletalDebugger.joint.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        SkeletalDebugger.joint.getSceneHints().setTextureCombineMode(TextureCombineMode.Off);
        // Do not queue... draw right away.
        SkeletalDebugger.joint.getSceneHints().setRenderBucketType(RenderBucketType.Skip);
        // Draw in wire frame mode.
        SkeletalDebugger.joint.setRenderState(new WireframeState());
        // Respect existing zbuffer, and write into it
        SkeletalDebugger.joint.setRenderState(new ZBufferState());
        // Update our joint and make it ready for use.
        SkeletalDebugger.joint.updateGeometricState(0);
    }

    /**
     * Draw a single Joint using the given world-space joint transform.
     * 
     * @param jntTransform
     *            our joint transform
     * @param renderer
     *            the Renderer to draw with.
     */
    private static void drawJoint(final Transform jntTransform, final Renderer renderer) {
        SkeletalDebugger.joint.setWorldTransform(jntTransform);
        SkeletalDebugger.joint.draw(renderer);
    }

    /**
     * Set the color of the joint object used in skeleton drawing.
     * 
     * @param color
     *            the new color to use for skeleton joints.
     */
    public static void setJointColor(final ReadOnlyColorRGBA color) {
        SkeletalDebugger.joint.setSolidColor(color);
    }
}
