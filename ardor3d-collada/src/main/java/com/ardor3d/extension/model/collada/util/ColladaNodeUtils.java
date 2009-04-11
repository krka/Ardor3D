/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.util;

import java.util.logging.Logger;

import com.ardor3d.extension.model.collada.binding.DaeTreeNode;
import com.ardor3d.extension.model.collada.binding.ColladaException;
import com.ardor3d.extension.model.collada.binding.core.Collada;
import com.ardor3d.extension.model.collada.binding.core.DaeController;
import com.ardor3d.extension.model.collada.binding.core.DaeGeometry;
import com.ardor3d.extension.model.collada.binding.core.DaeInstanceController;
import com.ardor3d.extension.model.collada.binding.core.DaeInstanceGeometry;
import com.ardor3d.extension.model.collada.binding.core.DaeInstanceNode;
import com.ardor3d.extension.model.collada.binding.core.DaeLookat;
import com.ardor3d.extension.model.collada.binding.core.DaeMatrix;
import com.ardor3d.extension.model.collada.binding.core.DaeNode;
import com.ardor3d.extension.model.collada.binding.core.DaeRotate;
import com.ardor3d.extension.model.collada.binding.core.DaeScale;
import com.ardor3d.extension.model.collada.binding.core.DaeTransform;
import com.ardor3d.extension.model.collada.binding.core.DaeTranslate;
import com.ardor3d.extension.model.collada.binding.core.DaeVisualScene;
import com.ardor3d.extension.model.collada.binding.core.DaeType;
import com.ardor3d.extension.model.collada.binding.core.DaeSkin;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.animations.reference.Joint;
import com.ardor3d.animations.reference.Skeleton;
import com.ardor3d.animations.reference.Animatable;
import com.ardor3d.animations.runtime.AnimationRegistry;
import com.ardor3d.animations.runtime.AnimatableInstance;
import com.ardor3d.animations.runtime.SkeletonInstance;
import com.google.common.collect.ImmutableList;

public class ColladaNodeUtils {
    private static final Logger logger = Logger.getLogger(ColladaNodeUtils.class.getName());

    public static Node getVisualScene(final String id, final Collada collada, AnimationRegistry animationRegistry) {
        final DaeVisualScene visualScene = (DaeVisualScene) Collada.findLibraryEntry(id, collada
                .getLibraryVisualScenes());

        if (visualScene != null) {
            final Node sceneRoot = new Node(visualScene.getName() != null ? visualScene.getName()
                    : DaeVisualScene.COLLADA_ROOT_NAME);

            // Load each sub node and attach
            for (final DaeNode n : visualScene.getNodes()) {
                System.out.println("node: " + n);
                if (n.getType() == DaeType.NODE) {
                    final Node subNode = buildNode(n, animationRegistry);
                    if (subNode != null) {
                        sceneRoot.attachChild(subNode);
                    }
                }
            }

            return sceneRoot;
        }
        return null;
    }

    public static Node getNode(final String id, final Collada collada) {
        final DaeNode node = (DaeNode) Collada.findLibraryEntry(id, collada.getLibraryNodes());

        if (node == null) {
            throw new ColladaException("No node with id: " + id + " found", collada);
        }

        return buildNode(node);
    }

    public static Node buildNode(final DaeNode dNode) {
        return buildNode(dNode, null);
    }

    public static Node buildNode(final DaeNode dNode, AnimationRegistry animationRegistry) {
        if (dNode.getType() == DaeType.JOINT) {
            // this is an internal error; we don't want to construct nodes for joints
            throw new ColladaException("buildNode(DaeNode) called with JOINT instead of NODE type node", dNode);
        }

        final Node node = new Node(dNode.getName());

        // process any transform information.
        if (dNode.getTransforms() != null) {
            final Transform localTransform = getNodeTransforms(dNode);

            node.setTransform(localTransform);
        }

        // process any instance geometries
        if (dNode.getInstanceGeometries() != null) {
            final Collada root = dNode.getRootNode();
            for (final DaeInstanceGeometry ig : dNode.getInstanceGeometries()) {
                ColladaMaterialUtils.bindMaterials(ig.getBindMaterial(), root);

                final String id = ig.getUrl().substring(1);
                final Spatial mesh = ColladaMeshUtils.getGeometryMesh(id, root);
                if (mesh != null) {
                    node.attachChild(mesh);
                }

                ColladaMaterialUtils.unbindMaterials(ig.getBindMaterial(), root);
            }
        }

        // process any instance geometries
        if (dNode.getInstanceControllers() != null) {
            final Collada root = dNode.getRootNode();
            for (final DaeInstanceController ic : dNode.getInstanceControllers()) {
                ColladaMaterialUtils.bindMaterials(ic.getBindMaterial(), root);

                final String id = ic.getUrl().substring(1);

                final DaeController controller = (DaeController) Collada.findLibraryEntry(id, root
                        .getLibraryControllers());

                if (controller == null) {
                    throw new ColladaException("Unable to find controller with id: " + id + ", referenced from node " + dNode, dNode);
                }

                final DaeSkin skin = controller.getSkin();

                if (skin != null) {
                    final String skinSource = skin.getSource();

                    final DaeTreeNode skinNode = root.resolveUrl(skinSource);
                    if (!(skinNode instanceof DaeGeometry)) {
                        throw new ColladaException("Expected a mesh for skin source with url: " + skinSource + " (line number is referring skin)", controller.getSkin());
                    }

                    final DaeGeometry geometry = (DaeGeometry) skinNode;

                    Spatial mesh = ColladaMeshUtils.buildMesh(geometry);

                    if (animationRegistry == null) {
                        // For now, just grab the associated skin mesh and add to the scene.
                        if (mesh != null) {
                            node.attachChild(mesh);
                        }
                    }
                    else {
                        Animatable animatable = buildAndRegisterAnimatable(mesh, skin, ic, animationRegistry);

                        AnimatableInstance animatableInstance = new AnimatableInstance(animatable, node, new SkeletonInstance(animatable.getSkeleton()));

                        animationRegistry.registerAnimatableInstance(animatableInstance);
                    }

                }

                ColladaMaterialUtils.unbindMaterials(ic.getBindMaterial(), root);
            }
        }

        // process any instance nodes
        if (dNode.getInstanceNodes() != null) {
            for (final DaeInstanceNode in : dNode.getInstanceNodes()) {
                final String id = in.getUrl().substring(1);
                final Node subNode = getNode(id, dNode.getRootNode());
                if (subNode != null) {
                    node.attachChild(subNode);
                }
            }
        }

        // process any concrete child nodes.
        if (dNode.getNodes() != null) {
            for (final DaeNode n : dNode.getNodes()) {
                final Node subNode = buildNode(n);
                if (subNode != null) {
                    node.attachChild(subNode);
                }
            }
        }

        return node;
    }

    private static Animatable buildAndRegisterAnimatable(Spatial mesh, DaeSkin skin, DaeInstanceController ic, AnimationRegistry animationRegistry) {
        Collada root = skin.getRootNode();
        
        if (ic.getSkeletons().size() != 1) {
            throw new ColladaException("This version of the collada importer can only handle exactly 1 skeleton per instance controller, found " + ic.getSkeletons().size(), ic);
        }

        String skeletonUri = ic.getSkeletons().get(0).getName();

        Skeleton skeleton = animationRegistry.getSkeleton(skeletonUri);

        if (skeleton == null) {
            DaeTreeNode skeletonTreeNode = root.resolveUrl(skeletonUri);

            if (!(skeletonTreeNode instanceof DaeNode)) {
                throw new ColladaException("Expected a DaeNode in reference " + skeletonUri + ", found " + skeletonTreeNode, ic);
            }

            Joint rootJoint = ColladaAnimUtils.createSkeleton((DaeNode) skeletonTreeNode, skin);


            skeleton = new Skeleton(skeletonTreeNode.getId(), rootJoint);

            animationRegistry.registerSkeleton(skeletonUri, skeleton);
        }

        Matrix4 bindShapeMatrix = ColladaMathUtils.toMatrix4(skin.getBindShapeMatrix());
        ImmutableList<Spatial> bindShape = ImmutableList.of(mesh);

        Animatable animatable = new Animatable(createIdFor(ic), bindShapeMatrix, bindShape, ImmutableList.of(skeleton));

        animationRegistry.registerAnimatable(animatable);

        return animatable;
    }

    private static String createIdFor(DaeInstanceController instanceController) {
        if (instanceController.getId() != null && instanceController.getId().length() > 0) {
            return instanceController.getId();
        }

        // this should be unique if ugly.
        return instanceController.getUrl() + "-" + instanceController.hashCode();
    }

    static Transform getNodeTransforms(DaeNode dNode) {
        final Transform localTransform = new Transform();
        for (final DaeTransform transform : dNode.getTransforms()) {
            if (transform instanceof DaeTranslate) {
                final DaeTranslate t = (DaeTranslate) transform;
                localTransform.translate(t.getDoubleValues()[0], t.getDoubleValues()[1], t.getDoubleValues()[2]);
            } else if (transform instanceof DaeRotate) {
                final DaeRotate r = (DaeRotate) transform;
                final Matrix3 rotate = new Matrix3().fromAngleAxis(r.getDoubleValues()[3] * MathUtils.DEG_TO_RAD,
                        new Vector3(r.getDoubleValues()[0], r.getDoubleValues()[1], r.getDoubleValues()[2]));
                rotate.multiply(localTransform.getMatrix(), rotate);
                if (localTransform.isRotationMatrix()) {
                    localTransform.setRotation(rotate);
                } else {
                    localTransform.setMatrix(rotate);
                }
            } else if (transform instanceof DaeScale) {
                final DaeScale s = (DaeScale) transform;
                final Vector3 scale = new Vector3(s.getDoubleValues()[0], s.getDoubleValues()[1], s
                        .getDoubleValues()[2]);
                scale.multiplyLocal(localTransform.getScale());
                localTransform.setScale(scale);
            } else if (transform instanceof DaeMatrix) {
                // Note: This will not preserve skew.
                final DaeMatrix m = (DaeMatrix) transform;
                final Matrix4 matrix = new Matrix4().fromArray(m.getDoubleValues());
                final Matrix4 local = localTransform.getHomogeneousMatrix(null).multiplyLocal(matrix);
                localTransform.fromHomogeneousMatrix(local);
            } else if (transform instanceof DaeLookat) {
                // Note: This replaces any currently accumulated transforms.
                final DaeLookat l = (DaeLookat) transform;
                final Vector3 pos = new Vector3(l.getDoubleValues()[0], l.getDoubleValues()[1],
                        l.getDoubleValues()[2]);
                final Vector3 target = new Vector3(l.getDoubleValues()[3], l.getDoubleValues()[4], l
                        .getDoubleValues()[5]);
                final Vector3 up = new Vector3(l.getDoubleValues()[6], l.getDoubleValues()[7],
                        l.getDoubleValues()[8]);
                final Matrix3 rot = new Matrix3();
                rot.lookAt(target.subtractLocal(pos), up);
                localTransform.setRotation(rot);
                localTransform.setTranslation(pos);
            } else {
                logger.warning("transform not currently supported: " + transform.getClass().getCanonicalName());
            }
        }
        return localTransform;
    }
}
