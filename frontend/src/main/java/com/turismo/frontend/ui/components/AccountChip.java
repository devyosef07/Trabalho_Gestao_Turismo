package com.turismo.frontend.ui.components;

import com.turismo.frontend.ui.theme.Icons;
import com.turismo.frontend.ui.theme.Theme;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import org.kordamp.ikonli.materialdesign2.MaterialDesignU;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Conta com sessão iniciada, no rodapé da navegação: avatar com as iniciais,
 * nome, nome de utilizador e um ponto verde de sessão ativa. Ao clicar (ou
 * Enter/Espaço) abre o menu da conta: "A minha conta" e "Terminar sessão".
 */
public class AccountChip extends JPanel {

    private final Avatar avatar = new Avatar(34, () -> Theme.p().sidebar);
    private final JLabel name = UI.text(" ", Theme::strong, () -> Theme.p().sidebarTextActive);
    private final JLabel handle = UI.text(" ", Theme::small, () -> Theme.p().sidebarMuted);
    private final JPopupMenu menu = new JPopupMenu();
    private boolean hover;
    private boolean active;

    public AccountChip(Runnable onProfile, Runnable onLogout) {
        super(new BorderLayout(UI.S3, 0));
        setOpaque(false);
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(UI.pad(8, 10, 8, 10));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));

        add(avatar, BorderLayout.WEST);
        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        texts.add(Box.createVerticalGlue());
        texts.add(name);
        texts.add(handle);
        texts.add(Box.createVerticalGlue());
        add(texts, BorderLayout.CENTER);
        add(new JLabel(Icons.of(MaterialDesignU.UNFOLD_MORE_HORIZONTAL, 18, () -> Theme.p().sidebarMuted)),
                BorderLayout.EAST);

        JMenuItem profile = new JMenuItem("A minha conta",
                Icons.of(MaterialDesignA.ACCOUNT_CIRCLE_OUTLINE, 18, () -> Theme.p().textSecondary));
        profile.setName("menu-conta");
        profile.addActionListener(e -> onProfile.run());
        JMenuItem logout = new JMenuItem("Terminar sessão",
                Icons.of(MaterialDesignL.LOGOUT, 18, () -> Theme.p().danger)) {
            @Override
            public Color getForeground() {
                return Theme.p().danger;
            }
        };
        logout.setName("menu-terminar-sessao");
        logout.addActionListener(e -> onLogout.run());
        menu.add(profile);
        menu.addSeparator();
        menu.add(logout);

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
                requestFocusInWindow();
                openMenu();
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
        AbstractAction open = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openMenu();
            }
        };
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "abrir");
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "abrir");
        getActionMap().put("abrir", open);
    }

    public void setPerson(String fullName, String username) {
        avatar.setPerson(fullName, fullName != null);
        name.setText(fullName == null ? "Sem sessão" : fullName);
        handle.setText(username == null ? " " : "@" + username);
        String text = fullName == null ? "Sem sessão iniciada" : "Conta: " + fullName + ". Abrir menu da conta";
        getAccessibleContext().setAccessibleName(text);
        setToolTipText(fullName == null ? null : "Sessão iniciada como " + username);
    }

    /** Realce quando o ecrã "A minha conta" está aberto. */
    public void setActive(boolean value) {
        active = value;
        repaint();
    }

    /** O menu abre por cima do cartão (está no fundo da janela). */
    public void openMenu() {
        menu.pack();
        menu.show(this, 0, -menu.getPreferredSize().height - 4);
    }

    public JPopupMenu menu() {
        return menu;
    }

    public String displayedName() {
        return name.getText();
    }

    public String initials() {
        return avatar.initials();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(active ? Theme.p().sidebarActive : hover ? Theme.p().sidebarHover
                : new Color(255, 255, 255, 10));
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        g2.setColor(new Color(255, 255, 255, 22));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
        if (isFocusOwner()) {
            g2.setColor(Theme.p().accent);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);
        }
        g2.dispose();
    }
}
