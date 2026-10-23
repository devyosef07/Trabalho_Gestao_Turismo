package com.turismo.frontend.auth;

import com.formdev.flatlaf.FlatClientProperties;
import com.turismo.frontend.api.ApiException;
import com.turismo.frontend.ui.async.Task;
import com.turismo.frontend.ui.components.Alert;
import com.turismo.frontend.ui.components.FormField;
import com.turismo.frontend.ui.components.UI;
import com.turismo.frontend.ui.theme.Icons;
import com.turismo.frontend.ui.theme.Theme;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Arrays;

/**
 * Início de sessão: nome de utilizador e palavra-passe, verificados pela API.
 * Erros de credenciais aparecem dentro do formulário (sem janelas por cima);
 * o nome escrito nunca se perde e a palavra-passe fica selecionada para ser
 * reescrita. Enter envia o formulário.
 */
public class LoginView extends JPanel implements AuthShell.Shown {

    private final AuthShell shell;
    private final Alert alert = new Alert();
    private final JTextField username = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final JCheckBox remember = new JCheckBox("Manter sessão iniciada neste computador", true);
    private final UI.PrimaryButton submit = UI.primary("Entrar", MaterialDesignL.LOGIN);
    private final AuthLayout layout;

    LoginView(AuthShell shell) {
        super(new BorderLayout());
        this.shell = shell;
        setOpaque(false);
        setName("ecra-entrar");
        layout = new AuthLayout(AuthLayout.brandColumn("Olá<br>de novo.",
                "Entre para gerir destinos, alojamentos, reservas e pagamentos da agência.",
                new String[][]{
                        {"Tudo num só painel", "Destinos, atrações, alojamentos e transportes.", "mapa"},
                        {"Reservas sempre em dia", "Pagamentos e recibos ligados ao Oracle.", "reserva"},
                        {"Sessão protegida", "Palavras-passe guardadas apenas como hash.", "escudo"}}),
                buildForm());
        add(layout, BorderLayout.CENTER);
    }

    private JComponent buildForm() {
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JButton back = UI.link("Voltar", () -> Theme.p().textSecondary);
        back.setIcon(Icons.of(MaterialDesignA.ARROW_LEFT, 16, () -> Theme.p().textSecondary));
        back.setName("voltar");
        back.addActionListener(e -> shell.show(AuthShell.WELCOME));
        add(form, back);
        form.add(Box.createVerticalStrut(UI.S5));

        add(form, UI.text("Iniciar sessão", Theme::display, () -> Theme.p().text));
        form.add(Box.createVerticalStrut(UI.S2));
        add(form, UI.secondary("Use o nome de utilizador que recebeu ao criar a conta."));
        form.add(Box.createVerticalStrut(UI.S5));

        alert.setName("alerta-entrar");
        add(form, alert);
        form.add(Box.createVerticalStrut(UI.S1));

        username.setName("entrar-utilizador");
        username.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "ex.: maria.cossa");
        username.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                Icons.text(MaterialDesignA.ACCOUNT_OUTLINE, 18));
        username.getAccessibleContext().setAccessibleName("Nome de utilizador");
        add(form, new FormField("Nome de utilizador", username, null));

        password.setName("entrar-palavra-passe");
        password.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "A sua palavra-passe");
        password.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                Icons.text(MaterialDesignL.LOCK_OUTLINE, 18));
        password.getAccessibleContext().setAccessibleName("Palavra-passe");
        add(form, new FormField("Palavra-passe", password, null));

        remember.setOpaque(false);
        remember.setName("manter-sessao");
        remember.setFont(Theme.body());
        remember.setToolTipText("Ao reabrir a aplicação, a sessão é retomada durante 7 dias.");
        add(form, remember);
        form.add(Box.createVerticalStrut(UI.S5));

        submit.setName("entrar");
        submit.setEnabled(false);
        submit.setPreferredSize(new Dimension(10, 46));
        submit.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        submit.addActionListener(e -> submit());
        add(form, submit);
        form.add(Box.createVerticalStrut(UI.S5));

        JPanel signup = new JPanel(new FlowLayout(FlowLayout.CENTER, UI.S1, 0));
        signup.setOpaque(false);
        signup.add(UI.secondary("Ainda não tem conta?"));
        JButton create = UI.link("Criar conta");
        create.setName("ir-criar-conta");
        create.addActionListener(e -> shell.show(AuthShell.REGISTER));
        signup.add(create);
        add(form, signup);

        DocumentListener changes = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateSubmit();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateSubmit();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateSubmit();
            }
        };
        username.getDocument().addDocumentListener(changes);
        password.getDocument().addDocumentListener(changes);
        username.addActionListener(e -> {
            if (password.getPassword().length == 0) {
                password.requestFocusInWindow();
            } else {
                submit();
            }
        });
        password.addActionListener(e -> submit());
        return form;
    }

    private static void add(JPanel form, JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        if ((c instanceof JPanel || c instanceof JLabel) && !(c instanceof Alert)) {
            UI.stretch(c);
        }
        form.add(c);
    }

    private void updateSubmit() {
        submit.setEnabled(!username.getText().isBlank() && password.getDocument().getLength() > 0);
    }

    /** Prepara o ecrã antes de ser mostrado: nome escrito e mensagem opcional. */
    void prepare(String user, UI.Tone tone, String notice) {
        if (user != null) {
            username.setText(user);
        }
        password.setText("");
        if (notice == null) {
            alert.hideMessage();
        } else {
            alert.show(tone, notice);
        }
    }

    @Override
    public void onShown() {
        if (username.getText().isBlank()) {
            username.requestFocusInWindow();
        } else {
            password.requestFocusInWindow();
        }
    }

    private void submit() {
        if (submit.isLoading() || !submit.isEnabled()) {
            return;
        }
        char[] secret = password.getPassword();
        String user = username.getText().trim();
        boolean keep = remember.isSelected();
        alert.hideMessage();
        submit.setLoading(true, "A entrar…");
        Task.run(() -> {
            try {
                return shell.auth().login(user, new String(secret), keep);
            } finally {
                Arrays.fill(secret, '\0');
            }
        }, session -> {
            password.setText("");
            shell.authenticated(session);
        }, this::showError, () -> submit.setLoading(false, null));
    }

    private void showError(Exception error) {
        if (error instanceof ApiException && ((ApiException) error).isUnreachable()) {
            alert.show(UI.Tone.WARNING, "Não foi possível contactar o servidor. Confirme que a API está em "
                    + "execução e tente novamente.");
        } else if (error instanceof ApiException && ((ApiException) error).getStatus() == 503) {
            alert.show(UI.Tone.WARNING, error.getMessage());
        } else {
            alert.show(UI.Tone.DANGER, error.getMessage());
        }
        password.requestFocusInWindow();
        password.selectAll();
    }

    // Acesso para testes
    public JTextField usernameField() {
        return username;
    }

    public JPasswordField passwordField() {
        return password;
    }

    public UI.PrimaryButton submitButton() {
        return submit;
    }

    public String alertText() {
        return alert.message();
    }

    /** Painel da marca visível (janela larga) ou só o cartão do formulário (janela estreita). */
    public boolean isBrandVisible() {
        return layout.isBrandVisible();
    }

    public java.awt.Rectangle formBounds() {
        return layout.sheetBounds();
    }
}
