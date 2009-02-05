/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.intersection;

import java.nio.IntBuffer;
import java.util.List;

import com.ardor3d.bounding.CollisionTree;
import com.ardor3d.bounding.CollisionTreeManager;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyMatrix3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.Spatial.PickingHint;
import com.ardor3d.util.geom.BufferUtils;

public abstract class PickingUtil {
    public static void findPick(final Spatial spatial, final Ray3 ray, final PickResults results) {
        if (!spatial.isPickingHintEnabled(PickingHint.Pickable)) {
            return;
        }

        if (spatial instanceof Node) {
            final Node node = (Node) spatial;

            if (node.getNumberOfChildren() == 0 || node.getWorldBound() == null) {
                return;
            }
            if (node.getWorldBound().intersects(ray)) {
                // further checking needed.
                for (int i = 0; i < node.getNumberOfChildren(); i++) {
                    PickingUtil.findPick(node.getChild(i), ray, results);
                }
            }
        } else if (spatial instanceof Mesh) {
            final Mesh mesh = (Mesh) spatial;

            if (mesh.getWorldBound() == null) {
                return;
            }
            if (mesh.getWorldBound().intersects(ray)) {
                // find the triangle that is being hit.
                // add this node and the triangle to the PickResults list.
                results.addPick(ray, mesh);
            }
        }
    }

    public static void findCollisions(final Spatial spatial, final Spatial scene, final CollisionResults results) {
        if (spatial == scene || spatial.getWorldBound() == null
                || !spatial.isPickingHintEnabled(PickingHint.Collidable)
                || !scene.isPickingHintEnabled(PickingHint.Collidable)) {
            return;
        }

        if (spatial instanceof Node) {
            final Node node = (Node) spatial;

            if (node.getWorldBound().intersects(scene.getWorldBound())) {
                // further checking needed.
                for (int i = 0; i < node.getNumberOfChildren(); i++) {
                    PickingUtil.findCollisions(node.getChild(i), scene, results);
                }
            }
        } else if (spatial instanceof Mesh) {
            final Mesh mesh = (Mesh) spatial;

            if (mesh.getWorldBound().intersects(scene.getWorldBound())) {
                if (scene instanceof Node) {
                    final Node parent = (Node) scene;
                    for (int i = 0; i < parent.getNumberOfChildren(); i++) {
                        PickingUtil.findCollisions(mesh, parent.getChild(i), results);
                    }
                } else {
                    results.addCollision(mesh, (Mesh) scene);
                }
            }
        }
    }

    public static void findTrianglePick(final Mesh mesh, final Ray3 toTest, final List<Integer> results) {
        if (mesh.getWorldBound() == null || !mesh.isPickingHintEnabled(PickingHint.Pickable)) {
            return;
        }

        if (mesh.getWorldBound().intersects(toTest)) {
            final CollisionTree ct = CollisionTreeManager.getInstance().getCollisionTree(mesh);
            if (ct != null) {
                final ReadOnlyMatrix3 worldRotation = mesh.getWorldRotation();
                final ReadOnlyVector3 worldTranslation = mesh.getWorldTranslation();
                final ReadOnlyVector3 worldScale = mesh.getWorldScale();

                ct.getBounds().transform(worldRotation, worldTranslation, worldScale, ct.getWorldBounds());
                ct.intersect(toTest, results);
            }
        }
    }

    /**
     * This function checks for intersection between this mesh and the given one. On the first intersection, true is
     * returned.
     * 
     * @param toCheck
     *            The intersection testing mesh.
     * @return True if they intersect.
     */
    public static boolean hasTriangleCollision(final Mesh testMesh, final Mesh toCheck) {
        if (!testMesh.isPickingHintEnabled(PickingHint.Collidable)
                || !toCheck.isPickingHintEnabled(PickingHint.Collidable)) {
            return false;
        }

        final CollisionTree thisCT = CollisionTreeManager.getInstance().getCollisionTree(testMesh);
        final CollisionTree checkCT = CollisionTreeManager.getInstance().getCollisionTree(toCheck);

        if (thisCT == null || checkCT == null) {
            return false;
        }

        final ReadOnlyMatrix3 worldRotation = testMesh.getWorldRotation();
        final ReadOnlyVector3 worldTranslation = testMesh.getWorldTranslation();
        final ReadOnlyVector3 worldScale = testMesh.getWorldScale();

        thisCT.getBounds().transform(worldRotation, worldTranslation, worldScale, thisCT.getWorldBounds());
        return thisCT.intersect(checkCT);
    }

    /**
     * This function finds all intersections between this mesh and the checking one. The intersections are stored as
     * Integer objects of Triangle indexes in each of the parameters.
     * 
     * @param toCheck
     *            The Mesh to check.
     * @param testIndex
     *            The array of triangle indexes intersecting in this mesh.
     * @param otherIndex
     *            The array of triangle indexes intersecting in the given mesh.
     */
    public static void findTriangleCollision(final Mesh testMesh, final Mesh toCheck, final List<Integer> testIndex,
            final List<Integer> otherIndex) {
        if (!testMesh.isPickingHintEnabled(PickingHint.Collidable)
                || !toCheck.isPickingHintEnabled(PickingHint.Collidable)) {
            return;
        }

        final CollisionTree myTree = CollisionTreeManager.getInstance().getCollisionTree(testMesh);
        final CollisionTree otherTree = CollisionTreeManager.getInstance().getCollisionTree(toCheck);

        if (myTree == null || otherTree == null) {
            return;
        }

        final ReadOnlyMatrix3 worldRotation = testMesh.getWorldRotation();
        final ReadOnlyVector3 worldTranslation = testMesh.getWorldTranslation();
        final ReadOnlyVector3 worldScale = testMesh.getWorldScale();

        myTree.getBounds().transform(worldRotation, worldTranslation, worldScale, myTree.getWorldBounds());

        myTree.intersect(otherTree, testIndex, otherIndex);
    }

    public static boolean hasCollision(final Spatial spatial, final Spatial scene, final boolean checkTriangles) {
        if (spatial == scene || spatial.getWorldBound() == null
                || !spatial.isPickingHintEnabled(PickingHint.Collidable)
                || !scene.isPickingHintEnabled(PickingHint.Collidable)) {
            return false;
        }

        if (spatial instanceof Node) {
            final Node node = (Node) spatial;

            if (node.getWorldBound().intersects(scene.getWorldBound())) {
                if (node.getNumberOfChildren() == 0 && !checkTriangles) {
                    return true;
                }
                // further checking needed.
                for (int i = 0; i < node.getNumberOfChildren(); i++) {
                    if (PickingUtil.hasCollision(node.getChild(i), scene, checkTriangles)) {
                        return true;
                    }
                }
            }
        } else if (spatial instanceof Mesh) {
            final Mesh mesh = (Mesh) spatial;

            if (mesh.getWorldBound().intersects(scene.getWorldBound())) {
                if (scene instanceof Node) {
                    final Node parent = (Node) scene;
                    for (int i = 0; i < parent.getNumberOfChildren(); i++) {
                        if (PickingUtil.hasCollision(mesh, parent.getChild(i), checkTriangles)) {
                            return true;
                        }
                    }

                    return false;
                }

                if (!checkTriangles) {
                    return true;
                }

                return PickingUtil.hasTriangleCollision(mesh, (Mesh) scene);
            }

            return false;

        }

        return false;
    }

    /**
     * Stores in the <code>storage</code> array the indices of triangle <code>i</code>. If <code>i</code> is an invalid
     * index, or if <code>storage.length < 3</code>, then nothing happens
     * 
     * @param index
     *            The index of the triangle to get.
     * @param storage
     *            The array that will hold the i's indexes.
     */
    public static void getTriangle(final Mesh mesh, final int index, final int[] storage) {
        // FIXME: hard coded section 0
        if (index < mesh.getMeshData().getPrimitiveCount(0) && storage.length >= 3) {
            final IntBuffer indices = mesh.getMeshData().getIndexBuffer();
            storage[0] = indices.get(mesh.getMeshData().getVertexIndex(index, 0, 0));
            storage[1] = indices.get(mesh.getMeshData().getVertexIndex(index, 1, 0));
            storage[2] = indices.get(mesh.getMeshData().getVertexIndex(index, 2, 0));
        }
    }

    /**
     * Stores in the <code>vertices</code> array the vertex values of triangle <code>i</code>. If <code>i</code> is an
     * invalid triangle index, nothing happens.
     * 
     * @param index
     * @param vertices
     */
    public static void getTriangle(final Mesh mesh, final int index, final Vector3[] store) {
        // FIXME: hard coded section 0
        if (index < mesh.getMeshData().getPrimitiveCount(0) && index >= 0) {
            for (int x = 0; x < 3; x++) {
                if (store[x] == null) {
                    store[x] = new Vector3();
                }

                BufferUtils.populateFromBuffer(store[x], mesh.getMeshData().getVertexBuffer(), mesh.getMeshData()
                        .getIndexBuffer().get(mesh.getMeshData().getVertexIndex(index, x, 0)));
            }
        }
    }

    public static int[] getTriangleIndices(final Mesh mesh, int[] store) {
        // FIXME: hard coded section 0
        final int maxCount = mesh.getMeshData().getPrimitiveCount(0);
        if (store == null || store.length != maxCount) {
            store = new int[maxCount];
        }

        for (int i = 0, tLength = maxCount; i < tLength; i++) {
            store[i] = i;
        }
        return store;
    }
}
