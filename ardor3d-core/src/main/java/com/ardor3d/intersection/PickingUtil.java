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

import java.util.List;

import com.ardor3d.bounding.CollisionTree;
import com.ardor3d.bounding.CollisionTreeManager;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.PickingHint;

public abstract class PickingUtil {
    public static void findPick(final Spatial spatial, final Ray3 ray, final PickResults results) {
        if (!spatial.getSceneHints().isPickingHintEnabled(PickingHint.Pickable)) {
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
                // find the primitive that is being hit.
                // add this node and the primitive to the PickResults list.
                results.addPick(ray, mesh);
            }
        }
    }

    public static void findCollisions(final Spatial spatial, final Spatial scene, final CollisionResults results) {
        if (spatial == scene || spatial.getWorldBound() == null
                || !spatial.getSceneHints().isPickingHintEnabled(PickingHint.Collidable)
                || !scene.getSceneHints().isPickingHintEnabled(PickingHint.Collidable)) {
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

    public static void findTrianglePick(final Mesh mesh, final Ray3 toTest, final List<PrimitiveKey> results) {
        if (mesh.getWorldBound() == null || !mesh.getSceneHints().isPickingHintEnabled(PickingHint.Pickable)) {
            return;
        }

        if (mesh.getWorldBound().intersects(toTest)) {
            final CollisionTree ct = CollisionTreeManager.getInstance().getCollisionTree(mesh);
            if (ct != null) {
                ct.getBounds().transform(mesh.getWorldTransform(), ct.getWorldBounds());
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
        if (!testMesh.getSceneHints().isPickingHintEnabled(PickingHint.Collidable)
                || !toCheck.getSceneHints().isPickingHintEnabled(PickingHint.Collidable)) {
            return false;
        }

        final CollisionTree thisCT = CollisionTreeManager.getInstance().getCollisionTree(testMesh);
        final CollisionTree checkCT = CollisionTreeManager.getInstance().getCollisionTree(toCheck);

        if (thisCT == null || checkCT == null) {
            return false;
        }

        final ReadOnlyTransform worldTransform = testMesh.getWorldTransform();
        thisCT.getBounds().transform(worldTransform, thisCT.getWorldBounds());
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
    public static void findPrimitiveCollision(final Mesh testMesh, final Mesh toCheck,
            final List<PrimitiveKey> testIndex, final List<PrimitiveKey> otherIndex) {
        if (!testMesh.getSceneHints().isPickingHintEnabled(PickingHint.Collidable)
                || !toCheck.getSceneHints().isPickingHintEnabled(PickingHint.Collidable)) {
            return;
        }

        final CollisionTree myTree = CollisionTreeManager.getInstance().getCollisionTree(testMesh);
        final CollisionTree otherTree = CollisionTreeManager.getInstance().getCollisionTree(toCheck);

        if (myTree == null || otherTree == null) {
            return;
        }

        myTree.getBounds().transform(testMesh.getWorldTransform(), myTree.getWorldBounds());

        myTree.intersect(otherTree, testIndex, otherIndex);
    }

    public static boolean hasCollision(final Spatial spatial, final Spatial scene, final boolean checkPrimitives) {
        if (spatial == scene || spatial.getWorldBound() == null
                || !spatial.getSceneHints().isPickingHintEnabled(PickingHint.Collidable)
                || !scene.getSceneHints().isPickingHintEnabled(PickingHint.Collidable)) {
            return false;
        }

        if (spatial instanceof Node) {
            final Node node = (Node) spatial;

            if (node.getWorldBound().intersects(scene.getWorldBound())) {
                if (node.getNumberOfChildren() == 0 && !checkPrimitives) {
                    return true;
                }
                // further checking needed.
                for (int i = 0; i < node.getNumberOfChildren(); i++) {
                    if (PickingUtil.hasCollision(node.getChild(i), scene, checkPrimitives)) {
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
                        if (PickingUtil.hasCollision(mesh, parent.getChild(i), checkPrimitives)) {
                            return true;
                        }
                    }

                    return false;
                }

                if (!checkPrimitives) {
                    return true;
                }

                return PickingUtil.hasTriangleCollision(mesh, (Mesh) scene);
            }

            return false;

        }

        return false;
    }
}
