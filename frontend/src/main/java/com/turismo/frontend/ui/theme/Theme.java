package com.turismo.frontend.ui.theme;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.SwingUtilities;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Aparência da aplicação: FlatLaf (igual em macOS e Windows), fonte IBM Plex
 * Sans incluída no jar e tokens de cor de {@link Palette}.
 */
public final class Theme {

    public static final String FONT_FAMILY = "IBM Plex Sans";
    private static final String PREF_DARK = "temaEscuro";

    private static volatile Palette palette = Palette.LIGHT;
    private static final List<Runnable> listeners = new ArrayList<>();
    private static Font regular;
    private static Font medium;
    private static Font semibold;

    private Theme() {
    }

    public static Palette p() {
        return palette;
    }

    public static boolean isDark() {
        return palette.dark;
    }

    /** Instala as fontes e o tema guardado nas preferências do utilizador. */
    public static void install() {
        loadFonts();
        boolean dark = false;
        try {
            dark = Preferences.userNodeForPackage(Theme.class).getBoolean(PREF_DARK, false);
        } catch (SecurityException ignored) {
            // Sem acesso às preferências: usa o tema claro.
        }
        apply(dark);
    }

    public static void setDark(boolean dark) {
        apply(dark);
        try {
            Preferences.userNodeForPackage(Theme.class).putBoolean(PREF_DARK, dark);
        } catch (SecurityException ignored) {
            // A preferência fica só para esta sessão.
        }
        for (Window w : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(w);
            w.repaint();
        }
        for (Runnable r : new ArrayList<>(listeners)) {
            r.run();
        }
    }

    public static void addListener(Runnable r) {
        listeners.add(r);
    }

    public static void removeListener(Runnable r) {
        listeners.remove(r);
    }

    private static void apply(boolean dark) {
        palette = dark ? Palette.DARK : Palette.LIGHT;
        Palette p = palette;
        Map<String, String> extra = new HashMap<>();
        extra.put("@accentColor", hex(p.primary));
        extra.put("@background", hex(p.page));
        extra.put("@foreground", hex(p.text));
        extra.put("Component.arc", "10");
        extra.put("Button.arc", "10");
        extra.put("TextComponent.arc", "10");
        extra.put("CheckBox.arc", "5");
        extra.put("Component.focusWidth", "2");
        extra.put("Component.innerFocusWidth", "0");
        extra.put("Component.focusColor", hex(p.focusRing));
        extra.put("Component.focusedBorderColor", hex(p.primary));
        extra.put("Component.borderColor", hex(p.border));
        extra.put("Button.margin", "7,16,7,16");
        extra.put("Button.hoverBorderColor", hex(p.primary));
        extra.put("TextField.margin", "6,10,6,10");
        extra.put("PasswordField.margin", "6,10,6,10");
        extra.put("PasswordField.showRevealButton", "true");
        extra.put("ComboBox.padding", "5,10,5,10");
        extra.put("Table.rowHeight", "38");
        extra.put("Table.background", hex(p.card));
        extra.put("Table.showHorizontalLines", "true");
        extra.put("Table.showVerticalLines", "false");
        extra.put("Table.gridColor", hex(p.line));
        extra.put("Table.intercellSpacing", "0,1");
        extra.put("Table.selectionBackground", hex(p.primarySoft));
        extra.put("Table.selectionForeground", hex(p.text));
        extra.put("Table.selectionInactiveBackground", hex(p.neutralSurface));
        extra.put("Table.selectionInactiveForeground", hex(p.text));
        extra.put("TableHeader.height", "38");
        extra.put("TableHeader.background", hex(p.card));
        extra.put("TableHeader.separatorColor", hex(p.card));
        extra.put("TableHeader.bottomSeparatorColor", hex(p.border));
        extra.put("TableHeader.foreground", hex(p.textSecondary));
        extra.put("ScrollPane.background", hex(p.card));
        extra.put("ScrollBar.width", "10");
        extra.put("ScrollBar.thumbArc", "999");
        extra.put("ScrollBar.thumbInsets", "2,2,2,2");
        extra.put("ScrollBar.showButtons", "false");
        extra.put("TabbedPane.tabHeight", "38");
        extra.put("TabbedPane.selectedBackground", hex(p.page));
        extra.put("TabbedPane.underlineColor", hex(p.primary));
        extra.put("TabbedPane.tabSelectionHeight", "2");
        extra.put("PopupMenu.borderCornerRadius", "10");
        extra.put("Popup.borderCornerRadius", "10");
        extra.put("MenuItem.selectionBackground", hex(p.primarySoft));
        extra.put("MenuItem.selectionForeground", hex(p.text));
        extra.put("MenuItem.selectionArc", "8");
        extra.put("MenuItem.selectionInsets", "0,6,0,6");
        extra.put("MenuItem.margin", "6,10,6,10");
        extra.put("ProgressBar.arc", "999");
        extra.put("CheckBox.icon.focusWidth", "2");
        extra.put("ToolTip.background", hex(dark ? p.neutralSurface : p.text));
        extra.put("ToolTip.foreground", hex(dark ? p.text : p.card));
        extra.put("ToolTip.border", "6,10,6,10");
        extra.put("OptionPane.buttonAreaBorder", "12,0,0,0");
        extra.put("SplitPaneDivider.style", "plain");
        extra.put("SplitPane.dividerSize", "1");
        FlatLaf.setGlobalExtraDefaults(extra);
        if (regular != null) {
            FlatLaf.setPreferredFontFamily(FONT_FAMILY);
            FlatLaf.setPreferredSemiboldFontFamily(semibold.getFamily());
        }
        if (dark) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
    }

    private static String hex(java.awt.Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static void loadFonts() {
        regular = register("IBMPlexSans-Regular.ttf");
        Font bold = register("IBMPlexSans-Bold.ttf");
        medium = register("IBMPlexSans-Medium.ttf");
        semibold = register("IBMPlexSans-SemiBold.ttf");
        if (regular == null || bold == null || medium == null || semibold == null) {
            System.err.println("Aviso: fontes IBM Plex Sans não encontradas; a usar a fonte do sistema.");
            regular = null;
        }
    }

    private static Font register(String file) {
        try (InputStream in = Theme.class.getResourceAsStream("/fonts/" + file)) {
            if (in == null) {
                return null;
            }
            Font font = Font.createFont(Font.TRUETYPE_FONT, in);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (IOException | FontFormatException e) {
            return null;
        }
    }

    // ---------------------------------------------------------------
    // Escala tipográfica (13 px de base)
    // ---------------------------------------------------------------

    public static Font body() {
        return font(regular, Font.PLAIN, 13f);
    }

    public static Font small() {
        return font(regular, Font.PLAIN, 12f);
    }

    public static Font label() {
        return font(medium, Font.PLAIN, 12f);
    }

    public static Font strong() {
        return font(semibold, Font.BOLD, 13f);
    }

    public static Font title() {
        return font(semibold, Font.BOLD, 24f);
    }

    public static Font section() {
        return font(semibold, Font.BOLD, 15f);
    }

    public static Font figure() {
        return font(medium, Font.PLAIN, 28f);
    }

    /** Títulos dos ecrãs de autenticação. */
    public static Font display() {
        return font(semibold, Font.BOLD, 28f);
    }

    /** Título do ecrã de boas-vindas. */
    public static Font hero() {
        return font(semibold, Font.BOLD, 40f);
    }

    private static Font font(Font base, int fallbackStyle, float size) {
        if (base == null || regular == null) {
            return new Font(Font.SANS_SERIF, fallbackStyle, Math.round(size));
        }
        return base.deriveFont(size);
    }
}
