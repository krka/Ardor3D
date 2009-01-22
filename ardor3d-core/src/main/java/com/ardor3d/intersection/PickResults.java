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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.ardor3d.math.Ray3;
import com.ardor3d.scenegraph.Mesh;

/**
 * <code>PickResults</code> contains information resulting from a pick test. The results will contain a list of every
 * node that was "struck" during a pick test. Distance can be used to order the results. If <code>checkDistance</code>
 * is set, objects will be ordered with the first element in the list being the closest picked object.
 */
public abstract class PickResults {

    private final List<PickData> nodeList;
    private boolean checkDistance;
    private DistanceComparator distanceCompare;

    /**
     * Constructor instantiates a new <code>PickResults</code> object.
     */
    public PickResults() {
        nodeList = new ArrayList<PickData>();
    }

    /**
     * remember modification of the list to allow sorting after all picks have been added - not each time.
     */
    private boolean modified = false;

    /**
     * Places a new geometry (enclosed in PickData) into the results list.
     * 
     * @param data
     *            the PickData to be placed in the results list.
     */
    public void addPickData(final PickData data) {
        nodeList.add(data);
        modified = true;
    }

    /**
     * <code>getNumber</code> retrieves the number of geometries that have been placed in the results.
     * 
     * @return the number of Mesh objects in the list.
     */
    public int getNumber() {
        return nodeList.size();
    }

    /**
     * Retrieves a geometry (enclosed in PickData) from a specific index.
     * 
     * @param i
     *            the index requested.
     * @return the data at the specified index.
     */
    public PickData getPickData(final int i) {
        if (modified) {
            if (checkDistance) {
                Collections.sort(nodeList, distanceCompare);
            }
            modified = false;
        }
        return nodeList.get(i);
    }

    /**
     * <code>clear</code> clears the list of all Mesh objects.
     */
    public void clear() {
        nodeList.clear();
    }

    /**
     * <code>addPick</code> generates an entry to be added to the list of picked objects. If checkDistance is true, the
     * implementing class should order the object.
     * 
     * @param ray
     *            the ray that was cast for the pick calculation.
     * @param g
     *            the object to add to the pick data.
     */
    public abstract void addPick(Ray3 ray, Mesh g);

    /**
     * Optional method that can be implemented by sub classes to define methods for handling picked objects. After
     * calculating all pick results this method is called.
     * 
     */
    public abstract void processPick();

    /**
     * Reports if these pick results will order the data by distance from the origin of the Ray.
     * 
     * @return true if objects will be ordered by distance, false otherwise.
     */
    public boolean willCheckDistance() {
        return checkDistance;
    }

    /**
     * Sets if these pick results will order the data by distance from the origin of the Ray.
     * 
     * @param checkDistance
     *            true if objects will be ordered by distance, false otherwise.
     */
    public void setCheckDistance(final boolean checkDistance) {
        this.checkDistance = checkDistance;
        if (checkDistance) {
            distanceCompare = new DistanceComparator();
        }
    }

    /**
     * Implementation of comparator that uses the distance set in the pick data to order the objects.
     */
    private class DistanceComparator implements Comparator<PickData> {

        public int compare(final PickData o1, final PickData o2) {
            if (o1.getRecord().getClosestDistance() <= o2.getRecord().getClosestDistance()) {
                return -1;
            }

            return 1;
        }
    }
}
