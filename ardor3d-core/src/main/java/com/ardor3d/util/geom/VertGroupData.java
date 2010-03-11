/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.geom;

import java.util.EnumSet;
import java.util.Map;

import com.ardor3d.util.geom.GeometryTool.MatchCondition;
import com.google.common.collect.Maps;

public class VertGroupData {

    public static final int DEFAULT_GROUP = 0;

    private final Map<Integer, EnumSet<MatchCondition>> _groupConditions = Maps.newHashMap();
    private int[] _vertGroups = null;

    public VertGroupData() {}

    public void setGroupConditions(final int groupNumber, final EnumSet<MatchCondition> conditions) {
        _groupConditions.put(groupNumber, conditions);
    }

    public EnumSet<MatchCondition> getGroupConditions(final int groupNumber) {
        return _groupConditions.get(groupNumber);
    }

    public int getGroupForVertex(final int index) {
        if (_vertGroups != null) {
            return _vertGroups[index];
        }
        return DEFAULT_GROUP;
    }

    public void setVertGroups(final int[] vertGroupMap) {
        _vertGroups = vertGroupMap;
    }
}
