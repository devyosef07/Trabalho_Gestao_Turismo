package com.turismo.frontend.auth;

import com.google.gson.JsonObject;
import com.turismo.frontend.ui.async.Task;
import com.turismo.frontend.ui.components.BrandMark;
import com.turismo.frontend.ui.components.UI;
import com.turismo.frontend.ui.motion.Motion;
import com.turismo.frontend.ui.motion.MotionPanel;
import com.turismo.frontend.ui.theme.Theme;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/**
 * Ecrã de boas-vindas: símbolo, nome, uma frase sobre a aplicação e os dois
 * caminhos possíveis (iniciar sessão ou criar conta). Os elementos entram um
 * a um (fade e subida de 14 px, 70 ms de intervalo). No canto, o estado real
 * do servidor, lido de /api/estado.
 */
public class WelcomeView extends JPanel implements AuthShell.Shown {

    private final AuthShell shell;
    private final List<MotionPanel> parts = new ArrayList<>();
    private final JLabel server = AuthLayout.onDark("A verificar o servidor…", Theme.small(), 200);
    private final JButton enter = UI.primary("Iniciar sessão", MaterialDesignL.LOGIN);
    private final JButton create = UI.glass("Criar conta", MaterialDesignA.ACCOUNT_PLUS_OUTLINE);
    private Color serverDot = new Color(255, 255, 255, 120);

    WelcomeView(AuthShell shell) {
        super(new BorderLayout());
        this.shell = shell;
        setOpaque(false);
        setName("ecra-boas-vindas");

        JPanel column = new JPanel(new GridBagLayout());
        column.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.CENTER;

        JLabel mark = new JLabel(new BrandMark(84));
        column.add(part(mark), c);

        c.gridy++;
        c.insets = new Insets(UI.S5, 0, 0, 0);
        JLabel title = AuthLayout.onDark("Gestão de Turismo", Theme.hero(), 255);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        column.add(part(title), c);

        c.gridy++;
        c.insets = new Insets(UI.S3, 0, 0, 0);
        JLabel lead = AuthLayout.onDark("<html><div style='text-align:center'>Destinos, reservas e operadores de "
                + "Moçambique,<br>num só lugar e ligados à base de dados Oracle.</div></html>",
                Theme.body().deriveFont(16f), 205);
        lead.setHorizontalAlignment(SwingConstants.CENTER);
        column.add(part(lead), c);

        c.gridy++;
        c.insets = new Insets(UI.S6 + UI.S2, 0, 0, 0);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, UI.S3, 0));
        buttons.setOpaque(false);
        enter.setName("boas-vindas-entrar");
        create.setName("boas-vindas-criar");
        Dimension size = new Dimension(190, 48);
        enter.setPreferredSize(size);
        create.setPreferredSize(size);
        enter.addActionListener(e -> shell.show(AuthShell.LOGIN));
        create.addActionListener(e -> shell.show(AuthShell.REGISTER));
        buttons.add(enter);
        buttons.add(create);
        column.add(part(buttons), c);

        add(column, BorderLayout.CENTER);

        JPanel foot = new JPanel(new BorderLayout());
        foot.setOpaque(false);
        foot.setBorder(UI.pad(0, 40, 28, 40));
        foot.add(AuthLayout.onDark("ISCTEM 2026 · Base de Dados I · Grupo 1", Theme.small(), 150), BorderLayout.WEST);
        server.setIcon(new javax.swing.Icon() {
            @Override
            public void paintIcon(Component comp, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(serverDot);
                g2.fillOval(x, y + 1, 8, 8);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 8;
            }

            @Override
            public int getIconHeight() {
                return 10;
            }
        });
        server.setIconTextGap(UI.S2);
        server.setName("estado-servidor");
        foot.add(server, BorderLayout.EAST);
        add(foot, BorderLayout.SOUTH);
    }

    private MotionPanel part(JComponent c) {
        MotionPanel p = new MotionPanel(new BorderLayout());
        p.add(c);
        parts.add(p);
        return p;
    }

    @Override
    public void onShown() {
        for (int i = 0; i < parts.size(); i++) {
            parts.get(i).reveal(Motion.SLOW, 60 + i * 70, 0, 14);
        }
        enter.requestFocusInWindow();
        checkServer();
    }

    private void checkServer() {
        Task.run(() -> shell.auth().api().getObject("/estado"), this::showServer, error -> {
            serverDot = new Color(0xFB, 0xBF, 0x24);
            server.setText("Servidor indisponível — verifique se a API está em execução");
            server.repaint();
        });
    }

    private void showServer(JsonObject estado) {
        boolean db = "OK".equals(estado.get("bd").getAsString());
        serverDot = db ? new Color(0x4A, 0xDE, 0x80) : new Color(0xFB, 0xBF, 0x24);
        server.setText(db ? "Servidor e base de dados disponíveis" : "Servidor sem ligação à base de dados");
        server.repaint();
    }

    public String serverText() {
        return server.getText();
    }
}
