/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.text.font;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.text.CharacterDescriptor;
import com.ardor3d.extension.ui.text.StyleConstants;
import com.ardor3d.image.Texture2D;
import com.ardor3d.ui.text.BMFont;
import com.ardor3d.ui.text.BMFont.Char;
import com.google.common.collect.Maps;

/**
 * Provides BMFonts for use in UIFont.
 */
public class BMFontProvider implements FontProvider {

    private final Map<BMFont.Info, UIFont> _fontMap = Maps.newHashMap();
    Map<UIFont, Integer> _scoreMap = Maps.newHashMap();

    public BMFontProvider(final Collection<BMFont> fonts) {
        for (final BMFont font : fonts) {
            final Map<Character, CharacterDescriptor> descriptors = Maps.newHashMap();
            for (final int val : font.getMappedChars()) {
                final Char c = font.getChar(val);
                final CharacterDescriptor desc = new CharacterDescriptor(c.x, c.y, c.width, c.height, c.xadvance,
                        c.xoffset, c.yoffset, 1.0, null);
                descriptors.put((char) val, desc);
            }

            final UIFont uiFont = new UIFont((Texture2D) font.getPageTexture(), descriptors, font.getLineHeight(), font
                    .getSize());
            final Map<Integer, Map<Integer, Integer>> kernings = font.getKerningMap();
            for (final int valA : kernings.keySet()) {
                final Map<Integer, Integer> kerns = kernings.get(valA);
                for (final int valB : kerns.keySet()) {
                    uiFont.addKerning((char) valA, (char) valB, kerns.get(valB));
                }
            }

            _fontMap.put(font.getInfo(), uiFont);
        }
    }

    @Override
    public UIFont getClosestMatchingFont(final Map<String, Object> currentStyles, final AtomicReference<Double> scale) {
        final boolean isBold = currentStyles.containsKey(StyleConstants.KEY_BOLD) ? (Boolean) currentStyles
                .get(StyleConstants.KEY_BOLD) : false;
        final boolean isItalic = currentStyles.containsKey(StyleConstants.KEY_ITALICS) ? (Boolean) currentStyles
                .get(StyleConstants.KEY_ITALICS) : false;
        final int size = currentStyles.containsKey(StyleConstants.KEY_SIZE) ? (Integer) currentStyles
                .get(StyleConstants.KEY_SIZE) : UIComponent.getDefaultFontSize();
        final String family = currentStyles.containsKey(StyleConstants.KEY_FAMILY) ? currentStyles.get(
                StyleConstants.KEY_FAMILY).toString() : UIComponent.getDefaultFontFamily();

        UIFont closest = null;
        int score, bestScore = Integer.MIN_VALUE;

        for (final BMFont.Info info : _fontMap.keySet()) {
            score = 0;
            if (family.equalsIgnoreCase(info.face)) {
                score += 200;
            }
            if (info.bold == isBold) {
                score += 50;
            } else {
                score -= 50;
            }
            if (info.italic == isItalic) {
                score += 50;
            } else {
                score -= 50;
            }
            score -= Math.abs(size - info.size);

            if (score > bestScore) {
                closest = _fontMap.get(info);
                bestScore = score;
            }
        }

        if (closest == null) {
            scale.set(1d);
        } else {
            scale.set((double) size / closest.getFontSize());
        }

        return closest;
    }
}
