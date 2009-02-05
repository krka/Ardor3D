/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.awt;

import java.awt.event.KeyEvent;

import com.ardor3d.input.Key;

/**
 * Enum used for translations between AWT key codes and Ardor3D {@link Key} instances.
 */
public enum AwtKey {

    AWT_KEY_ESCAPE(KeyEvent.VK_ESCAPE, Key.ESCAPE), AWT_KEY_1(KeyEvent.VK_1, Key.ONE), AWT_KEY_2(KeyEvent.VK_2, Key.TWO), AWT_KEY_3(
            KeyEvent.VK_3, Key.THREE), AWT_KEY_4(KeyEvent.VK_4, Key.FOUR), AWT_KEY_5(KeyEvent.VK_5, Key.FIVE), AWT_KEY_6(
            KeyEvent.VK_6, Key.SIX), AWT_KEY_7(KeyEvent.VK_7, Key.SEVEN), AWT_KEY_8(KeyEvent.VK_8, Key.EIGHT), AWT_KEY_9(
            KeyEvent.VK_9, Key.NINE), AWT_KEY_0(KeyEvent.VK_0, Key.ZERO), AWT_KEY_MINUS(KeyEvent.VK_MINUS, Key.MINUS), AWT_KEY_EQUALS(
            KeyEvent.VK_EQUALS, Key.EQUALS), AWT_KEY_BACK(KeyEvent.VK_BACK_SPACE, Key.BACK), AWT_KEY_TAB(
            KeyEvent.VK_TAB, Key.TAB), AWT_KEY_Q(KeyEvent.VK_Q, Key.Q), AWT_KEY_W(KeyEvent.VK_W, Key.W), AWT_KEY_E(
            KeyEvent.VK_E, Key.E), AWT_KEY_R(KeyEvent.VK_R, Key.R), AWT_KEY_T(KeyEvent.VK_T, Key.T), AWT_KEY_Y(
            KeyEvent.VK_Y, Key.Y), AWT_KEY_U(KeyEvent.VK_U, Key.U), AWT_KEY_I(KeyEvent.VK_I, Key.I), AWT_KEY_O(
            KeyEvent.VK_O, Key.O), AWT_KEY_P(KeyEvent.VK_P, Key.P), AWT_KEY_LBRACKET(KeyEvent.VK_OPEN_BRACKET,
            Key.LBRACKET), AWT_KEY_RBRACKET(KeyEvent.VK_CLOSE_BRACKET, Key.RBRACKET), AWT_KEY_RETURN(KeyEvent.VK_ENTER,
            Key.RETURN), AWT_KEY_LCONTROL(KeyEvent.VK_CONTROL, Key.LCONTROL), AWT_KEY_A(KeyEvent.VK_A, Key.A), AWT_KEY_S(
            KeyEvent.VK_S, Key.S), AWT_KEY_D(KeyEvent.VK_D, Key.D), AWT_KEY_F(KeyEvent.VK_F, Key.F), AWT_KEY_G(
            KeyEvent.VK_G, Key.G), AWT_KEY_H(KeyEvent.VK_H, Key.H), AWT_KEY_J(KeyEvent.VK_J, Key.J), AWT_KEY_K(
            KeyEvent.VK_K, Key.K), AWT_KEY_L(KeyEvent.VK_L, Key.L), AWT_KEY_SEMICOLON(KeyEvent.VK_SEMICOLON,
            Key.SEMICOLON), AWT_KEY_APOSTROPHE(KeyEvent.VK_QUOTE, Key.APOSTROPHE), AWT_KEY_GRAVE(
            KeyEvent.VK_BACK_QUOTE, Key.GRAVE), AWT_KEY_LSHIFT(KeyEvent.VK_SHIFT, Key.LSHIFT), AWT_KEY_BACKSLASH(
            KeyEvent.VK_BACK_SLASH, Key.BACKSLASH), AWT_KEY_Z(KeyEvent.VK_Z, Key.Z), AWT_KEY_X(KeyEvent.VK_X, Key.X), AWT_KEY_C(
            KeyEvent.VK_C, Key.C), AWT_KEY_V(KeyEvent.VK_V, Key.V), AWT_KEY_B(KeyEvent.VK_B, Key.B), AWT_KEY_N(
            KeyEvent.VK_N, Key.N), AWT_KEY_M(KeyEvent.VK_M, Key.M), AWT_KEY_COMMA(KeyEvent.VK_COMMA, Key.COMMA), AWT_KEY_PERIOD(
            KeyEvent.VK_PERIOD, Key.PERIOD), AWT_KEY_SLASH(KeyEvent.VK_SLASH, Key.SLASH), AWT_KEY_RSHIFT(
            KeyEvent.VK_SHIFT, Key.RSHIFT), AWT_KEY_MULTIPLY(KeyEvent.VK_MULTIPLY, Key.MULTIPLY), AWT_KEY_SPACE(
            KeyEvent.VK_SPACE, Key.SPACE), AWT_KEY_CAPITAL(KeyEvent.VK_CAPS_LOCK, Key.CAPITAL), AWT_KEY_F1(
            KeyEvent.VK_F1, Key.F1), AWT_KEY_F2(KeyEvent.VK_F2, Key.F2), AWT_KEY_F3(KeyEvent.VK_F3, Key.F3), AWT_KEY_F4(
            KeyEvent.VK_F4, Key.F4), AWT_KEY_F5(KeyEvent.VK_F5, Key.F5), AWT_KEY_F6(KeyEvent.VK_F6, Key.F6), AWT_KEY_F7(
            KeyEvent.VK_F7, Key.F7), AWT_KEY_F8(KeyEvent.VK_F8, Key.F8), AWT_KEY_F9(KeyEvent.VK_F9, Key.F9), AWT_KEY_F10(
            KeyEvent.VK_F10, Key.F10), AWT_KEY_NUMLOCK(KeyEvent.VK_NUM_LOCK, Key.NUMLOCK), AWT_KEY_SCROLL(
            KeyEvent.VK_SCROLL_LOCK, Key.SCROLL), AWT_KEY_NUMPAD7(KeyEvent.VK_NUMPAD7, Key.NUMPAD7), AWT_KEY_NUMPAD8(
            KeyEvent.VK_NUMPAD8, Key.NUMPAD8), AWT_KEY_NUMPAD9(KeyEvent.VK_NUMPAD9, Key.NUMPAD9), AWT_KEY_SUBTRACT(
            KeyEvent.VK_SUBTRACT, Key.SUBTRACT), AWT_KEY_NUMPAD4(KeyEvent.VK_NUMPAD4, Key.NUMPAD4), AWT_KEY_NUMPAD5(
            KeyEvent.VK_NUMPAD5, Key.NUMPAD5), AWT_KEY_NUMPAD6(KeyEvent.VK_NUMPAD6, Key.NUMPAD6), AWT_KEY_ADD(
            KeyEvent.VK_ADD, Key.ADD), AWT_KEY_NUMPAD1(KeyEvent.VK_NUMPAD1, Key.NUMPAD1), AWT_KEY_NUMPAD2(
            KeyEvent.VK_NUMPAD2, Key.NUMPAD2), AWT_KEY_NUMPAD3(KeyEvent.VK_NUMPAD3, Key.NUMPAD3), AWT_KEY_NUMPAD0(
            KeyEvent.VK_NUMPAD0, Key.NUMPAD0), AWT_KEY_DECIMAL(KeyEvent.VK_DECIMAL, Key.DECIMAL), AWT_KEY_F11(
            KeyEvent.VK_F11, Key.F11), AWT_KEY_F12(KeyEvent.VK_F12, Key.F12), AWT_KEY_F13(KeyEvent.VK_F13, Key.F13), AWT_KEY_F14(
            KeyEvent.VK_F14, Key.F14), AWT_KEY_F15(KeyEvent.VK_F15, Key.F15), AWT_KEY_KANA(KeyEvent.VK_KANA, Key.KANA), AWT_KEY_CONVERT(
            KeyEvent.VK_CONVERT, Key.CONVERT), AWT_KEY_NOCONVERT(KeyEvent.VK_NONCONVERT, Key.NOCONVERT), AWT_KEY_NUMPADEQUALS(
            KeyEvent.VK_EQUALS, Key.NUMPADEQUALS), AWT_KEY_CIRCUMFLEX(KeyEvent.VK_CIRCUMFLEX, Key.CIRCUMFLEX), AWT_KEY_AT(
            KeyEvent.VK_AT, Key.AT), AWT_KEY_COLON(KeyEvent.VK_COLON, Key.COLON), AWT_KEY_UNDERLINE(
            KeyEvent.VK_UNDERSCORE, Key.UNDERLINE), AWT_KEY_STOP(KeyEvent.VK_STOP, Key.STOP), AWT_KEY_NUMPADENTER(
            KeyEvent.VK_ENTER, Key.NUMPADENTER), AWT_KEY_RCONTROL(KeyEvent.VK_CONTROL, Key.RCONTROL), AWT_KEY_NUMPADCOMMA(
            KeyEvent.VK_COMMA, Key.NUMPADCOMMA), AWT_KEY_DIVIDE(KeyEvent.VK_DIVIDE, Key.DIVIDE), AWT_KEY_PAUSE(
            KeyEvent.VK_PAUSE, Key.PAUSE), AWT_KEY_HOME(KeyEvent.VK_HOME, Key.HOME), AWT_KEY_UP(KeyEvent.VK_UP, Key.UP), AWT_KEY_PRIOR(
            KeyEvent.VK_PAGE_UP, Key.PRIOR), AWT_KEY_LEFT(KeyEvent.VK_LEFT, Key.LEFT), AWT_KEY_RIGHT(KeyEvent.VK_RIGHT,
            Key.RIGHT), AWT_KEY_END(KeyEvent.VK_END, Key.END), AWT_KEY_DOWN(KeyEvent.VK_DOWN, Key.DOWN), AWT_KEY_NEXT(
            KeyEvent.VK_PAGE_DOWN, Key.NEXT), AWT_KEY_INSERT(KeyEvent.VK_INSERT, Key.INSERT), AWT_KEY_DELETE(
            KeyEvent.VK_DELETE, Key.DELETE), AWT_KEY_LMENU(KeyEvent.VK_ALT, Key.LMENU), AWT_KEY_RMENU(KeyEvent.VK_ALT,
            Key.RMENU);

    private final int _awtCode;
    private final Key _key;

    private AwtKey(final int awtCode, final Key key) {
        _awtCode = awtCode;
        _key = key;
    }

    public static Key findByCode(final int awtCode) {
        for (final AwtKey ak : values()) {
            if (ak._awtCode == awtCode) {
                return ak._key;
            }
        }

        throw new IllegalStateException("No AWT key found corresponding to code: " + awtCode);
    }

    public int getAwtCode() {
        return _awtCode;
    }
}
