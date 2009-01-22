/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.bounding;

import java.io.Serializable;
import java.util.List;

import com.ardor3d.intersection.Intersection;
import com.ardor3d.intersection.PickingUtil;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyMatrix3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.SortUtil;

/**
 * CollisionTree defines a well balanced red black tree used for triangle accurate collision detection. The
 * CollisionTree supports three types: Oriented Bounding Box, Axis-Aligned Bounding Box and Sphere. The tree is composed
 * of a heirarchy of nodes, all but leaf nodes have two children, a left and a right, where the children contain half of
 * the triangles of the parent. This "half split" is executed down the tree until the node is maintaining a set maximum
 * of triangles. This node is called the leaf node. Intersection checks are handled as follows:<br>
 * 1. The bounds of the node is checked for intersection. If no intersection occurs here, no further processing is
 * needed, the children (nodes or triangles) do not intersect.<br>
 * 2a. If an intersection occurs and we have children left/right nodes, pass the intersection information to the
 * children.<br>
 * 2b. If an intersection occurs and we are a leaf node, pass each triangle individually for intersection checking.<br>
 * Optionally, during creation of the collision tree, sorting can be applied. Sorting will attempt to optimize the order
 * of the triangles in such a way as to best split for left and right sub-trees. This function can lead to faster
 * intersection tests, but increases the creation time for the tree. The number of triangles a leaf node is responsible
 * for is defined in CollisionTreeManager. It is actually recommended to allow CollisionTreeManager to maintain the
 * collision trees for a scene.
 * 
 * @see com.ardor3d.bounding.CollisionTreeManager
 */
public class CollisionTree implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type {
        /** CollisionTree using Oriented Bounding Boxes. */
        OBB,
        /** CollisionTree using Axis-Aligned Bounding Boxes. */
        AABB,
        /** CollisionTree using Bounding Spheres. */
        Sphere;
    }

    // Default tree is axis-aligned
    private Type type = Type.AABB;

    // children trees
    private CollisionTree left;
    private CollisionTree right;

    // bounding volumes that contain the triangles that the node is
    // handling
    private BoundingVolume bounds;
    private BoundingVolume worldBounds;

    // the list of triangle indices that compose the tree. This list
    // contains all the triangles of the mesh and is shared between
    // all nodes of this tree.
    private int[] triIndex;

    // Defines the pointers into the triIndex array that this node is
    // directly responsible for.
    private int start, end;

    // Required Spatial information
    protected Mesh mesh;

    // Comparator used to sort triangle indices
    protected transient final TreeComparator comparator = new TreeComparator();

    /**
     * Constructor creates a new instance of CollisionTree.
     * 
     * @param type
     *            the type of collision tree to make
     * @see Type
     */
    public CollisionTree(final Type type) {
        this.type = type;
    }

    /**
     * Recreate this Collision Tree for the given Node and child index.
     * 
     * @param childIndex
     *            the index of the child to generate the tree for.
     * @param parent
     *            The Node that this OBBTree should represent.
     * @param doSort
     *            true to sort triangles during creation, false otherwise
     */
    public void construct(final int childIndex, final Node parent, final boolean doSort) {

        final Spatial spat = parent.getChild(childIndex);
        if (spat instanceof Mesh) {
            mesh = (Mesh) spat;
            triIndex = PickingUtil.getTriangleIndices(mesh, triIndex);
            createTree(0, triIndex.length, doSort);
        }
    }

    /**
     * Recreate this Collision Tree for the given mesh.
     * 
     * @param mesh
     *            The trimesh that this OBBTree should represent.
     * @param doSort
     *            true to sort triangles during creation, false otherwise
     */
    public void construct(final Mesh mesh, final boolean doSort) {
        this.mesh = mesh;
        triIndex = PickingUtil.getTriangleIndices(mesh, triIndex);
        createTree(0, triIndex.length, doSort);
    }

    /**
     * Creates a Collision Tree by recursively creating children nodes, splitting the triangles this node is responsible
     * for in half until the desired triangle count is reached.
     * 
     * @param start
     *            The start index of the tris array, inclusive.
     * @param end
     *            The end index of the tris array, exclusive.
     * @param doSort
     *            True if the triangles should be sorted at each level, false otherwise.
     */
    public void createTree(final int start, final int end, final boolean doSort) {
        this.start = start;
        this.end = end;

        if (triIndex == null) {
            return;
        }

        createBounds();

        // the bounds at this level should contain all the triangles this level
        // is reponsible for.
        bounds.computeFromTris(triIndex, mesh, start, end);

        // check to see if we are a leaf, if the number of triangles we
        // reference is less than or equal to the maximum defined by the
        // CollisionTreeManager we are done.
        if (end - start + 1 <= CollisionTreeManager.getInstance().getMaxTrisPerLeaf()) {
            return;
        }

        // if doSort is set we need to attempt to optimize the referenced
        // triangles.
        // optimizing the sorting of the triangles will help group them
        // spatially
        // in the left/right children better.
        if (doSort) {
            sortTris();
        }

        // create the left child
        if (left == null) {
            left = new CollisionTree(type);
        }

        left.triIndex = triIndex;
        left.mesh = mesh;
        left.createTree(start, (start + end) / 2, doSort);

        // create the right child
        if (right == null) {
            right = new CollisionTree(type);
        }
        right.triIndex = triIndex;
        right.mesh = mesh;
        right.createTree((start + end) / 2, end, doSort);
    }

    /**
     * Tests if the world bounds of the node at this level intersects a provided bounding volume. If an intersection
     * occurs, true is returned, otherwise false is returned. If the provided volume is invalid, false is returned.
     * 
     * @param volume
     *            the volume to intersect with.
     * @return true if there is an intersect, false otherwise.
     */
    public boolean intersectsBounding(final BoundingVolume volume) {
        switch (volume.getType()) {
            case AABB:
                return worldBounds.intersectsBoundingBox((BoundingBox) volume);
            case OBB:
                return worldBounds.intersectsOrientedBoundingBox((OrientedBoundingBox) volume);
            case Sphere:
                return worldBounds.intersectsSphere((BoundingSphere) volume);
            default:
                return false;
        }

    }

    /**
     * Determines if this Collision Tree intersects the given CollisionTree. If a collision occurs, true is returned,
     * otherwise false is returned. If the provided collisionTree is invalid, false is returned.
     * 
     * @param collisionTree
     *            The Tree to test.
     * @return True if they intersect, false otherwise.
     */
    public boolean intersect(final CollisionTree collisionTree) {
        if (collisionTree == null) {
            return false;
        }

        {
            final ReadOnlyMatrix3 rotation = collisionTree.mesh.getWorldRotation();
            final ReadOnlyVector3 translation = collisionTree.mesh.getWorldTranslation();
            final ReadOnlyVector3 scale = collisionTree.mesh.getWorldScale();

            collisionTree.worldBounds = collisionTree.bounds.transform(rotation, translation, scale,
                    collisionTree.worldBounds);
        }

        // our two collision bounds do not intersect, therefore, our triangles
        // must
        // not intersect. Return false.
        if (!intersectsBounding(collisionTree.worldBounds)) {
            return false;
        }

        // check children
        if (left != null) { // This is not a leaf
            if (collisionTree.intersect(left)) {
                return true;
            }
            if (collisionTree.intersect(right)) {
                return true;
            }
            return false;
        }

        // This is a leaf
        if (collisionTree.left != null) {
            // but collision isn't
            if (intersect(collisionTree.left)) {
                return true;
            }
            if (intersect(collisionTree.right)) {
                return true;
            }
            return false;
        }

        // both are leaves
        final ReadOnlyMatrix3 roti = mesh.getWorldRotation();
        final ReadOnlyVector3 scalei = mesh.getWorldScale();
        final ReadOnlyVector3 transi = mesh.getWorldTranslation();

        final ReadOnlyMatrix3 rotj = collisionTree.mesh.getWorldRotation();
        final ReadOnlyVector3 scalej = collisionTree.mesh.getWorldScale();
        final ReadOnlyVector3 transj = collisionTree.mesh.getWorldTranslation();

        final Vector3 tempVa = Vector3.fetchTempInstance();
        final Vector3 tempVb = Vector3.fetchTempInstance();
        final Vector3 tempVc = Vector3.fetchTempInstance();
        final Vector3 tempVd = Vector3.fetchTempInstance();
        final Vector3 tempVe = Vector3.fetchTempInstance();
        final Vector3 tempVf = Vector3.fetchTempInstance();
        final Vector3[] verts = { Vector3.fetchTempInstance(), Vector3.fetchTempInstance(), Vector3.fetchTempInstance() };
        final Vector3[] target = { Vector3.fetchTempInstance(), Vector3.fetchTempInstance(),
                Vector3.fetchTempInstance() };

        // for every triangle to compare, put them into world space and check
        // for intersections
        boolean result = false;
        outer: for (int i = start; i < end; i++) {
            PickingUtil.getTriangle(mesh, triIndex[i], verts);
            roti.applyPost(tempVa.set(verts[0]).multiplyLocal(scalei), tempVa).addLocal(transi);
            roti.applyPost(tempVb.set(verts[1]).multiplyLocal(scalei), tempVb).addLocal(transi);
            roti.applyPost(tempVc.set(verts[2]).multiplyLocal(scalei), tempVc).addLocal(transi);
            for (int j = collisionTree.start; j < collisionTree.end; j++) {
                PickingUtil.getTriangle(collisionTree.mesh, collisionTree.triIndex[j], target);
                rotj.applyPost(tempVd.set(target[0]).multiplyLocal(scalej), tempVd).addLocal(transj);
                rotj.applyPost(tempVe.set(target[1]).multiplyLocal(scalej), tempVe).addLocal(transj);
                rotj.applyPost(tempVf.set(target[2]).multiplyLocal(scalej), tempVf).addLocal(transj);
                if (Intersection.intersection(tempVa, tempVb, tempVc, tempVd, tempVe, tempVf)) {
                    result = true;
                    break outer;
                }
            }
        }

        Vector3.releaseTempInstance(tempVa);
        Vector3.releaseTempInstance(tempVb);
        Vector3.releaseTempInstance(tempVc);
        Vector3.releaseTempInstance(tempVd);
        Vector3.releaseTempInstance(tempVe);
        Vector3.releaseTempInstance(tempVf);

        for (final Vector3 vec : verts) {
            Vector3.releaseTempInstance(vec);
        }
        for (final Vector3 vec : target) {
            Vector3.releaseTempInstance(vec);
        }

        return result;
    }

    /**
     * Determines if this Collision Tree intersects the given CollisionTree. If a collision occurs, true is returned,
     * otherwise false is returned. If the provided collisionTree is invalid, false is returned. All collisions that
     * occur are stored in lists as an integer index into the mesh's triangle buffer. where aList is the triangles for
     * this mesh and bList is the triangles for the test tree.
     * 
     * @param collisionTree
     *            The Tree to test.
     * @param aList
     *            a list to contain the colliding triangles of this mesh.
     * @param bList
     *            a list to contain the colliding triangles of the testing mesh.
     * @return True if they intersect, false otherwise.
     */
    public boolean intersect(final CollisionTree collisionTree, final List<Integer> aList, final List<Integer> bList) {

        if (collisionTree == null) {
            return false;
        }

        {
            final ReadOnlyMatrix3 rotation = collisionTree.mesh.getWorldRotation();
            final ReadOnlyVector3 translation = collisionTree.mesh.getWorldTranslation();
            final ReadOnlyVector3 scale = collisionTree.mesh.getWorldScale();

            collisionTree.worldBounds = collisionTree.bounds.transform(rotation, translation, scale,
                    collisionTree.worldBounds);
        }

        // our two collision bounds do not intersect, therefore, our triangles
        // must not intersect. Return false.
        if (!intersectsBounding(collisionTree.worldBounds)) {
            return false;
        }

        // if our node is not a leaf send the children (both left and right) to
        // the test tree.
        if (left != null) { // This is not a leaf
            boolean test = collisionTree.intersect(left, bList, aList);
            test = collisionTree.intersect(right, bList, aList) || test;
            return test;
        }

        // This node is a leaf, but the testing tree node is not. Therefore,
        // continue processing the testing tree until we find its leaves.
        if (collisionTree.left != null) {
            boolean test = intersect(collisionTree.left, aList, bList);
            test = intersect(collisionTree.right, aList, bList) || test;
            return test;
        }

        // both this node and the testing node are leaves. Therefore, we can
        // switch to checking the contained triangles with each other. Any
        // that are found to intersect are placed in the appropriate list.
        final ReadOnlyMatrix3 roti = mesh.getWorldRotation();
        final ReadOnlyVector3 scalei = mesh.getWorldScale();
        final ReadOnlyVector3 transi = mesh.getWorldTranslation();

        final ReadOnlyMatrix3 rotj = collisionTree.mesh.getWorldRotation();
        final ReadOnlyVector3 scalej = collisionTree.mesh.getWorldScale();
        final ReadOnlyVector3 transj = collisionTree.mesh.getWorldTranslation();

        boolean test = false;

        final Vector3 tempVa = Vector3.fetchTempInstance();
        final Vector3 tempVb = Vector3.fetchTempInstance();
        final Vector3 tempVc = Vector3.fetchTempInstance();
        final Vector3 tempVd = Vector3.fetchTempInstance();
        final Vector3 tempVe = Vector3.fetchTempInstance();
        final Vector3 tempVf = Vector3.fetchTempInstance();
        final Vector3[] verts = { Vector3.fetchTempInstance(), Vector3.fetchTempInstance(), Vector3.fetchTempInstance() };
        final Vector3[] target = { Vector3.fetchTempInstance(), Vector3.fetchTempInstance(),
                Vector3.fetchTempInstance() };

        for (int i = start; i < end; i++) {
            PickingUtil.getTriangle(mesh, triIndex[i], verts);
            roti.applyPost(tempVa.set(verts[0]).multiplyLocal(scalei), tempVa).addLocal(transi);
            roti.applyPost(tempVb.set(verts[1]).multiplyLocal(scalei), tempVb).addLocal(transi);
            roti.applyPost(tempVc.set(verts[2]).multiplyLocal(scalei), tempVc).addLocal(transi);
            for (int j = collisionTree.start; j < collisionTree.end; j++) {
                PickingUtil.getTriangle(collisionTree.mesh, collisionTree.triIndex[j], target);
                rotj.applyPost(tempVd.set(target[0]).multiplyLocal(scalej), tempVd).addLocal(transj);
                rotj.applyPost(tempVe.set(target[1]).multiplyLocal(scalej), tempVe).addLocal(transj);
                rotj.applyPost(tempVf.set(target[2]).multiplyLocal(scalej), tempVf).addLocal(transj);
                if (Intersection.intersection(tempVa, tempVb, tempVc, tempVd, tempVe, tempVf)) {
                    test = true;
                    aList.add(triIndex[i]);
                    bList.add(collisionTree.triIndex[j]);
                }
            }
        }

        Vector3.releaseTempInstance(tempVa);
        Vector3.releaseTempInstance(tempVb);
        Vector3.releaseTempInstance(tempVc);
        Vector3.releaseTempInstance(tempVd);
        Vector3.releaseTempInstance(tempVe);
        Vector3.releaseTempInstance(tempVf);

        for (final Vector3 vec : verts) {
            Vector3.releaseTempInstance(vec);
        }
        for (final Vector3 vec : target) {
            Vector3.releaseTempInstance(vec);
        }

        return test;

    }

    /**
     * intersect checks for collisions between this collision tree and a provided Ray. Any collisions are stored in a
     * provided list. The ray is assumed to have a normalized direction for accurate calculations.
     * 
     * @param ray
     *            the ray to test for intersections.
     * @param triList
     *            the list to store instersections with.
     */
    public void intersect(final Ray3 ray, final List<Integer> triList) {

        // if our ray doesn't hit the bounds, then it must not hit a triangle.
        if (!worldBounds.intersects(ray)) {
            return;
        }

        // This is not a leaf node, therefore, check each child (left/right) for
        // intersection with the ray.
        if (left != null) {
            final ReadOnlyMatrix3 rotation = mesh.getWorldRotation();
            final ReadOnlyVector3 translation = mesh.getWorldTranslation();
            final ReadOnlyVector3 scale = mesh.getWorldScale();

            left.worldBounds = left.bounds.transform(rotation, translation, scale, left.worldBounds);
            left.intersect(ray, triList);
        }

        if (right != null) {
            final ReadOnlyMatrix3 rotation = mesh.getWorldRotation();
            final ReadOnlyVector3 translation = mesh.getWorldTranslation();
            final ReadOnlyVector3 scale = mesh.getWorldScale();

            right.worldBounds = right.bounds.transform(rotation, translation, scale, right.worldBounds);

            right.intersect(ray, triList);
        } else if (left == null) {
            // This is a leaf node. We can therfore, check each triangle this
            // node contains. If an intersection occurs, place it in the
            // list.

            final Vector3[] points = new Vector3[3];
            for (int i = start; i < end; i++) {
                PickingUtil.getTriangle(mesh, triIndex[i], points);
                mesh.localToWorld(points[0], points[0]);
                mesh.localToWorld(points[1], points[1]);
                mesh.localToWorld(points[2], points[2]);
                if (ray.intersects(points[0], points[1], points[2], null, true)) {
                    triList.add(triIndex[i]);
                }
            }
        }
    }

    /**
     * Returns the bounding volume for this tree node in local space.
     * 
     * @return the bounding volume for this tree node in local space.
     */
    public BoundingVolume getBounds() {
        return bounds;
    }

    /**
     * Returns the bounding volume for this tree node in world space.
     * 
     * @return the bounding volume for this tree node in world space.
     */
    public BoundingVolume getWorldBounds() {
        return worldBounds;
    }

    /**
     * creates the appropriate bounding volume based on the type set during construction.
     */
    private void createBounds() {
        switch (type) {
            case AABB:
                bounds = new BoundingBox();
                worldBounds = new BoundingBox();
                break;
            case OBB:
                bounds = new OrientedBoundingBox();
                worldBounds = new OrientedBoundingBox();
                break;
            case Sphere:
                bounds = new BoundingSphere();
                worldBounds = new BoundingSphere();
                break;
            default:
                break;
        }
    }

    /**
     * sortTris attempts to optimize the ordering of the subsection of the array of triangles this node is responsible
     * for. The sorting is based on the most efficient method along an axis. Using the TreeComparator and quick sort,
     * the subsection of the array is sorted.
     */
    public void sortTris() {
        switch (type) {
            case AABB:
                // determine the longest length of the box, this axis will be
                // best
                // for sorting.
                if (((BoundingBox) bounds).getXExtent() > ((BoundingBox) bounds).getYExtent()) {
                    if (((BoundingBox) bounds).getXExtent() > ((BoundingBox) bounds).getZExtent()) {
                        comparator.setAxis(TreeComparator.Axis.X);
                    } else {
                        comparator.setAxis(TreeComparator.Axis.Z);
                    }
                } else {
                    if (((BoundingBox) bounds).getYExtent() > ((BoundingBox) bounds).getZExtent()) {
                        comparator.setAxis(TreeComparator.Axis.Y);
                    } else {
                        comparator.setAxis(TreeComparator.Axis.Z);
                    }
                }
                break;
            case OBB:
                // determine the longest length of the box, this axis will be
                // best
                // for sorting.
                if (((OrientedBoundingBox) bounds).extent.getX() > ((OrientedBoundingBox) bounds).extent.getY()) {
                    if (((OrientedBoundingBox) bounds).extent.getX() > ((OrientedBoundingBox) bounds).extent.getZ()) {
                        comparator.setAxis(TreeComparator.Axis.X);
                    } else {
                        comparator.setAxis(TreeComparator.Axis.Z);
                    }
                } else {
                    if (((OrientedBoundingBox) bounds).extent.getY() > ((OrientedBoundingBox) bounds).extent.getZ()) {
                        comparator.setAxis(TreeComparator.Axis.Y);
                    } else {
                        comparator.setAxis(TreeComparator.Axis.Z);
                    }
                }
                break;
            case Sphere:
                // sort any axis, X is fine.
                comparator.setAxis(TreeComparator.Axis.X);
                break;
            default:
                break;
        }

        comparator.setCenter(bounds.center);
        comparator.setMesh(mesh);
        SortUtil.qsort(triIndex, start, end - 1, comparator);
    }

    /**
     * Rebuild all the leaves listed in triangleIndices, and any branches leading up to them.
     * 
     * @param triangleIndices
     *            a list of all the leaves to rebuild
     * @param startLevel
     *            how many trunk levels to ignore, for none put zero (ignoring the first 2-3 levels increases speed
     *            greatly)
     */
    public void rebuildLeaves(final List<Integer> triangleIndices, final int startLevel) {
        rebuildLeaves(triangleIndices, startLevel, 0);
    }

    private void rebuildLeaves(final List<Integer> triangleIndices, final int startLevel, int currentLevel) {
        int i = 0;
        currentLevel++;

        if (left == null && right == null) {
            // is a leaf, get rid of any matching indexes and rebuild
            boolean alreadyRebuilt = false;
            while (i < triangleIndices.size()) {
                if (triangleIndices.get(i).intValue() >= start && triangleIndices.get(i).intValue() < end) {
                    triangleIndices.remove(i);
                    if (alreadyRebuilt == false) {
                        alreadyRebuilt = true;
                        bounds.computeFromTris(triIndex, mesh, start, end);
                    }
                } else {
                    i++;
                }
            }
        } else if (containsAnyLeaf(triangleIndices)) {
            if (left != null) {
                left.rebuildLeaves(triangleIndices, startLevel, currentLevel);
            }

            if (right != null) {
                right.rebuildLeaves(triangleIndices, startLevel, currentLevel);
            }

            if (currentLevel > startLevel) {
                bounds.computeFromTris(triIndex, mesh, start, end);
            }
        }
    }

    /**
     * Checks if this branch or one of its subbranches/leaves contains any of the given triangleIndices
     * 
     * @param triangleIndices
     *            the indices to look for
     * @return true if the index is contained, false otherwise
     */
    public boolean containsAnyLeaf(final List<Integer> triangleIndices) {
        boolean rtnVal = false;

        for (int i = 0; i < triangleIndices.size(); i++) {
            if (triangleIndices.get(i).intValue() >= start && triangleIndices.get(i).intValue() < end) {
                rtnVal = true;
                break;
            }
        }

        return rtnVal;
    }
}
