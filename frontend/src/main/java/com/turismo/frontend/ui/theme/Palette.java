package com.turismo.frontend.ui.theme;

import java.awt.Color;

/**
 * Tokens de cor da aplicação. O tema escuro tem valores próprios (não é uma
 * inversão automática do claro).
 */
public final class Palette {

    public final boolean dark;
    public final Color page;
    public final Color card;
    public final Color border;
    public final Color line;
    public final Color text;
    public final Color textSecondary;
    public final Color textMuted;
    public final Color primary;
    public final Color primaryHover;
    public final Color onPrimary;
    public final Color danger;
    public final Color dangerSurface;
    public final Color warning;
    public final Color warningSurface;
    public final Color success;
    public final Color successSurface;
    public final Color info;
    public final Color infoSurface;
    public final Color neutralSurface;
    public final Color sidebar;
    public final Color sidebarHover;
    public final Color sidebarActive;
    public final Color sidebarText;
    public final Color sidebarTextActive;
    public final Color sidebarMuted;
    public final Color accent;
    public final Color chartSeries;
    public final Color chartGrid;

    private Palette(boolean dark, String... hex) {
        this.dark = dark;
        int i = 0;
        page = c(hex[i++]);
        card = c(hex[i++]);
        border = c(hex[i++]);
        line = c(hex[i++]);
        text = c(hex[i++]);
        textSecondary = c(hex[i++]);
        textMuted = c(hex[i++]);
        primary = c(hex[i++]);
        primaryHover = c(hex[i++]);
        onPrimary = c(hex[i++]);
        danger = c(hex[i++]);
        dangerSurface = c(hex[i++]);
        warning = c(hex[i++]);
        warningSurface = c(hex[i++]);
        success = c(hex[i++]);
        successSurface = c(hex[i++]);
        info = c(hex[i++]);
        infoSurface = c(hex[i++]);
        neutralSurface = c(hex[i++]);
        sidebar = c(hex[i++]);
        sidebarHover = c(hex[i++]);
        sidebarActive = c(hex[i++]);
        sidebarText = c(hex[i++]);
        sidebarTextActive = c(hex[i++]);
        sidebarMuted = c(hex[i++]);
        accent = c(hex[i++]);
        chartSeries = c(hex[i++]);
        chartGrid = c(hex[i]);
    }

    private static Color c(String hex) {
        return Color.decode(hex);
    }

    public static final Palette LIGHT = new Palette(false,
            "#F5F7F6", "#FFFFFF", "#DDE3E0", "#EBEFED",
            "#17211E", "#4F5B57", "#6B7772",
            "#1F6B54", "#185A46", "#FFFFFF",
            "#B3261E", "#FBEAE8", "#8A5A00", "#FFF3D6",
            "#1E6B3A", "#E5F3EA", "#1D5C8C", "#E5EFF8", "#EEF1F0",
            "#12352D", "#1A4339", "#21503F", "#CFE0DA", "#FFFFFF", "#8FB0A5",
            "#D8A330",
            "#2A78D6", "#E6EAE8");

    public static final Palette DARK = new Palette(true,
            "#111614", "#1A211E", "#2C3632", "#242D2A",
            "#E6ECE9", "#AEBBB6", "#8D9B96",
            "#3E9C7E", "#4BAE8E", "#07120E",
            "#F2867E", "#3B201D", "#E8B85A", "#3A2E14",
            "#6CC28A", "#17301F", "#7FB6E6", "#172A3A", "#232B28",
            "#0C211C", "#143229", "#1B3E33", "#B9CCC5", "#FFFFFF", "#7E9A90",
            "#E0B04A",
            "#3987E5", "#2C3632");
}
