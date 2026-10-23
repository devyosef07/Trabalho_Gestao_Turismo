package com.turismo.frontend.ui.components;

import com.turismo.frontend.ui.theme.Icons;
import com.turismo.frontend.ui.theme.Palette;
import com.turismo.frontend.ui.theme.Theme;
import org.kordamp.ikonli.Ikon;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Navegação lateral agrupada, sobre um azul profundo. A secção ativa é marcada
 * por fundo, peso do texto e um traço azul-claro que desliza até ela (150 ms). Navegável por teclado: Tab chega aos itens e Enter/Espaço ativa.
 */
public class Sidebar extends JPanel {

    public static final int WIDTH = 256;

    private final JPanel items = new JPanel() {
        @Override
        protected void paintChildren(Graphics g) {
            super.paintChildren(g);
            paintIndicator(g);
        }
    };
    private final List<Item> all = new ArrayList<>();
    private final Consumer<String> onSelect;
    private final JPanel footer = new JPanel(new BorderLayout());
    private Item active;

    // Animação do indicador (coordenadas no painel "items")
    private float indicatorY = -1;
    private float indicatorH = 0;
    private float fromY;
    private float toY;
    private float fromH;
    private float toH;
    private long animStart;
    private final Timer anim;

    public Sidebar(String appName, String appSubtitle, Consumer<String> onSelect) {
        super(new BorderLayout());
        this.onSelect = onSelect;
        setOpaque(true);
        setPreferredSize(new Dimension(WIDTH, 600));

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBorder(UI.pad(UI.S5, UI.S5, UI.S3, UI.S4));
        JLabel name = UI.text(appName, () -> Theme.section().deriveFont(16f), () -> Theme.p().sidebarTextActive);
        JLabel sub = UI.text(appSubtitle, Theme::small, () -> Theme.p().sidebarMuted);
        JPanel names = new JPanel();
        names.setOpaque(false);
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        names.add(name);
        names.add(Box.createVerticalStrut(1));
        names.add(sub);
        JPanel row = new JPanel(new BorderLayout(UI.S3, 0));
        row.setOpaque(false);
        row.add(new JLabel(new BrandMark(36)), BorderLayout.WEST);
        row.add(names, BorderLayout.CENTER);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.add(row);
        add(brand, BorderLayout.NORTH);

        items.setOpaque(false);
        items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));
        items.setBorder(UI.pad(0, UI.S3, UI.S4, UI.S3));
        JPanel itemsHolder = new JPanel(new BorderLayout());
        itemsHolder.setOpaque(false);
        itemsHolder.add(items, BorderLayout.NORTH);
        JScrollPane scroll = Scroll.vertical(itemsHolder);
        scroll.getVerticalScrollBar().setOpaque(false);
        scroll.getVerticalScrollBar().putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE,
                "width: 6; trackArc: 999; thumbArc: 999; thumbInsets: 0,0,0,0");
        add(scroll, BorderLayout.CENTER);

        footer.setOpaque(false);
        footer.setBorder(UI.pad(UI.S3, UI.S4, UI.S4, UI.S4));
        add(footer, BorderLayout.SOUTH);

        anim = new Timer(15, e -> stepAnimation());
    }

    @Override
    public Color getBackground() {
        return Theme.p().sidebar;
    }

    /** Azul profundo com um gradiente vertical e uma luz difusa no topo. */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth();
        int h = getHeight();
        Color bottom = Backdrop.mix(Theme.p().sidebar, Theme.p().brandMid, 0.35f);
        g2.setPaint(new java.awt.GradientPaint(0, 0, Theme.p().sidebar, 0, h, bottom));
        g2.fillRect(0, 0, w, h);
        float r = w * 1.1f;
        g2.setPaint(new java.awt.RadialGradientPaint(new java.awt.geom.Point2D.Float(w * 0.15f, 0), r,
                new float[]{0f, 1f}, new Color[]{Palette.alpha(Theme.p().brandBright, 46),
                Palette.alpha(Theme.p().brandBright, 0)}));
        g2.fillRect(0, 0, w, Math.round(r));
        g2.setColor(new Color(255, 255, 255, 14));
        g2.drawLine(w - 1, 0, w - 1, h);
        g2.dispose();
    }

    public void addGroup(String title) {
        if (items.getComponentCount() > 0) {
            items.add(Box.createVerticalStrut(UI.S3));
        }
        JLabel label = UI.text(title, Theme::label, () -> Theme.p().sidebarMuted);
        label.setBorder(UI.pad(0, UI.S3, UI.S1, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height + UI.S1));
        items.add(label);
    }

    public void addItem(String id, String text, Ikon icon) {
        Item item = new Item(id, text, icon);
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        all.add(item);
        items.add(item);
    }

    public void setFooter(JComponent c) {
        footer.removeAll();
        footer.add(c, BorderLayout.CENTER);
    }

    /** Marca o item como ativo sem disparar a navegação; um id sem item (ex.: "conta") não marca nenhum. */
    public void setActive(String id) {
        for (Item item : all) {
            if (item.id.equals(id)) {
                Item previous = active;
                active = item;
                all.forEach(Component::repaint);
                animateTo(item, previous == null);
                item.scrollRectToVisible(new Rectangle(0, 0, item.getWidth(), item.getHeight()));
                return;
            }
        }
        active = null;
        anim.stop();
        indicatorY = -1;
        all.forEach(Component::repaint);
        items.repaint();
    }

    public String getActive() {
        return active == null ? null : active.id;
    }

    public List<String> itemIds() {
        List<String> ids = new ArrayList<>();
        all.forEach(i -> ids.add(i.id));
        return ids;
    }

    public boolean isAnimating() {
        return anim.isRunning();
    }

    private void animateTo(Item item, boolean immediate) {
        if (item.getHeight() == 0 || immediate || indicatorY < 0) {
            // Ainda sem layout: posiciona diretamente quando o layout existir.
            anim.stop();
            indicatorY = -1;
            items.repaint();
            return;
        }
        fromY = indicatorY;
        fromH = indicatorH;
        toY = item.getY();
        toH = item.getHeight();
        animStart = System.nanoTime();
        anim.start();
    }

    private void stepAnimation() {
        float t = Math.min(1f, (System.nanoTime() - animStart) / 150_000_000f);
        float eased = 1 - (1 - t) * (1 - t) * (1 - t);
        indicatorY = fromY + (toY - fromY) * eased;
        indicatorH = fromH + (toH - fromH) * eased;
        items.repaint();
        if (t >= 1f) {
            anim.stop();
        }
    }

    /** Traço do item ativo, desenhado no próprio painel dos itens (mesmas coordenadas). */
    private void paintIndicator(Graphics g) {
        if (active == null || active.getHeight() == 0) {
            return;
        }
        if (indicatorY < 0 || (!anim.isRunning() && indicatorY != active.getY())) {
            indicatorY = active.getY();
            indicatorH = active.getHeight();
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Theme.p().accent);
        g2.fillRoundRect(active.getX() + 2, Math.round(indicatorY + 8), 3, Math.round(indicatorH - 16), 3, 3);
        g2.dispose();
    }

    private final class Item extends JPanel {
        final String id;
        boolean hover;

        Item(String id, String text, Ikon icon) {
            super(new BorderLayout(UI.S3, 0));
            this.id = id;
            setOpaque(false);
            setFocusable(true);
            setBorder(UI.pad(6, UI.S4, 6, UI.S3));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            JLabel iconLabel = new JLabel(Icons.of(icon, 18,
                    () -> active == this ? Theme.p().sidebarTextActive : Theme.p().sidebarText));
            JLabel label = UI.text(text,
                    () -> active == this ? Theme.strong() : Theme.body(),
                    () -> active == this ? Theme.p().sidebarTextActive : Theme.p().sidebarText);
            add(iconLabel, BorderLayout.WEST);
            add(label, BorderLayout.CENTER);
            getAccessibleContext().setAccessibleName(text);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    select();
                }
            });
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    repaint();
                }
            });
            AbstractAction activate = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    select();
                }
            };
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "ativar");
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "ativar");
            getActionMap().put("ativar", activate);
        }

        void select() {
            if (active != this) {
                setActive(id);
                onSelect.accept(id);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (active == this) {
                g2.setColor(Theme.p().sidebarActive);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            } else if (hover) {
                g2.setColor(Theme.p().sidebarHover);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            }
            if (isFocusOwner()) {
                g2.setColor(Theme.p().accent);
                g2.setStroke(new java.awt.BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);
            }
            g2.dispose();
        }
    }
}
