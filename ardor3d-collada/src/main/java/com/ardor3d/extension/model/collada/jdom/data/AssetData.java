/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.jdom.data;

import com.ardor3d.math.type.ReadOnlyVector3;

/**
 * Holds data related to asset info.
 */
public class AssetData {
    private String author;
    private String authoringTool;
    private String comments;
    private String copyright;
    private String sourceData;
    private String created;
    private String keywords;
    private String modified;
    private String revision;
    private String subject;
    private String title;
    private String unitName;
    private float unitMeter;
    private ReadOnlyVector3 upAxis;

    public String getAuthor() {
        return author;
    }

    public void setAuthor(final String author) {
        this.author = author;
    }

    public String getAuthoringTool() {
        return authoringTool;
    }

    public void setAuthoringTool(final String authoringTool) {
        this.authoringTool = authoringTool;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(final String comments) {
        this.comments = comments;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setCopyright(final String copyright) {
        this.copyright = copyright;
    }

    public String getSourceData() {
        return sourceData;
    }

    public void setSourceData(final String sourceData) {
        this.sourceData = sourceData;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(final String created) {
        this.created = created;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(final String keywords) {
        this.keywords = keywords;
    }

    public String getModified() {
        return modified;
    }

    public void setModified(final String modified) {
        this.modified = modified;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(final String revision) {
        this.revision = revision;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(final String subject) {
        this.subject = subject;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(final String unitName) {
        this.unitName = unitName;
    }

    public float getUnitMeter() {
        return unitMeter;
    }

    public void setUnitMeter(final float unitMeter) {
        this.unitMeter = unitMeter;
    }

    /**
     * @return the up axis as defined in the &lt;asset> tag, or null if not existing.
     */
    public ReadOnlyVector3 getUpAxis() {
        return upAxis;
    }

    public void setUpAxis(final ReadOnlyVector3 upAxis) {
        this.upAxis = upAxis;
    }

}
