/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.Camera.FrustumIntersect;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.geom.BufferUtils;

/**
 * ClipmapLevel
 */
public class ClipmapLevel extends Mesh {

    /**
     * Precalculated useful variables.
     */
    private final int doubleVertexDistance; // vertexDistance * 2;
    private final int frameDistance; // (frameSize - 1) * vertexDistance

    /**
     * Gets the distance between two horizontal/vertical vertices. The finest level L = 0 has a distance of
     * vertexDistance = 1. Then every level it doubles so vertexDistance is always 2^L
     */
    private final int vertexDistance;

    /**
     * Gets the levelindex of current clip. The finest level has the index L = 0
     */
    private final int levelIndex;

    /**
     * Gets framesize in number of vertices between outer border of current clip and outer border of next finer (inner)
     * clip.
     */
    private final int frameSize;

    /**
     * Gets the width of a clip in number of vertices. This is always one less than power of two (2^x - 1)
     */
    private final int clipSideSize;

    /**
     * Gets the region of the current clip.
     */
    private final Region clipRegion;

    /**
     * Value that indicates the height scaling. It is also represents the maximum terrain height.
     */
    private final float heightScale;

    private float heightRangeMin = 0.0f;
    private float heightRangeMax = 1.0f;

    /**
     * Index to indicate how much vertices are added to the triangle strip.
     */
    private int stripIndex = 0;

    /**
     * The used terrain heightfield. This must be set per reference so all clip levels share the same memory for that
     * variable. The values are between 0.0f and 1.0f
     */
    private final HeightmapPyramid heightmapPyramid;

    /**
     * Number of components for vertices
     */
    private static final int VERT_SIZE = 4;

    /**
     * Camera frustum to test clipmap tiles against for culling
     */
    private final Camera clipmapTestFrustum;

    private final Vector3 transformedFrustumPos = new Vector3();

    /**
     * Bounding box used for culling
     */
    private final BoundingBox frustumCheckBounds = new BoundingBox();

    /**
     * Possible nio speedup when storing indices
     */
    private int[] tmpIndices;

    /**
     * Creates a new clipmaplevel.
     * 
     * @param levelIndex
     *            Levelindex of the clipmap. If is 0 this will be the finest level
     * @param clipSideSize
     *            Number of vertices per clipside. Must be one less than power of two.
     * @param heightScale
     *            Maximum terrainheight and heightscale
     * @param fieldsize
     *            Width and heightvalue of the heightfield
     * @param heightfield
     *            Heightvalues with a range of 0.0f - 1.0f
     * @exception Exception
     */
    public ClipmapLevel(final int levelIndex, final Camera clipmapTestFrustum, final int clipSideSize,
            final float heightScale, final HeightmapPyramid heightmapPyramid) throws Exception {
        super("Clipmap Level " + levelIndex);

        // Check some exception cases
        if (levelIndex < 0) {
            throw new Exception("levelIndezx must be positive");
        }
        if (!MathUtils.isPowerOfTwo(clipSideSize + 1)) {
            throw new Exception("clipSideSize must be one less than power of two");
        }

        // Apply the values
        this.clipmapTestFrustum = clipmapTestFrustum;
        this.heightmapPyramid = heightmapPyramid;

        this.levelIndex = levelIndex;
        this.heightScale = heightScale;
        this.clipSideSize = clipSideSize;

        vertexDistance = (int) Math.pow(2, levelIndex);
        frameSize = (clipSideSize + 1) / 4;
        doubleVertexDistance = vertexDistance * 2;
        frameDistance = (frameSize - 1) * vertexDistance;
        clipRegion = new Region(0, 0, (clipSideSize - 1) * vertexDistance, (clipSideSize - 1) * vertexDistance);

        // Initialize the vertices
        initialize();
    }

    /**
     * Initializes the vertices and indices.
     */
    private void initialize() {
        getMeshData().setIndexMode(IndexMode.TriangleStrip);

        // clipSideSize is the number of vertices per clipmapside, so number of all vertices is clipSideSize *
        // clipSideSize
        final FloatBuffer vertices = BufferUtils.createVector4Buffer(clipSideSize * clipSideSize);
        getMeshData().setVertexCoords(new FloatBufferData(vertices, 4));

        // final FloatBuffer textureCoords = BufferUtils.createVector2Buffer(N * N);
        // getMeshData().setTextureBuffer(textureCoords, 0);

        // Go through all vertices of the current clip and update their height
        for (int z = 0; z < clipSideSize; z++) {
            for (int x = 0; x < clipSideSize; x++) {
                updateVertex(x * vertexDistance, z * vertexDistance);
            }
        }

        final int indicesSize = 4 * (3 * frameSize * frameSize + (clipSideSize * clipSideSize) / 2 + 4 * frameSize - 10);
        final IntBuffer indices = BufferUtils.createIntBuffer(indicesSize);
        tmpIndices = new int[indicesSize];
        getMeshData().setIndexBuffer(indices);

        // Go through all rows and fill them with vertexindices.
        for (int z = 0; z < clipSideSize - 1; z++) {
            fillRow(0, clipSideSize - 1, z, z + 1);
        }
    }

    /**
     * Update clipmap vertices
     * 
     * @param center
     */
    public void updateVertices() {
        getWorldTransform().applyInverse(clipmapTestFrustum.getLocation(), transformedFrustumPos);
        final int cx = (int) transformedFrustumPos.getX();
        final int cz = (int) transformedFrustumPos.getZ();

        // Store the old position to be able to recover it if needed
        final int oldX = clipRegion.getX();
        final int oldZ = clipRegion.getY();

        // Calculate the new position
        clipRegion.setX(cx - ((clipSideSize + 1) * vertexDistance / 2));
        clipRegion.setY(cz - ((clipSideSize + 1) * vertexDistance / 2));

        // Calculate the modulo to doubleVertexDistance of the new position.
        // This makes sure that the current level always fits in the hole of the
        // coarser level. The gridspacing of the coarser level is vertexDistance * 2, so here doubleVertexDistance.
        final int modX = MathUtils.moduloPositive(clipRegion.getX(), doubleVertexDistance);
        final int modY = MathUtils.moduloPositive(clipRegion.getY(), doubleVertexDistance);
        clipRegion.setX(clipRegion.getX() + doubleVertexDistance - modX);
        clipRegion.setY(clipRegion.getY() + doubleVertexDistance - modY);

        // Calculate the moving distance
        final int dx = (clipRegion.getX() - oldX);
        final int dz = (clipRegion.getY() - oldZ);

        updateVertices(dx, dz);
    }

    public void regenerate() {
        updateVertices(clipRegion.getWidth(), clipRegion.getHeight());
    }

    /**
     * 
     * @param cx
     * @param cz
     */
    private void updateVertices(final int dx, final int dz) {
        // Create some better readable variables.
        // This are just the bounds of the current level (the new region).
        final int xmin = clipRegion.getLeft();
        final int xmax = clipRegion.getRight();
        final int zmin = clipRegion.getTop();
        final int zmax = clipRegion.getBottom();

        // Update the L shaped region.
        // This replaces the old data with the new one.
        if (dz > 0) {
            // Center moved in positive z direction.
            for (int z = zmin; z <= zmax - dz; z += vertexDistance) {
                if (dx > 0) {
                    // Center moved in positive x direction.
                    // Update the right part of the L shaped region.
                    for (int x = xmax - dx + vertexDistance; x <= xmax; x += vertexDistance) {
                        updateVertex(x, z);
                    }
                } else if (dx < 0) {
                    // Center moved in negative x direction.
                    // Update the left part of the L shaped region.
                    for (int x = xmin; x <= xmin - dx - vertexDistance; x += vertexDistance) {
                        updateVertex(x, z);
                    }
                }
            }

            for (int z = zmax - dz + vertexDistance; z <= zmax; z += vertexDistance) {
                // Update the bottom part of the L shaped region.
                for (int x = xmin; x <= xmax; x += vertexDistance) {
                    updateVertex(x, z);
                }
            }
        } else {
            // Center moved in negative z direction.
            for (int z = zmin; z <= zmin - dz - vertexDistance; z += vertexDistance) {
                // Update the top part of the L shaped region.
                for (int x = xmin; x <= xmax; x += vertexDistance) {
                    updateVertex(x, z);
                }
            }

            for (int z = zmin - dz; z <= zmax; z += vertexDistance) {
                if (dx > 0) {
                    // Center moved in poistive x direction.
                    // Update the right part of the L shaped region.
                    for (int x = xmax - dx + vertexDistance; x <= xmax; x += vertexDistance) {
                        updateVertex(x, z);
                    }
                } else if (dx < 0) {
                    // Center moved in negative x direction.
                    // Update the left part of the L shaped region.
                    for (int x = xmin; x <= xmin - dx - vertexDistance; x += vertexDistance) {
                        updateVertex(x, z);
                    }
                }
            }
        }
    }

    /**
     * Updates the height of a vertex at the specified position. The coordinates may be some values, even outside the
     * map.
     * 
     * @param x
     * @param z
     */
    private void updateVertex(final int x, final int z) {
        // Map the terraincoordinates to arraycoordinates.
        final int posx = MathUtils.moduloPositive(x / vertexDistance, clipSideSize);
        final int posy = MathUtils.moduloPositive(z / vertexDistance, clipSideSize);

        final FloatBuffer vertices = getMeshData().getVertexBuffer();

        // Set both heightvalues to zero first.
        final int index = posx + posy * clipSideSize;
        vertices.put(index * VERT_SIZE + 0, x); // x
        vertices.put(index * VERT_SIZE + 2, z); // z

        // Map the terraincoordinates to the heightmap
        final int mapX = x / vertexDistance;
        final int mapZ = z / vertexDistance;

        // Get the height of current coordinates
        // and set both heightvalues to that height.
        final float height = heightmapPyramid.getHeight(levelIndex, mapX, mapZ) * heightScale;
        vertices.put(index * VERT_SIZE + 1, height); // y
        if (levelIndex >= heightmapPyramid.getHeightmapCount() - 1) {
            vertices.put(index * VERT_SIZE + 3, height); // w
        } else {
            // indices of heightvalues we can use
            // for the second height to avoid cracks
            float xLow = (x / vertexDistance);
            float zLow = (z / vertexDistance);
            if (xLow < 0) {
                xLow -= 1.0f;
            }
            if (zLow < 0) {
                zLow -= 1.0f;
            }

            final int x1 = (int) (xLow / 2.0f);
            int z1 = (int) (zLow / 2.0f);
            int x2 = x1;
            int z2 = z1;

            if (((mapX) % 2) == 0) {
                if (((mapZ) % 2) == 0) {
                    //
                } else {
                    z2++;
                }
            } else {
                if (((mapZ) % 2) == 0) {
                    x2++;
                } else {
                    x2++;
                    z1++;
                }
            }

            // Apply the median of the coarser heightvalues to the W value
            final float coarser1 = heightmapPyramid.getHeight(levelIndex + 1, x1, z1);
            final float coarser2 = heightmapPyramid.getHeight(levelIndex + 1, x2, z2);
            vertices.put(index * VERT_SIZE + 3, (coarser1 + coarser2) * heightScale * 0.5f); // w
        }
    }

    /**
     * Updates the whole indexarray.
     * 
     * @param nextFinerLevel
     * @param frustum
     */
    public void updateIndices(final ClipmapLevel nextFinerLevel) {
        // set the stripindex to zero. We start count vertices from here.
        // The stripindex will tell us how much of the array is used.
        stripIndex = 0;

        // MxM Block 1
        fillBlock(clipRegion.getLeft(), clipRegion.getLeft() + frameDistance, clipRegion.getTop(), clipRegion.getTop()
                + frameDistance);

        // MxM Block 2
        fillBlock(clipRegion.getLeft() + frameDistance, clipRegion.getLeft() + 2 * frameDistance, clipRegion.getTop(),
                clipRegion.getTop() + frameDistance);

        // MxM Block 3
        fillBlock(clipRegion.getRight() - 2 * frameDistance, clipRegion.getRight() - frameDistance,
                clipRegion.getTop(), clipRegion.getTop() + frameDistance);

        // MxM Block 4
        fillBlock(clipRegion.getRight() - frameDistance, clipRegion.getRight(), clipRegion.getTop(), clipRegion
                .getTop()
                + frameDistance);

        // MxM Block 5
        fillBlock(clipRegion.getLeft(), clipRegion.getLeft() + frameDistance, clipRegion.getTop() + frameDistance,
                clipRegion.getTop() + 2 * frameDistance);

        // MxM Block 6
        fillBlock(clipRegion.getRight() - frameDistance, clipRegion.getRight(), clipRegion.getTop() + frameDistance,
                clipRegion.getTop() + 2 * frameDistance);

        // MxM Block 7
        fillBlock(clipRegion.getLeft(), clipRegion.getLeft() + frameDistance, clipRegion.getBottom() - 2
                * frameDistance, clipRegion.getBottom() - frameDistance);

        // MxM Block 8
        fillBlock(clipRegion.getRight() - frameDistance, clipRegion.getRight(), clipRegion.getBottom() - 2
                * frameDistance, clipRegion.getBottom() - frameDistance);

        // MxM Block 9
        fillBlock(clipRegion.getLeft(), clipRegion.getLeft() + frameDistance, clipRegion.getBottom() - frameDistance,
                clipRegion.getBottom());

        // MxM Block 10
        fillBlock(clipRegion.getLeft() + frameDistance, clipRegion.getLeft() + 2 * frameDistance, clipRegion
                .getBottom()
                - frameDistance, clipRegion.getBottom());

        // MxM Block 11
        fillBlock(clipRegion.getRight() - 2 * frameDistance, clipRegion.getRight() - frameDistance, clipRegion
                .getBottom()
                - frameDistance, clipRegion.getBottom());

        // MxM Block 12
        fillBlock(clipRegion.getRight() - frameDistance, clipRegion.getRight(), clipRegion.getBottom() - frameDistance,
                clipRegion.getBottom());

        // Fixup Top
        fillBlock(clipRegion.getLeft() + 2 * frameDistance, clipRegion.getLeft() + 2 * frameDistance
                + doubleVertexDistance, clipRegion.getTop(), clipRegion.getTop() + frameDistance);

        // Fixup Left
        fillBlock(clipRegion.getLeft(), clipRegion.getLeft() + frameDistance, clipRegion.getTop() + 2 * frameDistance,
                clipRegion.getTop() + 2 * frameDistance + doubleVertexDistance);

        // Fixup Right
        fillBlock(clipRegion.getRight() - frameDistance, clipRegion.getRight(),
                clipRegion.getTop() + 2 * frameDistance, clipRegion.getTop() + 2 * frameDistance + doubleVertexDistance);

        // Fixup Bottom
        fillBlock(clipRegion.getLeft() + 2 * frameDistance, clipRegion.getLeft() + 2 * frameDistance
                + doubleVertexDistance, clipRegion.getBottom() - frameDistance, clipRegion.getBottom());

        if (nextFinerLevel != null) {
            if ((nextFinerLevel.clipRegion.getX() - clipRegion.getX()) / vertexDistance == frameSize) {
                if ((nextFinerLevel.clipRegion.getY() - clipRegion.getY()) / vertexDistance == frameSize) {
                    // Upper Left L Shape

                    // Up
                    fillBlock(clipRegion.getLeft() + frameDistance, clipRegion.getRight() - frameDistance, clipRegion
                            .getTop()
                            + frameDistance, clipRegion.getTop() + frameDistance + vertexDistance);
                    // Left
                    fillBlock(clipRegion.getLeft() + frameDistance, clipRegion.getLeft() + frameDistance
                            + vertexDistance, clipRegion.getTop() + frameDistance + vertexDistance, clipRegion
                            .getBottom()
                            - frameDistance);
                } else {
                    // Lower Left L Shape

                    // Left
                    fillBlock(clipRegion.getLeft() + frameDistance, clipRegion.getLeft() + frameDistance
                            + vertexDistance, clipRegion.getTop() + frameDistance, clipRegion.getBottom()
                            - frameDistance - vertexDistance);

                    // Bottom
                    fillBlock(clipRegion.getLeft() + frameDistance, clipRegion.getRight() - frameDistance, clipRegion
                            .getBottom()
                            - frameDistance - vertexDistance, clipRegion.getBottom() - frameDistance);
                }
            } else {
                if ((nextFinerLevel.clipRegion.getY() - clipRegion.getY()) / vertexDistance == frameSize) {
                    // Upper Right L Shape

                    // Up
                    fillBlock(clipRegion.getLeft() + frameDistance, clipRegion.getRight() - frameDistance, clipRegion
                            .getTop()
                            + frameDistance, clipRegion.getTop() + frameDistance + vertexDistance);
                    // Right
                    fillBlock(clipRegion.getRight() - frameDistance - vertexDistance, clipRegion.getRight()
                            - frameDistance, clipRegion.getTop() + frameDistance + vertexDistance, clipRegion
                            .getBottom()
                            - frameDistance);
                } else {
                    // Lower Right L Shape

                    // Right
                    fillBlock(clipRegion.getRight() - frameDistance - vertexDistance, clipRegion.getRight()
                            - frameDistance, clipRegion.getTop() + frameDistance, clipRegion.getBottom()
                            - frameDistance - vertexDistance);

                    // Bottom
                    fillBlock(clipRegion.getLeft() + frameDistance, clipRegion.getRight() - frameDistance, clipRegion
                            .getBottom()
                            - frameDistance - vertexDistance, clipRegion.getBottom() - frameDistance);
                }
            }
        }

        // Fill in the middle patch if most detailed layer
        if (nextFinerLevel == null) {
            fillBlock(clipRegion.getLeft() + frameDistance, clipRegion.getLeft() + frameDistance + clipSideSize / 2,
                    clipRegion.getTop() + frameDistance, clipRegion.getTop() + frameDistance + clipSideSize / 2);

            fillBlock(clipRegion.getLeft() + frameDistance + clipSideSize / 2, clipRegion.getRight() - frameDistance,
                    clipRegion.getTop() + frameDistance, clipRegion.getTop() + frameDistance + clipSideSize / 2);

            fillBlock(clipRegion.getLeft() + frameDistance, clipRegion.getLeft() + frameDistance + clipSideSize / 2,
                    clipRegion.getTop() + frameDistance + clipSideSize / 2, clipRegion.getBottom() - frameDistance);

            fillBlock(clipRegion.getLeft() + frameDistance + clipSideSize / 2, clipRegion.getRight() - frameDistance,
                    clipRegion.getTop() + frameDistance + clipSideSize / 2, clipRegion.getBottom() - frameDistance);
        }

        final IntBuffer indices = getMeshData().getIndexBuffer();
        indices.clear();
        indices.put(tmpIndices, 0, getStripIndex());
        indices.flip();
    }

    /**
     * Fills a specified area to indexarray. This will be added only after a bounding test pass.
     * 
     * @param left
     * @param right
     * @param top
     * @param bottom
     */
    private void fillBlock(int left, int right, int top, int bottom) {
        // Setup the boundingbox of the block to fill.
        // The lowest value is zero, the highest is the scalesize.
        frustumCheckBounds.setCenter((left + right) * 0.5, (heightRangeMax + heightRangeMin) * heightScale * 0.5,
                (top + bottom) * 0.5);
        frustumCheckBounds.setXExtent((left - right) * 0.5);
        frustumCheckBounds.setYExtent((heightRangeMax - heightRangeMin) * heightScale * 0.5);
        frustumCheckBounds.setZExtent((top - bottom) * 0.5);

        frustumCheckBounds.transform(getWorldTransform(), frustumCheckBounds);

        final int state = clipmapTestFrustum.getPlaneState();

        if (clipmapTestFrustum.contains(frustumCheckBounds) != FrustumIntersect.Outside) {
            // Same moduloprocedure as when we updated the vertices.
            // Maps the terrainposition to arrayposition.
            left = (left / vertexDistance) % clipSideSize;
            right = (right / vertexDistance) % clipSideSize;
            top = (top / vertexDistance) % clipSideSize;
            bottom = (bottom / vertexDistance) % clipSideSize;
            left += left < 0 ? clipSideSize : 0;
            right += right < 0 ? clipSideSize : 0;
            top += top < 0 ? clipSideSize : 0;
            bottom += bottom < 0 ? clipSideSize : 0;

            // Now fill the block.
            if (bottom < top) {
                // Bottom border is positioned somwhere over the top border,
                // we have a wrapover so we must split up the update in two parts.

                // Go from top border to the end of the array and update every row
                for (int z = top; z <= clipSideSize - 2; z++) {
                    fillRow(left, right, z, z + 1);
                }

                // Update the wrapover row
                fillRow(left, right, clipSideSize - 1, 0);

                // Go from arraystart to the bottom border and update every row.
                for (int z = 0; z <= bottom - 1; z++) {
                    fillRow(left, right, z, z + 1);
                }
            } else {
                // Top border is over the bottom border. Update from top to bottom.
                for (int z = top; z <= bottom - 1; z++) {
                    fillRow(left, right, z, z + 1);
                }
            }
        }

        clipmapTestFrustum.setPlaneState(state);
    }

    /**
     * Fills a strip of triangles that can be build between vertices row Zn and Zn1.
     * 
     * @param startX
     *            Start x-coordinate
     * @param endX
     *            End x-coordinate
     * @param rowZ
     *            Row n
     * @param rowZPlus1
     *            Row n + 1
     */
    private void fillRow(final int startX, final int endX, final int rowZ, final int rowZPlus1) {
        addIndex(startX, rowZ);
        if (startX <= endX) {
            for (int x = startX; x <= endX; x++) {
                addIndex(x, rowZ);
                addIndex(x, rowZPlus1);
            }
        } else {
            for (int x = startX; x <= clipSideSize - 1; x++) {
                addIndex(x, rowZ);
                addIndex(x, rowZPlus1);
            }
            for (int x = 0; x <= endX; x++) {
                addIndex(x, rowZ);
                addIndex(x, rowZPlus1);
            }
        }
        addIndex(endX, rowZPlus1);
    }

    /**
     * Adds a specific index to indexarray.
     * 
     * @param x
     * @param z
     */
    private void addIndex(final int x, final int z) {
        // calculate the index
        final int i = x + z * clipSideSize;
        // add the index and increment counter.
        final int currentStripIndex = getStripIndex();
        tmpIndices[currentStripIndex] = i;
        stripIndex++;
    }

    /**
     * Gets the number of triangles that are visible in current frame. This changes every frame.
     */
    public int getStripIndex() {
        return stripIndex;
    }

    /**
     * @return the vertexDistance
     */
    public int getVertexDistance() {
        return vertexDistance;
    }

    public boolean isReady() {
        return heightmapPyramid.isReady(levelIndex);
    }

    public void setHeightRange(final float heightRangeMin, final float heightRangeMax) {
        this.heightRangeMin = heightRangeMin;
        this.heightRangeMax = heightRangeMax;
    }
}
