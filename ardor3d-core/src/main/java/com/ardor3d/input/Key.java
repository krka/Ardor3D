/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input;

/**
 * All the keys that the keyboard input system can handle. Currently, the 'code' field refers to LWJGL codes only.
 * Adding support for JOGL may mean that some changes are going to be required.
 */
public enum Key {
    /**
     * LWJGL special key 'none', which is returned when an unrecognized key is pressed.
     */
    NONE(0x00),
    /**
     * escape key.
     */
    ESCAPE(0x01),
    /**
     * 1 key.
     */
    ONE(0x02),
    /**
     * 2 key.
     */
    TWO(0x03),
    /**
     * 3 key.
     */
    THREE(0x04),
    /**
     * 4 key.
     */
    FOUR(0x05),
    /**
     * 5 key.
     */
    FIVE(0x06),
    /**
     * 6 key.
     */
    SIX(0x07),
    /**
     * 7 key.
     */
    SEVEN(0x08),
    /**
     * 8 key.
     */
    EIGHT(0x09),
    /**
     * 9 key.
     */
    NINE(0x0A),
    /**
     * 0 key.
     */
    ZERO(0x0B),
    /**
     * - key.
     */
    MINUS(0x0C),
    /**
     * = key.
     */
    EQUALS(0x0D),
    /**
     * back key.
     */
    BACK(0x0E),
    /**
     * tab key.
     */
    TAB(0x0F),
    /**
     * q key.
     */
    Q(0x10),
    /**
     * w key.
     */
    W(0x11),
    /**
     * e key.
     */
    E(0x12),
    /**
     * r key.
     */
    R(0x13),
    /**
     * t key.
     */
    T(0x14),
    /**
     * y key.
     */
    Y(0x15),
    /**
     * u key.
     */
    U(0x16),
    /**
     * i key.
     */
    I(0x17),
    /**
     * o key.
     */
    O(0x18),
    /**
     * p key.
     */
    P(0x19),
    /**
     * [ key.
     */
    LBRACKET(0x1A),
    /**
     * ] key.
     */
    RBRACKET(0x1B),
    /**
     * enter (main keyboard) key.
     */
    RETURN(0x1C),
    /**
     * left control key.
     */
    LCONTROL(0x1D),
    /**
     * a key.
     */
    A(0x1E),
    /**
     * s key.
     */
    S(0x1F),
    /**
     * d key.
     */
    D(0x20),
    /**
     * f key.
     */
    F(0x21),
    /**
     * g key.
     */
    G(0x22),
    /**
     * h key.
     */
    H(0x23),
    /**
     * j key.
     */
    J(0x24),
    /**
     * k key.
     */
    K(0x25),
    /**
     * l key.
     */
    L(0x26),
    /**
     * ; key.
     */
    SEMICOLON(0x27),
    /**
     * ' key.
     */
    APOSTROPHE(0x28),
    /**
     * ` key.
     */
    GRAVE(0x29),
    /**
     * left shift key.
     */
    LSHIFT(0x2A),
    /**
     * \ key.
     */
    BACKSLASH(0x2B),
    /**
     * z key.
     */
    Z(0x2C),
    /**
     * x key.
     */
    X(0x2D),
    /**
     * c key.
     */
    C(0x2E),
    /**
     * v key.
     */
    V(0x2F),
    /**
     * b key.
     */
    B(0x30),
    /**
     * n key.
     */
    N(0x31),
    /**
     * m key.
     */
    M(0x32),
    /**
     * , key.
     */
    COMMA(0x33),
    /**
     * . key (main keyboard).
     */
    PERIOD(0x34),
    /**
     * / key (main keyboard).
     */
    SLASH(0x35),
    /**
     * right shift key.
     */
    RSHIFT(0x36),
    /**
     * * key (on keypad).
     */
    MULTIPLY(0x37),
    /**
     * left alt key.
     */
    LMENU(0x38),
    /**
     * space key.
     */
    SPACE(0x39),
    /**
     * caps lock key.
     */
    CAPITAL(0x3A),
    /**
     * F1 key.
     */
    F1(0x3B),
    /**
     * F2 key.
     */
    F2(0x3C),
    /**
     * F3 key.
     */
    F3(0x3D),
    /**
     * F4 key.
     */
    F4(0x3E),
    /**
     * F5 key.
     */
    F5(0x3F),
    /**
     * F6 key.
     */
    F6(0x40),
    /**
     * F7 key.
     */
    F7(0x41),
    /**
     * F8 key.
     */
    F8(0x42),
    /**
     * F9 key.
     */
    F9(0x43),
    /**
     * F10 key.
     */
    F10(0x44),
    /**
     * NumLK key.
     */
    NUMLOCK(0x45),
    /**
     * Scroll lock key.
     */
    SCROLL(0x46),
    /**
     * 7 key (num pad).
     */
    NUMPAD7(0x47),
    /**
     * 8 key (num pad).
     */
    NUMPAD8(0x48),
    /**
     * 9 key (num pad).
     */
    NUMPAD9(0x49),
    /**
     * - key (num pad).
     */
    NUMPADSUBTRACT(0x4A),
    /**
     * 4 key (num pad).
     */
    NUMPAD4(0x4B),
    /**
     * 5 key (num pad).
     */
    NUMPAD5(0x4C),
    /**
     * 6 key (num pad).
     */
    NUMPAD6(0x4D),
    /**
     * + key (num pad).
     */
    NUMPADADD(0x4E),
    /**
     * 1 key (num pad).
     */
    NUMPAD1(0x4F),
    /**
     * 2 key (num pad).
     */
    NUMPAD2(0x50),
    /**
     * 3 key (num pad).
     */
    NUMPAD3(0x51),
    /**
     * 0 key (num pad).
     */
    NUMPAD0(0x52),
    /**
     * . key (num pad).
     */
    DECIMAL(0x53),
    /**
     * F11 key.
     */
    F11(0x57),
    /**
     * F12 key.
     */
    F12(0x58),
    /**
     * F13 key.
     */
    F13(0x64),
    /**
     * F14 key.
     */
    F14(0x65),
    /**
     * F15 key.
     */
    F15(0x66),
    /**
     * kana key (Japanese).
     */
    KANA(0x70),
    /**
     * convert key (Japanese).
     */
    CONVERT(0x79),
    /**
     * noconvert key (Japanese).
     */
    NOCONVERT(0x7B),
    /**
     * yen key (Japanese).
     */
    YEN(0x7D),
    /**
     * = on num pad (NEC PC98).
     */
    NUMPADEQUALS(0x8D),
    /**
     * circum flex key (Japanese).
     */
    CIRCUMFLEX(0x90),
    /**
     * &#064; key (NEC PC98).
     */
    AT(0x91),
    /**
     * : key (NEC PC98)
     */
    COLON(0x92),
    /**
     * _ key (NEC PC98).
     */
    UNDERLINE(0x93),
    /**
     * kanji key (Japanese).
     */
    KANJI(0x94),
    /**
     * stop key (NEC PC98).
     */
    STOP(0x95),
    /**
     * ax key (Japanese).
     */
    AX(0x96),
    /**
     * (J3100).
     */
    UNLABELED(0x97),
    /**
     * Enter key (num pad).
     */
    NUMPADENTER(0x9C),
    /**
     * right control key.
     */
    RCONTROL(0x9D),
    /**
     * , key on num pad (NEC PC98).
     */
    NUMPADCOMMA(0xB3),
    /**
     * / key (num pad).
     */
    DIVIDE(0xB5),
    /**
     * SysRq key.
     */
    SYSRQ(0xB7),
    /**
     * right alt key.
     */
    RMENU(0xB8),
    /**
     * pause key.
     */
    PAUSE(0xC5),
    /**
     * home key.
     */
    HOME(0xC7),
    /**
     * up arrow key.
     */
    UP(0xC8),
    /**
     * PageUp/Prior key.
     */
    PAGEUP_PRIOR(0xC9),
    /**
     * left arrow key.
     */
    LEFT(0xCB),
    /**
     * right arrow key.
     */
    RIGHT(0xCD),
    /**
     * end key.
     */
    END(0xCF),
    /**
     * down arrow key.
     */
    DOWN(0xD0),
    /**
     * PageDown/Next key.
     */
    PAGEDOWN_NEXT(0xD1),
    /**
     * insert key.
     */
    INSERT(0xD2),
    /**
     * delete key.
     */
    DELETE(0xD3),
    /**
     * Left Windows/Option key
     */
    LMETA(0xDB),
    /**
     * Right Windows/Option key
     */
    RMETA(0xDC), APPS(0xDD),
    /**
     * power key.
     */
    POWER(0xDE),
    /**
     * sleep key.
     */
    SLEEP(0xDF);

    private final int code;

    private Key(final int code) {
        this.code = code;
    }

    public static Key findByCode(final int keyCode) {
        for (final Key k : values()) {
            if (k.code == keyCode) {
                return k;
            }
        }

        throw new KeyNotFoundException(keyCode);
    }

    public int getCode() {
        return code;
    }
}
