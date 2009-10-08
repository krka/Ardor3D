/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.layout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.UIContainer;
import com.ardor3d.scenegraph.Spatial;

/**
 * A UI Layout that puts content in rows and columns where the row and column cells are set to the minimal size of its
 * content plus some inter-cell spacing. The components should be added from top to down and left to right. Set the
 * layout data of the last component in a row to wrap, e.g. by setLayoutData(GridLayoutData.Wrap); You can specify a
 * horizontal span bigger than one to specify that a component should use multiple cells in the current row.
 */
public class GridLayout extends UILayout {

    private LayoutGrid grid;
    private final int interCellSpacingHorizontal;
    private final int interCellSpacingVertical;
    private final int leftMargin;
    private final int rightMargin;
    private final int topMargin;
    private final int bottomMargin;
    private final Logger logger = Logger.getLogger(GridLayout.class.getCanonicalName());

    public GridLayout() {
        this(15, 5, 10, 10, 10, 0);
    }

    public GridLayout(final int interCellSpacingHorizontal, final int interCellSpacingVertical, final int leftMargin,
            final int topMargin, final int rightMargin, final int bottomMargin) {
        this.interCellSpacingHorizontal = interCellSpacingHorizontal;
        this.interCellSpacingVertical = interCellSpacingVertical;
        this.leftMargin = leftMargin;
        this.topMargin = topMargin;
        this.rightMargin = rightMargin;
        this.bottomMargin = bottomMargin;
    }

    @Override
    public void layoutContents(final UIContainer container) {
        rebuildGrid(container);
        grid.updateMinimalSize();
        final int height = grid.minHeight;
        for (final LayoutComponent lc : grid.components) {
            lc.component.setLocalXY(grid.columnOffsets[lc.firstColumn], height - grid.rowOffsets[lc.firstRow]
                    - lc.getComponentHeight());
            if (lc.grow) {
                logger.info("GROW: set width to " + grid.getCellsWidth(lc.firstColumn, lc.lastColumn));
                lc.component.setComponentWidth(grid.getCellsWidth(lc.firstColumn, lc.lastColumn));
            }
        }
    }

    @Override
    public void updateMinimumSizeFromContents(final UIContainer container) {
        rebuildGrid(container);
        grid.updateMinimalSize();
        container.setMinimumContentSize(grid.minWidth, grid.minHeight);
    }

    private void rebuildGrid(final UIContainer container) {
        final List<Spatial> content = container.getChildren();
        grid = new LayoutGrid();
        for (final Spatial spatial : content) {
            if (spatial instanceof UIComponent) {
                final UIComponent c = (UIComponent) spatial;
                grid.add(c);
            }
        }
    }

    class LayoutGrid {
        int currentRow = 0;
        int currentColumn = 0;
        int nextColumn = 0;
        int nextRow = 0;
        int maxColumn;
        int maxRow;
        int minWidth;
        int minHeight;
        int[] columnOffsets;
        int[] rowOffsets;
        LinkedList<LayoutComponent> components;
        ArrayList<Integer> columnWidths;

        LayoutGrid() {
            components = new LinkedList<LayoutComponent>();
            columnWidths = new ArrayList<Integer>();
        }

        void add(final UIComponent c) {
            final UILayoutData data = c.getLayoutData();
            final LayoutComponent lc = new LayoutComponent(c);
            lc.firstColumn = currentColumn;
            lc.firstRow = currentRow;
            lc.lastColumn = currentColumn;
            lc.lastRow = currentRow;
            if (data != null && data instanceof GridLayoutData) {
                final GridLayoutData gld = (GridLayoutData) data;
                if (gld.getSpan() > 1) {
                    if (!gld.isWrap()) {
                        nextColumn += gld.getSpan();
                    } else {
                        nextColumn = 0;
                        nextRow = currentRow + 1;
                    }
                    lc.lastColumn = lc.firstColumn + gld.getSpan() - 1;
                    maxColumn = Math.max(maxColumn, lc.lastColumn);
                } else {
                    if (gld.isWrap()) {
                        nextColumn = 0;
                        nextRow = currentRow + 1;
                    } else {
                        nextColumn = currentColumn + 1;
                    }
                }
                lc.grow = gld.isGrow();
            } else {
                nextColumn = currentColumn + 1;
            }
            components.add(lc);
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(lc.toString() + " max.col=" + maxColumn);
            }
            maxColumn = Math.max(maxColumn, currentColumn);
            maxRow = Math.max(maxRow, currentRow);
            currentColumn = nextColumn;
            currentRow = nextRow;
        }

        void updateMinimalSize() {
            columnOffsets = new int[maxColumn + 2];
            rowOffsets = new int[maxRow + 2];
            columnOffsets[0] = leftMargin;
            rowOffsets[0] = topMargin;
            for (final LayoutComponent lc : components) {
                columnOffsets[lc.lastColumn + 1] = Math.max(columnOffsets[lc.lastColumn + 1], lc.getComponentWidth()
                        + interCellSpacingHorizontal + columnOffsets[lc.firstColumn]);
                rowOffsets[lc.firstRow + 1] = Math.max(rowOffsets[lc.firstRow + 1], lc.getComponentHeight()
                        + interCellSpacingVertical + rowOffsets[lc.firstRow]);
            }

            if (logger.isLoggable(Level.FINE)) {
                logger.fine("column offsets: " + Arrays.toString(columnOffsets));
                logger.fine("row offsets: " + Arrays.toString(rowOffsets));
            }
            minWidth = columnOffsets[maxColumn + 1] - interCellSpacingHorizontal + rightMargin;
            minHeight = rowOffsets[maxRow + 1] - interCellSpacingVertical + bottomMargin;
        }

        int getCellsWidth(final int firstColumn, final int lastColumn) {
            int width = columnOffsets[lastColumn + 1] - columnOffsets[firstColumn] - interCellSpacingHorizontal;
            if (lastColumn >= maxColumn) {
                width -= rightMargin;
            }
            return width;
        }
    }

    class LayoutComponent {
        UIComponent component;
        int firstRow;
        int firstColumn;
        int lastRow;
        int lastColumn;
        boolean grow;

        LayoutComponent(final UIComponent c) {
            component = c;
        }

        public int getComponentWidth() {
            return Math.max(component.getComponentWidth(), component.getMinimumComponentWidth(true));
        }

        public int getComponentHeight() {
            return Math.max(component.getComponentHeight(), component.getMinimumComponentHeight(true));
        }

        @Override
        public String toString() {
            return component + " " + firstColumn + "-" + lastColumn + "/" + firstRow + "-" + lastRow;
        }
    }
}
