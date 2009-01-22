/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph.shape;

import java.io.IOException;

import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.TexCoords;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.geom.BufferUtils;

/**
 * This primitive represents a box that has options to orient it according to its X/Y/Z axis. It is used to create an
 * OrientedBoundingBox mostly.
 */
public class OrientedBox extends Mesh {
    private static final long serialVersionUID = 1L;

    /** Center of the Oriented Box. */
    protected Vector3 center;

    /** X axis of the Oriented Box. */
    protected Vector3 xAxis = new Vector3(1, 0, 0);

    /** Y axis of the Oriented Box. */
    protected Vector3 yAxis = new Vector3(0, 1, 0);

    /** Z axis of the Oriented Box. */
    protected Vector3 zAxis = new Vector3(0, 0, 1);

    /** Extents of the box along the x,y,z axis. */
    protected Vector3 extent = new Vector3(0, 0, 0);

    /** Texture coordintae values for the corners of the box. */
    protected Vector2 texTopRight, texTopLeft, texBotRight, texBotLeft;

    /** Vector array used to store the array of 8 corners the box has. */
    public Vector3[] vectorStore;

    /**
     * If true, the box's vectorStore array correctly represnts the box's corners.
     */
    public boolean correctCorners;

    public OrientedBox() {}

    /**
     * Creates a new OrientedBox with the given name.
     * 
     * @param name
     *            The name of the new box.
     */
    public OrientedBox(final String name) {
        super(name);
        vectorStore = new Vector3[8];
        for (int i = 0; i < vectorStore.length; i++) {
            vectorStore[i] = new Vector3();
        }
        texTopRight = new Vector2(1, 1);
        texTopLeft = new Vector2(1, 0);
        texBotRight = new Vector2(0, 1);
        texBotLeft = new Vector2(0, 0);
        center = new Vector3(0, 0, 0);
        correctCorners = false;
        computeInformation();
    }

    /**
     * Takes the plane and center information and creates the correct vertex,normal,color,texture,index information to
     * represent the OrientedBox.
     */
    public void computeInformation() {
        setVertexData();
        setNormalData();
        setTextureData();
        setIndexData();
    }

    /**
     * Sets the correct indices array for the box.
     */
    private void setIndexData() {
        _meshData.setIndexBuffer(BufferUtils.createIntBuffer(_meshData.getIndexBuffer(), 36));

        for (int i = 0; i < 6; i++) {
            _meshData.getIndexBuffer().put(i * 4 + 0);
            _meshData.getIndexBuffer().put(i * 4 + 1);
            _meshData.getIndexBuffer().put(i * 4 + 3);
            _meshData.getIndexBuffer().put(i * 4 + 1);
            _meshData.getIndexBuffer().put(i * 4 + 2);
            _meshData.getIndexBuffer().put(i * 4 + 3);
        }
    }

    /**
     * Sets the correct texture array for the box.
     */
    private void setTextureData() {
        if (_meshData.getTextureCoords(0) == null) {
            _meshData.setTextureCoords(new TexCoords(BufferUtils.createVector2Buffer(24)), 0);

            for (int x = 0; x < 6; x++) {
                _meshData.getTextureCoords(0).coords.put(texTopRight.getXf()).put(texTopRight.getYf());
                _meshData.getTextureCoords(0).coords.put(texTopLeft.getXf()).put(texTopLeft.getYf());
                _meshData.getTextureCoords(0).coords.put(texBotLeft.getXf()).put(texBotLeft.getYf());
                _meshData.getTextureCoords(0).coords.put(texBotRight.getXf()).put(texBotRight.getYf());
            }
        }
    }

    /**
     * Sets the correct normal array for the box.
     */
    private void setNormalData() {
        _meshData.setNormalBuffer(BufferUtils.createVector3Buffer(_meshData.getNormalBuffer(), 24));

        // top
        _meshData.getNormalBuffer().put(yAxis.getXf()).put(yAxis.getYf()).put(yAxis.getZf());
        _meshData.getNormalBuffer().put(yAxis.getXf()).put(yAxis.getYf()).put(yAxis.getZf());
        _meshData.getNormalBuffer().put(yAxis.getXf()).put(yAxis.getYf()).put(yAxis.getZf());
        _meshData.getNormalBuffer().put(yAxis.getXf()).put(yAxis.getYf()).put(yAxis.getZf());

        // right
        _meshData.getNormalBuffer().put(xAxis.getXf()).put(xAxis.getYf()).put(xAxis.getZf());
        _meshData.getNormalBuffer().put(xAxis.getXf()).put(xAxis.getYf()).put(xAxis.getZf());
        _meshData.getNormalBuffer().put(xAxis.getXf()).put(xAxis.getYf()).put(xAxis.getZf());
        _meshData.getNormalBuffer().put(xAxis.getXf()).put(xAxis.getYf()).put(xAxis.getZf());

        // left
        _meshData.getNormalBuffer().put(-xAxis.getXf()).put(-xAxis.getYf()).put(-xAxis.getZf());
        _meshData.getNormalBuffer().put(-xAxis.getXf()).put(-xAxis.getYf()).put(-xAxis.getZf());
        _meshData.getNormalBuffer().put(-xAxis.getXf()).put(-xAxis.getYf()).put(-xAxis.getZf());
        _meshData.getNormalBuffer().put(-xAxis.getXf()).put(-xAxis.getYf()).put(-xAxis.getZf());

        // bottom
        _meshData.getNormalBuffer().put(-yAxis.getXf()).put(-yAxis.getYf()).put(-yAxis.getZf());
        _meshData.getNormalBuffer().put(-yAxis.getXf()).put(-yAxis.getYf()).put(-yAxis.getZf());
        _meshData.getNormalBuffer().put(-yAxis.getXf()).put(-yAxis.getYf()).put(-yAxis.getZf());
        _meshData.getNormalBuffer().put(-yAxis.getXf()).put(-yAxis.getYf()).put(-yAxis.getZf());

        // back
        _meshData.getNormalBuffer().put(-zAxis.getXf()).put(-zAxis.getYf()).put(-zAxis.getZf());
        _meshData.getNormalBuffer().put(-zAxis.getXf()).put(-zAxis.getYf()).put(-zAxis.getZf());
        _meshData.getNormalBuffer().put(-zAxis.getXf()).put(-zAxis.getYf()).put(-zAxis.getZf());
        _meshData.getNormalBuffer().put(-zAxis.getXf()).put(-zAxis.getYf()).put(-zAxis.getZf());

        // front
        _meshData.getNormalBuffer().put(zAxis.getXf()).put(zAxis.getYf()).put(zAxis.getZf());
        _meshData.getNormalBuffer().put(zAxis.getXf()).put(zAxis.getYf()).put(zAxis.getZf());
        _meshData.getNormalBuffer().put(zAxis.getXf()).put(zAxis.getYf()).put(zAxis.getZf());
        _meshData.getNormalBuffer().put(zAxis.getXf()).put(zAxis.getYf()).put(zAxis.getZf());
    }

    /**
     * Sets the correct vertex information for the box.
     */
    private void setVertexData() {
        computeCorners();
        _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(_meshData.getVertexBuffer(), 24));

        // Top
        _meshData.getVertexBuffer().put(vectorStore[0].getXf()).put(vectorStore[0].getYf()).put(vectorStore[0].getZf());
        _meshData.getVertexBuffer().put(vectorStore[1].getXf()).put(vectorStore[1].getYf()).put(vectorStore[1].getZf());
        _meshData.getVertexBuffer().put(vectorStore[5].getXf()).put(vectorStore[5].getYf()).put(vectorStore[5].getZf());
        _meshData.getVertexBuffer().put(vectorStore[3].getXf()).put(vectorStore[3].getYf()).put(vectorStore[3].getZf());

        // Right
        _meshData.getVertexBuffer().put(vectorStore[0].getXf()).put(vectorStore[0].getYf()).put(vectorStore[0].getZf());
        _meshData.getVertexBuffer().put(vectorStore[3].getXf()).put(vectorStore[3].getYf()).put(vectorStore[3].getZf());
        _meshData.getVertexBuffer().put(vectorStore[6].getXf()).put(vectorStore[6].getYf()).put(vectorStore[6].getZf());
        _meshData.getVertexBuffer().put(vectorStore[2].getXf()).put(vectorStore[2].getYf()).put(vectorStore[2].getZf());

        // Left
        _meshData.getVertexBuffer().put(vectorStore[5].getXf()).put(vectorStore[5].getYf()).put(vectorStore[5].getZf());
        _meshData.getVertexBuffer().put(vectorStore[1].getXf()).put(vectorStore[1].getYf()).put(vectorStore[1].getZf());
        _meshData.getVertexBuffer().put(vectorStore[4].getXf()).put(vectorStore[4].getYf()).put(vectorStore[4].getZf());
        _meshData.getVertexBuffer().put(vectorStore[7].getXf()).put(vectorStore[7].getYf()).put(vectorStore[7].getZf());

        // Bottom
        _meshData.getVertexBuffer().put(vectorStore[6].getXf()).put(vectorStore[6].getYf()).put(vectorStore[6].getZf());
        _meshData.getVertexBuffer().put(vectorStore[7].getXf()).put(vectorStore[7].getYf()).put(vectorStore[7].getZf());
        _meshData.getVertexBuffer().put(vectorStore[4].getXf()).put(vectorStore[4].getYf()).put(vectorStore[4].getZf());
        _meshData.getVertexBuffer().put(vectorStore[2].getXf()).put(vectorStore[2].getYf()).put(vectorStore[2].getZf());

        // Back
        _meshData.getVertexBuffer().put(vectorStore[3].getXf()).put(vectorStore[3].getYf()).put(vectorStore[3].getZf());
        _meshData.getVertexBuffer().put(vectorStore[5].getXf()).put(vectorStore[5].getYf()).put(vectorStore[5].getZf());
        _meshData.getVertexBuffer().put(vectorStore[7].getXf()).put(vectorStore[7].getYf()).put(vectorStore[7].getZf());
        _meshData.getVertexBuffer().put(vectorStore[6].getXf()).put(vectorStore[6].getYf()).put(vectorStore[6].getZf());

        // Front
        _meshData.getVertexBuffer().put(vectorStore[1].getXf()).put(vectorStore[1].getYf()).put(vectorStore[1].getZf());
        _meshData.getVertexBuffer().put(vectorStore[4].getXf()).put(vectorStore[4].getYf()).put(vectorStore[4].getZf());
        _meshData.getVertexBuffer().put(vectorStore[2].getXf()).put(vectorStore[2].getYf()).put(vectorStore[2].getZf());
        _meshData.getVertexBuffer().put(vectorStore[0].getXf()).put(vectorStore[0].getYf()).put(vectorStore[0].getZf());
    }

    /**
     * Sets the vectorStore information to the 8 corners of the box.
     */
    public void computeCorners() {
        correctCorners = true;

        final Vector3 tempVa = Vector3.fetchTempInstance();
        final Vector3 tempVb = Vector3.fetchTempInstance();
        final Vector3 tempVc = Vector3.fetchTempInstance();
        tempVa.set(xAxis).multiplyLocal(extent.getX());
        tempVb.set(yAxis).multiplyLocal(extent.getY());
        tempVc.set(zAxis).multiplyLocal(extent.getZ());

        vectorStore[0].set(center).addLocal(tempVa).addLocal(tempVb).addLocal(tempVc);
        vectorStore[1].set(center).addLocal(tempVa).subtractLocal(tempVb).addLocal(tempVc);
        vectorStore[2].set(center).addLocal(tempVa).addLocal(tempVb).subtractLocal(tempVc);
        vectorStore[3].set(center).subtractLocal(tempVa).addLocal(tempVb).addLocal(tempVc);
        vectorStore[4].set(center).addLocal(tempVa).subtractLocal(tempVb).subtractLocal(tempVc);
        vectorStore[5].set(center).subtractLocal(tempVa).subtractLocal(tempVb).addLocal(tempVc);
        vectorStore[6].set(center).subtractLocal(tempVa).addLocal(tempVb).subtractLocal(tempVc);
        vectorStore[7].set(center).subtractLocal(tempVa).subtractLocal(tempVb).subtractLocal(tempVc);

        Vector3.releaseTempInstance(tempVa);
        Vector3.releaseTempInstance(tempVb);
        Vector3.releaseTempInstance(tempVc);
    }

    /**
     * Returns the center of the box.
     * 
     * @return The box's center.
     */
    public Vector3 getCenter() {
        return center;
    }

    /**
     * Sets the box's center to the given value. Shallow copy only.
     * 
     * @param center
     *            The box's new center.
     */
    public void setCenter(final Vector3 center) {
        this.center = center;
    }

    /**
     * Returns the box's extent vector along the x,y,z.
     * 
     * @return The box's extent vector.
     */
    public Vector3 getExtent() {
        return extent;
    }

    /**
     * Sets the box's extent vector to the given value. Shallow copy only.
     * 
     * @param extent
     *            The box's new extent.
     */
    public void setExtent(final Vector3 extent) {
        this.extent = extent;
    }

    /**
     * Returns the x axis of this box.
     * 
     * @return This OB's x axis.
     */
    public Vector3 getxAxis() {
        return xAxis;
    }

    /**
     * Sets the x axis of this OB. Shallow copy.
     * 
     * @param xAxis
     *            The new x axis.
     */
    public void setxAxis(final Vector3 xAxis) {
        this.xAxis = xAxis;
    }

    /**
     * Gets the Y axis of this OB.
     * 
     * @return This OB's Y axis.
     */
    public Vector3 getyAxis() {
        return yAxis;
    }

    /**
     * Sets the Y axis of this OB. Shallow copy.
     * 
     * @param yAxis
     *            The new Y axis.
     */
    public void setyAxis(final Vector3 yAxis) {
        this.yAxis = yAxis;
    }

    /**
     * Returns the Z axis of this OB.
     * 
     * @return The Z axis.
     */
    public Vector3 getzAxis() {
        return zAxis;
    }

    /**
     * Sets the Z axis of this OB. Shallow copy.
     * 
     * @param zAxis
     *            The new Z axis.
     */
    public void setzAxis(final Vector3 zAxis) {
        this.zAxis = zAxis;
    }

    /**
     * Returns if the corners are set corectly.
     * 
     * @return True if the vectorStore is correct.
     */
    public boolean isCorrectCorners() {
        return correctCorners;
    }

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);

        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(center, "center", new Vector3(Vector3.ZERO));
        capsule.write(xAxis, "xAxis", new Vector3(Vector3.UNIT_X));
        capsule.write(yAxis, "yAxis", new Vector3(Vector3.UNIT_Y));
        capsule.write(zAxis, "zAxis", new Vector3(Vector3.UNIT_Z));
        capsule.write(extent, "extent", new Vector3(Vector3.ZERO));
        capsule.write(texTopRight, "texTopRight", new Vector2(1, 1));
        capsule.write(texTopLeft, "texTopLeft", new Vector2(1, 0));
        capsule.write(texBotRight, "texBotRight", new Vector2(0, 1));
        capsule.write(texBotLeft, "texBotLeft", new Vector2(0, 0));
        capsule.write(vectorStore, "vectorStore", new Vector3[8]);
        capsule.write(correctCorners, "correctCorners", false);
    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);

        center = (Vector3) capsule.readSavable("center", new Vector3(Vector3.ZERO));
        xAxis = (Vector3) capsule.readSavable("xAxis", new Vector3(Vector3.UNIT_X));
        yAxis = (Vector3) capsule.readSavable("yAxis", new Vector3(Vector3.UNIT_Y));
        zAxis = (Vector3) capsule.readSavable("zAxis", new Vector3(Vector3.UNIT_Z));
        extent = (Vector3) capsule.readSavable("extent", new Vector3(Vector3.ZERO));
        texTopRight = (Vector2) capsule.readSavable("texTopRight", new Vector2(1, 1));
        texTopLeft = (Vector2) capsule.readSavable("texTopLeft", new Vector2(1, 0));
        texBotRight = (Vector2) capsule.readSavable("texBotRight", new Vector2(0, 1));
        texBotLeft = (Vector2) capsule.readSavable("texBotLeft", new Vector2(0, 0));

        final Savable[] savs = capsule.readSavableArray("vectorStore", new Vector3[8]);
        if (savs == null) {
            vectorStore = null;
        } else {
            vectorStore = new Vector3[savs.length];
            for (int x = 0; x < savs.length; x++) {
                vectorStore[x] = (Vector3) savs[x];
            }
        }

        correctCorners = capsule.readBoolean("correctCorners", false);
    }
}