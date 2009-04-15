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

import com.ardor3d.animations.reference.VertexInfluence;
import com.ardor3d.extension.model.collada.binding.DaeList;
import com.ardor3d.extension.model.collada.binding.core.Collada;
import com.ardor3d.extension.model.collada.binding.core.DaeFloatArray;
import com.ardor3d.extension.model.collada.binding.core.DaeInputShared;
import com.ardor3d.extension.model.collada.binding.core.DaeInputUnshared;
import com.ardor3d.extension.model.collada.binding.core.DaeJoints;
import com.ardor3d.extension.model.collada.binding.core.DaeNameArray;
import com.ardor3d.extension.model.collada.binding.core.DaeSimpleFloatArray;
import com.ardor3d.extension.model.collada.binding.core.DaeSimpleIntegerArray;
import com.ardor3d.extension.model.collada.binding.core.DaeSkin;
import com.ardor3d.extension.model.collada.binding.core.DaeSource;
import com.ardor3d.extension.model.collada.binding.core.DaeVertexWeights;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class ColladaAnimUtilsTest {
    Collada collada;


    @Before
    public void setup() throws Exception {
        collada = new Collada();
    }

    // Test data represents the following Collada:
    //    <skin>
    //      <source id="joints"/>
    //      <source id="weights"/>
    //      <vertex_weights count="4">
    //        <input semantic="JOINT" source="#joints"/>
    //        <input semantic="WEIGHT" source="#weights"/>
    //        <vcount>2 1 1 3</vcount>
    //        <v>
    //           0 1  1 2
    //           1 4
    //           2 4
    //           3 1  2 2  1 0
    //        </v>
    //      </vertex_weights>
    //    </skin>
    //
    // joint names: zero one two three
    // weights: 0.1 1.1 2.2 3.3 4.4

    @Test
    public void testExtractVertexInfluences() throws Exception {
        DaeSimpleFloatArray bindShapeMatrix = new DaeSimpleFloatArray();

        bindShapeMatrix.setTextData("1 0 0 0 0 1 0 0 0 0 1 0 0 0 0 1");

        DaeList<DaeSource> sources = new DaeList<DaeSource>();

        DaeNameArray names = new DaeNameArray();
        names.setTextData("zero one two three");

        DaeFloatArray weights = new DaeFloatArray();
        weights.setTextData("0.1 1.1 2.2 3.3 4.4");

        DaeSource jointNameSource = DaeSource.createNameArraySource(names);
        DaeSource weightsSource = DaeSource.createFloatArraySource(weights);

        sources.add(jointNameSource);
        sources.add(weightsSource);

        DaeList<DaeInputUnshared> jointInputs = new DaeList<DaeInputUnshared>();

        DaeInputUnshared jointInput = new DaeInputUnshared("#jointNamesSource", "JOINT", collada);

        jointInputs.add(jointInput);

        DaeJoints joints = new DaeJoints(jointInputs);

        DaeList<DaeInputShared> vertexWeightInputs = new DaeList<DaeInputShared>();

        final DaeInputShared vwJointsInput = new DaeInputShared(0, "#jointNamesSource", "JOINT", 0, collada);
        final DaeInputShared vwWeightInput = new DaeInputShared(0, "#weightsSource", "WEIGHT", 0, collada);

        vertexWeightInputs.add(vwJointsInput);
        vertexWeightInputs.add(vwWeightInput);

        DaeSimpleIntegerArray vcount = new DaeSimpleIntegerArray();
        DaeSimpleIntegerArray v = new DaeSimpleIntegerArray();

        vcount.setTextData("2 1 1 3");
        v.setTextData(
                "0 1  1 2 " +
                "1 4 " +
                "2 4 " +
                "3 1  2 2  1 0");

        DaeVertexWeights vertexWeights = new DaeVertexWeights(4, vertexWeightInputs, vcount, v);

        DaeSkin skin = new DaeSkin("source", bindShapeMatrix, sources, joints, vertexWeights);

        collada.mapId("jointNamesSource", jointNameSource);
        collada.mapId("weightsSource", weightsSource);

        Map<String, List<VertexInfluence>> result = ColladaAnimUtils.extractVertexInfluences(skin);

        checkVertexInfluences("zero", result, new int[] { 0 } , new double[] { 1.1f });
        checkVertexInfluences("one", result, new int[] { 0, 1, 3 } , new double[] { 2.2f, 4.4f, 0.1f });
        checkVertexInfluences("two", result, new int[] { 2, 3 } , new double[] { 4.4f, 2.2f });
        checkVertexInfluences("three", result, new int[] { 3 } , new double[] { 1.1f });

    }

    private void checkVertexInfluences(String jointName, Map<String, List<VertexInfluence>> influenceMap, int[] vertexIndices, double[] weights) {
        List<VertexInfluence> influences = influenceMap.get(jointName);

        assertNotNull("influences mapped", influences);
        assertEquals("#infs", weights.length, influences.size());

        for (int i = 0 ; i < weights.length ; i++) {
            assertTrue("weight #" + i, Double.compare(weights[i], influences.get(i).getWeight()) < 0.0001);
            assertEquals("index #" + i, vertexIndices[i], influences.get(i).getVertexIndex());
        }
    }
}
