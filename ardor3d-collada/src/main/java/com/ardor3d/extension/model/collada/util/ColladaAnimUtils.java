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

import com.ardor3d.animations.reference.Joint;
import com.ardor3d.animations.reference.VertexInfluence;
import com.ardor3d.animations.reference.Skeleton;
import com.ardor3d.extension.model.collada.binding.ColladaException;
import com.ardor3d.extension.model.collada.binding.DaeList;
import com.ardor3d.extension.model.collada.binding.DaeTreeNode;
import com.ardor3d.extension.model.collada.binding.core.Collada;
import com.ardor3d.extension.model.collada.binding.core.DaeInput;
import com.ardor3d.extension.model.collada.binding.core.DaeInputShared;
import com.ardor3d.extension.model.collada.binding.core.DaeInputUnshared;
import com.ardor3d.extension.model.collada.binding.core.DaeNode;
import com.ardor3d.extension.model.collada.binding.core.DaeSkin;
import com.ardor3d.extension.model.collada.binding.core.DaeSource;
import com.ardor3d.extension.model.collada.binding.core.DaeType;
import com.ardor3d.math.Transform;
import com.google.common.collect.ImmutableList;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * TODO: document this class!
 *
 */
public class ColladaAnimUtils {
    private static HashMap<String, Skeleton> registeredSkeletons = new HashMap<String, Skeleton>();

    public static Joint createSkeleton(DaeNode node, DaeSkin skin) {
        if (node.getType() != DaeType.JOINT) {
            throw new ColladaException("Node is not of type JOINT", node);
        }

        if (skin.getVertexWeights() == null) {
            throw new ColladaException("Null vertex weights for skin", skin);
        }

        // get hold of map of <joint name, vertexinfluences> from the mesh and skin
        Map<String, List<VertexInfluence>> vertexInfluences = extractVertexInfluences(skin);

        return createJoint(node, vertexInfluences);
    }

    static Map<String, List<VertexInfluence>> extractVertexInfluences(DaeSkin skin) {
        Map<String, List<VertexInfluence>> result = new HashMap<String, List<VertexInfluence>>();

        DaeInputUnshared jointNamesInput = findUnsharedInput("JOINT", skin.getJoints().getInputs());

        String[] jointNames = getNamesFromSource(jointNamesInput);

        // figure out the float array of weights
        DaeInputShared weightsInput = findSharedInput("WEIGHT", skin.getVertexWeights().getInputs());

        float[] weightArray = getFloatsFromSource(weightsInput);
        int[] v = skin.getVertexWeights().getV().getData();


        // loop through the entries in the <vcount> array, with an inner loop that loops up to vcount and adds vertex influences each time

        // this is the index of the vertex whose influences we're working on
        int vertexIndex = 0;

        // this is the index into the 'v' array that we're currently looking at
        int vIndex = 0;

        for (int vcount : skin.getVertexWeights().getVcount().getData()) {
            for (int influenceCount = 0 ; influenceCount < vcount ; influenceCount++) {
                String jointName = jointNames[v[vIndex]];
                float weight = weightArray[v[vIndex + 1]];

                List<VertexInfluence> vertexInfluences = result.get(jointName);

                if (vertexInfluences == null) {
                    vertexInfluences = new LinkedList<VertexInfluence>();
                }

                vertexInfluences.add(new VertexInfluence(vertexIndex, weight));
                result.put(jointName, vertexInfluences);

                vIndex += 2;
            }

            vertexIndex++;
        }


        return result;
    }

    private static float[] getFloatsFromSource(DaeInput input) {
        DaeSource source = getSourceFromInput(input);

        if (source.getFloatArray() == null) {
            throw new ColladaException("Source doesn't have a float array", source);
        }

        return source.getFloatArray().getData();
    }

    private static String[] getNamesFromSource(DaeInput input) {
        DaeSource source = getSourceFromInput(input);

        if (source.getNameArray() == null) {
            throw new ColladaException("Source doesn't have a name array", source);
        }

        return source.getNameArray().getData();
    }

    private static DaeSource getSourceFromInput(DaeInput input) {
        Collada collada = input.getRootNode();

        DaeTreeNode sourceNode = collada.resolveUrl(input.getSource());

        if (!(sourceNode instanceof DaeSource)) {
            throw new ColladaException("Source url: " + input.getSource() + " didn't resolve to a DaeSource instance", input);
        }

        return (DaeSource) sourceNode;
    }

    private static DaeInputUnshared findUnsharedInput(String semantic, DaeList<DaeInputUnshared> inputs) {
        for (DaeInputUnshared input : inputs) {
            if (input.getSemantic().equals(semantic)) {
                return input;
            }
        }

        throw new ColladaException("No input with semantic '" + semantic + "' found", inputs);
    }

    private static DaeInputShared findSharedInput(String semantic, DaeList<DaeInputShared> inputs) {
        for (DaeInputShared input : inputs) {
            if (input.getSemantic().equals(semantic)) {
                return input;
            }
        }

        throw new ColladaException("No input with semantic '" + semantic + "' found", inputs);
    }

    private static Joint createJoint(DaeNode node, Map<String, List<VertexInfluence>> vertexInfluences) {

        List<Joint> children = new LinkedList<Joint>();

        if (node.getNodes() != null) {
            for (DaeNode child : node.getNodes()) {
                children.add(createJoint(child, vertexInfluences));
            }
         }

        // bind pose matrix, vertex influences, children
        Transform bindPoseTransform = ColladaNodeUtils.getNodeTransforms(node);

        List<VertexInfluence> influences = vertexInfluences.get(node.getName());

        if (influences == null) {
            System.out.println("No vertex influences found for joint with name: " + node.getName());
            influences = new LinkedList<VertexInfluence>();
        }

        return new Joint(bindPoseTransform, ImmutableList.copyOf(influences), ImmutableList.copyOf(children));
    }

    public static Skeleton findSkeleton(String skeletonUri) {
        return registeredSkeletons.get(skeletonUri);
    }

    public static void registerSkeleton(String skeletonUri, Skeleton skeleton) {
        registeredSkeletons.put(skeletonUri, skeleton);
    }
}
