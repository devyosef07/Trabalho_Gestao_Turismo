package com.turismo.frontend.ui.theme;

import java.awt.Color;

/**
 * Tokens de cor da aplicação: identidade azul (azul principal #2563EB sobre
 * azul profundo #0F172A). O tema escuro tem valores próprios (não é uma
 * inversão automática do claro); todos os pares texto/fundo têm contraste
 * de pelo menos 4,5:1.
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
    /** Fundo de seleção e de destaque suave (linhas de tabela, item escolhido). */
    public final Color primarySoft;
    /** Anel de foco do teclado. */
    public final Color focusRing;
    /** Cor das sombras (usada com transparência). */
    public final Color shadow;
    /** Gradiente da identidade: do mais profundo ao mais vivo. */
    public final Color brandDeep;
    public final Color brandMid;
    public final Color brandBright;
    /** Luz difusa dos fundos decorativos. */
    public final Color glow;

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
        chartGrid = c(hex[i++]);
        primarySoft = c(hex[i++]);
        focusRing = c(hex[i++]);
        shadow = c(hex[i++]);
        brandDeep = c(hex[i++]);
        brandMid = c(hex[i++]);
        brandBright = c(hex[i++]);
        glow = c(hex[i]);
    }

    private static Color c(String hex) {
        return Color.decode(hex);
    }

    /** A mesma cor com outra opacidade (0-255). */
    public static Color alpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    public static final Palette LIGHT = new Palette(false,
            "#F3F6FB", "#FFFFFF", "#DCE3EE", "#EDF1F7",
            "#0F172A", "#475569", "#64748B",
            "#2563EB", "#1D4ED8", "#FFFFFF",
            "#C62828", "#FDECEC", "#A15C07", "#FEF3C7",
            "#15803D", "#DCFCE7", "#1D4ED8", "#DBEAFE", "#EEF2F8",
            "#0B1736", "#14244B", "#1C3474", "#C3CEE3", "#FFFFFF", "#8A9AC0",
            "#60A5FA",
            "#3B82F6", "#E6EBF3",
            "#E8F0FE", "#93C5FD", "#0F172A",
            "#0F172A", "#1E3A8A", "#2563EB", "#93C5FD");

    public static final Palette DARK = new Palette(true,
            "#0A1120", "#111B2E", "#22304A", "#1A263C",
            "#E5ECF6", "#A9B6CB", "#8F9DB5",
            "#2E6CE6", "#3B82F6", "#FFFFFF",
            "#F87171", "#3A1719", "#FBBF24", "#3A2A0C",
            "#4ADE80", "#0F2E1C", "#93C5FD", "#132A4A", "#18233A",
            "#070E1E", "#101C36", "#172B57", "#B4C1D9", "#FFFFFF", "#7C8DB0",
            "#60A5FA",
            "#60A5FA", "#22304A",
            "#16294D", "#3B82F6", "#000000",
            "#050B19", "#102559", "#1D4ED8", "#3B82F6");
}
