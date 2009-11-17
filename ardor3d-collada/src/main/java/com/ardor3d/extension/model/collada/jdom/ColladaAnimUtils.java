/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.jdom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.logging.Logger;

import org.jdom.DataConversionException;
import org.jdom.Element;

import com.ardor3d.extension.animation.skeletal.Joint;
import com.ardor3d.extension.animation.skeletal.Skeleton;
import com.ardor3d.extension.animation.skeletal.SkeletonPose;
import com.ardor3d.extension.animation.skeletal.SkinnedMesh;
import com.ardor3d.extension.model.collada.jdom.ColladaInputPipe.ParamType;
import com.ardor3d.extension.model.collada.jdom.data.GlobalData;
import com.ardor3d.extension.model.collada.jdom.data.MeshVertPairs;
import com.ardor3d.extension.model.collada.jdom.data.SkinData;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Transform;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.export.binary.BinaryExporter;
import com.ardor3d.util.export.binary.BinaryImporter;
import com.ardor3d.util.geom.BufferUtils;
import com.google.common.collect.Lists;

/**
 * TODO: document this class!
 * 
 */
public class ColladaAnimUtils {
    private static final Logger logger = Logger.getLogger(ColladaAnimUtils.class.getName());

    /**
     * Construct skin mesh(es) from the skin element and attach them (under a single new Node) to the given parent Node.
     * 
     * @param ardorParentNode
     *            Ardor3D Node to attach our skin node to.
     * @param instanceController
     *            the <instance_controller> element. We'll parse the skeleon reference from here.
     * @param controller
     *            the referenced <controller> element. Used for naming purposes.
     * @param skin
     *            our <skin> element.
     */
    @SuppressWarnings("unchecked")
    public static void buildSkinMeshes(final Node ardorParentNode, final Element instanceController,
            final Element controller, final Element skin) {
        final String skinSource = skin.getAttributeValue("source");

        final Element skinNodeEL = ColladaDOMUtil.findTargetWithId(skinSource);
        if (skinNodeEL == null || !"geometry".equals(skinNodeEL.getName())) {
            throw new ColladaException("Expected a mesh for skin source with url: " + skinSource
                    + " (line number is referring skin)", controller.getChild("skin"));
        }

        final Element geometry = skinNodeEL;

        final Node meshNode = ColladaMeshUtils.buildMesh(geometry);
        if (meshNode != null) {
            // Look for skeleton entries in the original <instance_controller> element
            final List<Element> skeletonRoots = Lists.newArrayList();
            for (final Element sk : (List<Element>) instanceController.getChildren("skeleton")) {
                final Element skroot = ColladaDOMUtil.findTargetWithId(sk.getText());
                if (skroot != null) {
                    // add as a possible root for when we need to locate a joint by name later.
                    skeletonRoots.add(skroot);
                } else {
                    throw new ColladaException("Unable to find node with id: " + sk.getText()
                            + ", referenced from skeleton " + sk, sk);
                }
            }

            // Read in our joints node
            final Element jointsEL = skin.getChild("joints");
            if (jointsEL == null) {
                throw new ColladaException("skin found without joints.", skin);
            }

            // Pull out our joint names and bind matrices
            final List<String> jointNames = Lists.newArrayList();
            final List<Transform> bindMatrices = Lists.newArrayList();
            final List<ColladaInputPipe.ParamType> paramTypes = Lists.newArrayList();

            for (final Element inputEL : (List<Element>) jointsEL.getChildren("input")) {
                final ColladaInputPipe pipe = new ColladaInputPipe(inputEL);
                final ColladaInputPipe.SourceData sd = pipe.getSourceData();
                if (pipe.getType() == ColladaInputPipe.Type.JOINT) {
                    final String[] namesData = sd.stringArray;
                    for (int i = sd.offset; i < namesData.length; i += sd.stride) {
                        jointNames.add(namesData[i]);
                        paramTypes.add(sd.paramType);
                    }
                } else if (pipe.getType() == ColladaInputPipe.Type.INV_BIND_MATRIX) {
                    final float[] floatData = sd.floatArray;
                    final FloatBuffer source = BufferUtils.createFloatBufferOnHeap(16);
                    for (int i = sd.offset; i < floatData.length; i += sd.stride) {
                        source.rewind();
                        source.put(floatData, i, 16);
                        source.flip();
                        final Matrix4 mat = new Matrix4().fromFloatBuffer(source);
                        bindMatrices.add(new Transform().fromHomogeneousMatrix(mat));
                    }
                }
            }

            // Make a joint array with name and inverse bind matrix
            final Joint[] joints = new Joint[jointNames.size()];
            for (int i = 0; i < joints.length; i++) {
                joints[i] = new Joint(jointNames.get(i));
                if (bindMatrices.size() > i) {
                    joints[i].setInverseBindPose(bindMatrices.get(i));
                }
            }

            // Use the skeleton information from the instance_controller to set the parent array locations on the
            // joints.
            for (int i = 0; i < joints.length; i++) {
                final Joint joint = joints[i];
                final ParamType paramType = paramTypes.get(i);
                final String searcher = paramType == ParamType.idref_param ? "id" : "sid";
                final String name = joint.getName();
                Element found = null;
                for (final Element root : skeletonRoots) {
                    if (name.equals(root.getAttributeValue(searcher))) {
                        found = root;
                    } else if (paramType == ParamType.idref_param) {
                        found = ColladaDOMUtil.findTargetWithId(name);
                    } else {
                        found = (Element) ColladaDOMUtil.selectSingleNode(root, ".//*[@sid='" + name + "']");
                    }

                    // Last resorts (bad exporters)
                    if (found == null) {
                        found = ColladaDOMUtil.findTargetWithId(name);
                    }
                    if (found == null) {
                        found = (Element) ColladaDOMUtil.selectSingleNode(root, ".//*[@name='" + name + "']");
                    }

                    if (found != null) {
                        break;
                    }
                }
                if (found == null) {
                    if (paramType == ParamType.idref_param) {
                        found = ColladaDOMUtil.findTargetWithId(name);
                    } else {
                        found = (Element) ColladaDOMUtil.selectSingleNode(geometry, "/*//visual_scene//*[@sid='" + name
                                + "']");
                    }

                    // Last resorts (bad exporters)
                    if (found == null) {
                        found = ColladaDOMUtil.findTargetWithId(name);
                    }
                    if (found == null) {
                        found = (Element) ColladaDOMUtil.selectSingleNode(geometry, "/*//visual_scene//*[@name='"
                                + name + "']");
                    }

                    if (found == null) {
                        throw new ColladaException("Unable to find joint with " + searcher + ": " + name, skin);
                    }
                }
                if (found.getParentElement() != null) {
                    String parName = found.getParentElement().getAttributeValue(searcher);

                    // Last resort (bad exporters)
                    if (parName == null) {
                        parName = found.getParentElement().getAttributeValue("id");
                    }
                    if (parName == null) {
                        parName = found.getParentElement().getAttributeValue("name");
                    }

                    if (parName != null) {
                        final int index = jointNames.indexOf(parName);
                        if (index >= 0) {
                            // found a valid index, so set on joint.
                            joint.setParentIndex((short) index);
                            continue;
                        }
                    }
                }
                // no parent, so it's a root bone
                joint.setParentIndex(Short.MAX_VALUE);
            }

            // Make our skeleton
            final Skeleton ourSkeleton = new Skeleton("skeleton", joints);
            final SkeletonPose skPose = new SkeletonPose(ourSkeleton);
            // Skeleton's default to bind position, so update the global transforms.
            skPose.updateTransforms();

            // Read in our vertex_weights node
            final Element weightsEL = skin.getChild("vertex_weights");
            if (weightsEL == null) {
                throw new ColladaException("skin found without vertex_weights.", skin);
            }

            // Pull out our per vertex joint indices and weights
            final List<Short> jointIndices = Lists.newArrayList();
            final List<Float> jointWeights = Lists.newArrayList();
            int indOff = 0, weightOff = 0;

            int maxOffset = 0;
            for (final Element inputEL : (List<Element>) weightsEL.getChildren("input")) {
                final ColladaInputPipe pipe = new ColladaInputPipe(inputEL);
                final ColladaInputPipe.SourceData sd = pipe.getSourceData();
                if (pipe.getOffset() > maxOffset) {
                    maxOffset = pipe.getOffset();
                }
                if (pipe.getType() == ColladaInputPipe.Type.JOINT) {
                    indOff = pipe.getOffset();
                    final String[] namesData = sd.stringArray;
                    for (int i = sd.offset; i < namesData.length; i += sd.stride) {
                        // XXX: the Collada spec says this could be -1?
                        final String name = namesData[i];
                        final int index = jointNames.indexOf(name);
                        if (index >= 0) {
                            jointIndices.add((short) index);
                        } else {
                            throw new ColladaException("Unknown joint accessed: " + name, inputEL);
                        }
                    }
                } else if (pipe.getType() == ColladaInputPipe.Type.WEIGHT) {
                    weightOff = pipe.getOffset();
                    final float[] floatData = sd.floatArray;
                    for (int i = sd.offset; i < floatData.length; i += sd.stride) {
                        jointWeights.add(floatData[i]);
                    }
                }
            }

            // Pull our values array
            int firstIndex = 0, count = 0;
            final int[] vals = ColladaDOMUtil.parseIntArray(weightsEL.getChild("v"));
            try {
                count = weightsEL.getAttribute("count").getIntValue();
            } catch (final DataConversionException e) {
                throw new ColladaException("Unable to parse count attribute.", weightsEL);
            }
            // use the vals to fill our vert weight map
            final int[][] vertWeightMap = new int[count][];
            int index = 0;
            for (final int length : ColladaDOMUtil.parseIntArray(weightsEL.getChild("vcount"))) {
                final int[] entry = new int[(maxOffset + 1) * length];
                vertWeightMap[index++] = entry;

                System.arraycopy(vals, (maxOffset + 1) * firstIndex, entry, 0, entry.length);

                firstIndex += length;
            }

            // Create a record for the global ColladaStorage.
            final String storeName = ColladaAnimUtils.getSkinStoreName(instanceController, controller);
            final SkinData skinDataStore = new SkinData(storeName);
            // add pose to store
            skinDataStore.setPose(skPose);

            // Create a base Node for our skin meshes
            final Node skinNode = new Node(meshNode.getName());
            // copy Node render states across.
            ColladaAnimUtils.copyRenderStates(meshNode, skinNode);
            // add node to store
            skinDataStore.setSkinBaseNode(skinNode);

            // Visit our Node and pull out any Mesh children. Turn them into SkinnedMeshes
            for (final Spatial spat : meshNode.getChildren()) {
                if (spat instanceof Mesh) {
                    final Mesh sourceMesh = (Mesh) spat;
                    final SkinnedMesh skMesh = new SkinnedMesh(sourceMesh.getName());
                    skMesh.setCurrentPose(skPose);

                    // copy mesh render states across.
                    ColladaAnimUtils.copyRenderStates(sourceMesh, skMesh);

                    // Use source mesh as bind pose data in the new SkinnedMesh
                    skMesh.setBindPoseData(sourceMesh.getMeshData());

                    // TODO: This is only needed for CPU skinning... consider a way of making it optional.
                    try {
                        // Copy bind pose to mesh data to setup for CPU skinning
                        skMesh.setMeshData(ColladaAnimUtils.copyMeshData(skMesh.getBindPoseData()));
                    } catch (final IOException e) {
                        e.printStackTrace();
                        throw new ColladaException("Unable to copy skeleton bind pose data.", geometry);
                    }

                    // Grab the MeshVertPairs from Global for this mesh.
                    final Collection<MeshVertPairs> vertPairsList = GlobalData.getInstance().getVertMappings().get(
                            geometry);
                    MeshVertPairs pairsMap = null;
                    if (vertPairsList != null) {
                        for (final MeshVertPairs pairs : vertPairsList) {
                            if (pairs.mesh == sourceMesh) {
                                pairsMap = pairs;
                            }
                        }
                    }

                    if (pairsMap == null) {
                        throw new ColladaException("Unable to locate pair map for geometry.", geometry);
                    }

                    // Use pairs map and vertWeightMap to build our weights and joint indices.
                    {
                        final FloatBuffer weightBuffer = BufferUtils.createFloatBuffer(pairsMap.indices.length * 4);
                        final ShortBuffer jointIndexBuffer = BufferUtils.createShortBuffer(pairsMap.indices.length * 4);
                        int j;
                        float sum = 0;
                        final float[] weights = new float[4];
                        final short[] indices = new short[4];
                        for (final int originalIndex : pairsMap.indices) {
                            j = 0;
                            sum = 0;

                            // get first 4 weights and joints at original index and add weights up to get divisor sum
                            final int[] data = vertWeightMap[originalIndex];
                            for (int i = 0; i < data.length && j < 4; i += maxOffset + 1) {
                                weights[j] = jointWeights.get(data[i + weightOff]);
                                indices[j] = jointIndices.get(data[i + indOff]);
                                sum += weights[j++];
                            }
                            // add extra padding as needed
                            while (j < 4) {
                                weights[j] = 0;
                                indices[j++] = 0;
                            }
                            // add weights to weightBuffer / sum
                            for (final float w : weights) {
                                weightBuffer.put(w / sum);
                            }
                            // add joint indices to jointIndexBuffer
                            jointIndexBuffer.put(indices);
                        }

                        skMesh.setWeights(weightBuffer);
                        skMesh.setJointIndices(jointIndexBuffer);
                    }

                    // add to the skinNode.
                    skinNode.attachChild(skMesh);

                    // Apply our bind pose to the skin mesh.
                    skMesh.applyPose();

                    // Update the model bounding.
                    skMesh.updateModelBound();

                    // add mesh to store
                    skinDataStore.getSkins().add(skMesh);
                }
            }

            // add to Node
            ardorParentNode.attachChild(skinNode);

            // Add skin record to storage.
            GlobalData.getInstance().getColladaStorage().getSkins().add(skinDataStore);
        }
    }

    /**
     * Retrieve a name to use for the skin node based on the element names.
     * 
     * @param ic
     *            instance_controller element.
     * @param controller
     *            controller element
     * @return name.
     * @see SkinData#SkinData(String)
     */
    private static String getSkinStoreName(final Element ic, final Element controller) {
        final String controllerName = controller.getAttributeValue("name", (String) null) != null ? controller
                .getAttributeValue("name", (String) null) : controller.getAttributeValue("id", (String) null);
        final String instanceControllerName = ic.getAttributeValue("name", (String) null) != null ? ic
                .getAttributeValue("name", (String) null) : ic.getAttributeValue("sid", (String) null);
        final String storeName = (controllerName != null ? controllerName : "")
                + (controllerName != null && instanceControllerName != null ? " : " : "")
                + (instanceControllerName != null ? instanceControllerName : "");
        return storeName;
    }

    /**
     * Copy the render states from our source Spatial to the destination Spatial. Does not recurse.
     * 
     * @param source
     * @param target
     */
    private static void copyRenderStates(final Spatial source, final Spatial target) {
        final EnumMap<StateType, RenderState> states = source.getLocalRenderStates();
        for (final RenderState state : states.values()) {
            target.setRenderState(state);
        }
    }

    /**
     * Clone the given MeshData object via deep copy using the Ardor3D BinaryExporter and BinaryImporter.
     * 
     * @param meshData
     *            the source to clone.
     * @return the clone.
     * @throws IOException
     *             if we have troubles during the clone.
     */
    private static MeshData copyMeshData(final MeshData meshData) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final BinaryExporter exporter = new BinaryExporter();
        exporter.save(meshData, bos);
        bos.flush();
        final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        final BinaryImporter importer = new BinaryImporter();
        final Savable sav = importer.load(bis);
        return (MeshData) sav;
    }

    /**
     * Construct morph mesh(es) from the <morph> element and attach them (under a single new Node) to the given parent
     * Node.
     * 
     * Note: This method current does not do anything but attach the referenced mesh since Ardor3D does not yet support
     * morph target animation.
     * 
     * @param ardorParentNode
     *            Ardor3D Node to attach our morph mesh to.
     * @param controller
     *            the referenced <controller> element. Used for naming purposes.
     * @param morph
     *            our <morph> element
     */
    public static void buildMorphMeshes(final Node ardorParentNode, final Element controller, final Element morph) {
        final String skinSource = morph.getAttributeValue("source");

        final Element skinNode = ColladaDOMUtil.findTargetWithId(skinSource);
        if (skinNode == null || !"geometry".equals(skinNode.getName())) {
            throw new ColladaException("Expected a mesh for morph source with url: " + skinSource
                    + " (line number is referring morph)", controller.getChild("morph"));
        }

        final Element geometry = skinNode;

        final Spatial baseMesh = ColladaMeshUtils.buildMesh(geometry);

        // TODO: support morph animations someday.
        ColladaAnimUtils.logger.warning("Morph target animation not yet supported.");

        // Just add mesh.
        if (baseMesh != null) {
            ardorParentNode.attachChild(baseMesh);
        }
    }
}
