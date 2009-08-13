/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.ui;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.extension.ui.UIButton;
import com.ardor3d.extension.ui.UICheckBox;
import com.ardor3d.extension.ui.UIFrame;
import com.ardor3d.extension.ui.UIHud;
import com.ardor3d.extension.ui.UILabel;
import com.ardor3d.extension.ui.UIPanel;
import com.ardor3d.extension.ui.UIProgressBar;
import com.ardor3d.extension.ui.UIRadioButton;
import com.ardor3d.extension.ui.UITabbedPane;
import com.ardor3d.extension.ui.UITabbedPane.TabPlacement;
import com.ardor3d.extension.ui.backdrop.ImageBackdrop;
import com.ardor3d.extension.ui.backdrop.ImageBackdrop.StretchAxis;
import com.ardor3d.extension.ui.layout.BorderLayout;
import com.ardor3d.extension.ui.layout.BorderLayoutData;
import com.ardor3d.extension.ui.layout.RowLayout;
import com.ardor3d.extension.ui.util.Alignment;
import com.ardor3d.extension.ui.util.ButtonGroup;
import com.ardor3d.extension.ui.util.Dimension;
import com.ardor3d.extension.ui.util.SubTex;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Image.Format;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;
import com.google.inject.Inject;

public class SimpleUIExample extends ExampleBase {
    UIHud hud;
    UILabel fpslabel;
    UIProgressBar bar;

    public static void main(final String[] args) {
        start(SimpleUIExample.class);
    }

    @Inject
    public SimpleUIExample(final LogicalLayer layer, final FrameHandler frameWork) {
        super(layer, frameWork);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Simple UI Example");

        // Add a spinning 3D box to show behind UI.
        final Box box = new Box("Box", new Vector3(0, 0, 0), 5, 5, 5);
        box.setModelBound(new BoundingBox());
        box.setTranslation(new Vector3(0, 0, -15));
        box.addController(new SpatialController<Box>() {
            private final Matrix3 rotate = new Matrix3();
            private double angle = 0;
            private final Vector3 axis = new Vector3(1, 1, 0.5f).normalizeLocal();

            public void update(final double time, final Box caller) {
                angle += time * 50;
                angle %= 360;
                rotate.fromAngleNormalAxis(angle * MathUtils.DEG_TO_RAD, axis);
                caller.setRotation(rotate);
            }
        });
        // Add a texture to the box.
        final TextureState ts = new TextureState();

        final Texture tex = TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear,
                Format.GuessNoCompression, false);
        ts.setTexture(tex);
        box.setRenderState(ts);
        _root.attachChild(box);

        final UIPanel panel = new UIPanel();
        panel.setForegroundColor(ColorRGBA.DARK_GRAY);
        panel.setLayout(new BorderLayout());

        final UIButton button = new UIButton("Button A");
        button.setIcon(new SubTex(tex));
        button.setIconDimensions(new Dimension(26, 26));
        button.setGap(10);
        button.setLayoutData(BorderLayoutData.NORTH);
        button.setTooltipText("This is a tooltip!");
        panel.add(button);

        final RowLayout rowLay = new RowLayout(false);
        rowLay.setExpands(false);
        final UIPanel centerPanel = new UIPanel(rowLay);
        centerPanel.setLayoutData(BorderLayoutData.CENTER);
        panel.add(centerPanel);

        final UICheckBox check1 = new UICheckBox("Hello");
        check1.setSelected(true);
        check1.setEnabled(false);
        centerPanel.add(check1);
        final UICheckBox check2 = new UICheckBox("World");
        centerPanel.add(check2);

        final ButtonGroup group = new ButtonGroup();
        final UIRadioButton radio1 = new UIRadioButton("option A");
        radio1.setGroup(group);
        centerPanel.add(radio1);
        final UIRadioButton radio2 = new UIRadioButton("option B");
        radio2.setGroup(group);
        centerPanel.add(radio2);

        bar = new UIProgressBar("Loading: ", true);
        bar.setPercentFilled(0);
        bar.setComponentWidth(250);
        bar.setLayoutResizeableX(false);
        centerPanel.add(bar);

        fpslabel = new UILabel("FPS");
        fpslabel.setLayoutData(BorderLayoutData.SOUTH);
        panel.add(fpslabel);

        final UIPanel panel2 = new UIPanel();
        final ImageBackdrop imgBD = new ImageBackdrop(new SubTex(tex), new ColorRGBA(1, 1, 1, 1));
        imgBD.setAlignment(Alignment.BOTTOM_LEFT);
        imgBD.setStretch(StretchAxis.None);
        panel2.setBackdrop(imgBD);
        panel2.add(new UILabel("You are on panel two."));

        final UITabbedPane pane = new UITabbedPane(TabPlacement.NORTH);
        pane.add(panel, "panel 1");
        pane.add(panel2, "panel 2");

        final UIFrame frame = new UIFrame("Sample");
        frame.setContentPanel(pane);
        frame.updateMinimumSizeFromContents();
        frame.layout();
        frame.pack();

        frame.setUseStandin(true);
        UIFrame.setUseTransparency(false);
        frame.setLocationRelativeTo(_canvas.getCanvasRenderer().getCamera());
        frame.setName("sample");

        hud = new UIHud();
        hud.add(frame);
        hud.setupInput(_canvas, _physicalLayer, _logicalLayer);
    }

    @Override
    protected void updateLogicalLayer(final ReadOnlyTimer timer) {
        hud.getLogicalLayer().checkTriggers(timer.getTimePerFrame());
    }

    @Override
    protected void renderExample(final Renderer renderer) {
        super.renderExample(renderer);
        renderer.renderBuckets();
        renderer.draw(hud);
    }

    private double counter = 0;
    private int frames = 0;

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        counter += timer.getTimePerFrame();
        frames++;
        if (counter > 1) {
            final double fps = (frames / counter);
            counter = 0;
            frames = 0;
            System.out.printf("%7.1f FPS\n", fps);
            fpslabel.setText(fps + " FPS");
            bar.setPercentFilled(timer.getTimeInSeconds() / 15);
            bar.updateMinimumSizeFromContents();
        }
        hud.updateGeometricState(timer.getTimePerFrame());
    }
}
