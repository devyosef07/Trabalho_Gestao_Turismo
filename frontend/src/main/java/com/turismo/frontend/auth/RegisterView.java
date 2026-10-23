package com.turismo.frontend.auth;

import com.formdev.flatlaf.FlatClientProperties;
import com.turismo.frontend.api.ApiException;
import com.turismo.frontend.ui.async.Task;
import com.turismo.frontend.ui.components.Alert;
import com.turismo.frontend.ui.components.DateInput;
import com.turismo.frontend.ui.components.FormField;
import com.turismo.frontend.ui.components.UI;
import com.turismo.frontend.ui.motion.MotionPanel;
import com.turismo.frontend.ui.theme.Icons;
import com.turismo.frontend.ui.theme.Theme;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Criação de conta com os quatro campos pedidos: nome completo, data de
 * nascimento, palavra-passe e confirmação.
 *
 * Validação: cada campo é verificado quando a pessoa sai dele e, a partir
 * daí, à medida que escreve; o erro aparece junto do campo. O botão só fica
 * ativo com tudo válido e desativa-se durante o envio. Os erros devolvidos
 * pela API vão para o campo respetivo e os dados escritos mantêm-se.
 *
 * Depois do registo mostra o nome de utilizador gerado pela API (é com ele
 * que se inicia sessão) e o caminho para o ecrã de início de sessão.
 */
public class RegisterView extends JPanel implements AuthShell.Shown {

    private static final String FORM = "formulario";
    private static final String DONE = "sucesso";

    private final AuthShell shell;
    private final Supplier<LocalDate> today;
    private final CardLayout states = new CardLayout();
    private final MotionPanel host = new MotionPanel(states);
    private final Alert alert = new Alert();
    private final JTextField name = new JTextField();
    private final DateInput birth;
    private final JPasswordField password = new JPasswordField();
    private final JPasswordField confirm = new JPasswordField();
    private final PasswordStrength strength = new PasswordStrength();
    private final FormField nameField;
    private final FormField birthField;
    private final FormField passwordField;
    private final FormField confirmField;
    private final UI.PrimaryButton submit = UI.primary("Criar conta", MaterialDesignA.ACCOUNT_PLUS_OUTLINE);
    private final Set<FormField> touched = new HashSet<>();

    private final SuccessCheck check = new SuccessCheck(72);
    private final JLabel createdUser = UI.text(" ", () -> Theme.display().deriveFont(22f), () -> Theme.p().text);
    private String createdUsername;

    RegisterView(AuthShell shell) {
        this(shell, LocalDate::now);
    }

    RegisterView(AuthShell shell, Supplier<LocalDate> today) {
        super(new BorderLayout());
        this.shell = shell;
        this.today = today;
        this.birth = new DateInput(today.get());
        setOpaque(false);
        setName("ecra-registo");

        name.setName("registo-nome");
        name.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "ex.: Maria José Cossa");
        name.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                Icons.text(MaterialDesignA.ACCOUNT_OUTLINE, 18));
        name.getAccessibleContext().setAccessibleName("Nome completo");
        nameField = new FormField("Nome completo", name, "Nome e apelido, como no documento de identificação.");

        birthField = new FormField("Data de nascimento", birth, "Tem de ter pelo menos 13 anos.");

        password.setName("registo-palavra-passe");
        password.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Pelo menos 8 caracteres");
        password.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                Icons.text(MaterialDesignL.LOCK_OUTLINE, 18));
        password.getAccessibleContext().setAccessibleName("Palavra-passe");
        passwordField = new FormField("Palavra-passe", password, null);

        confirm.setName("registo-confirmacao");
        confirm.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Repita a palavra-passe");
        confirm.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                Icons.text(MaterialDesignL.LOCK_CHECK_OUTLINE, 18));
        confirm.getAccessibleContext().setAccessibleName("Confirmar palavra-passe");
        confirmField = new FormField("Confirmar palavra-passe", confirm, null);

        host.add(buildForm(), FORM);
        host.add(buildSuccess(), DONE);
        add(new AuthLayout(AuthLayout.brandColumn("Comece em<br>menos de um minuto.",
                "Só pedimos quatro dados. O nome de utilizador é criado a partir do seu nome.",
                new String[][]{
                        {"Sem dados desnecessários", "Nome, data de nascimento e palavra-passe.", "pessoa"},
                        {"Palavra-passe protegida", "Guardada só como hash PBKDF2 com sal.", "chave"},
                        {"Ligado à agência", "Acesso aos 15 módulos da base de dados.", "base"}}),
                host), BorderLayout.CENTER);
        wireValidation();
    }

    private JComponent buildForm() {
        JPanel form = column();

        JButton back = UI.link("Voltar", () -> Theme.p().textSecondary);
        back.setIcon(Icons.of(MaterialDesignA.ARROW_LEFT, 16, () -> Theme.p().textSecondary));
        back.setName("voltar");
        back.addActionListener(e -> shell.show(AuthShell.WELCOME));
        add(form, back);
        form.add(Box.createVerticalStrut(UI.S5));
        add(form, UI.text("Criar conta", Theme::display, () -> Theme.p().text));
        form.add(Box.createVerticalStrut(UI.S2));
        add(form, UI.secondary("Preencha os quatro campos. Todos são obrigatórios."));
        form.add(Box.createVerticalStrut(UI.S5));

        alert.setName("alerta-registo");
        add(form, alert);
        form.add(Box.createVerticalStrut(UI.S1));
        add(form, nameField);
        form.add(Box.createVerticalStrut(UI.S1));
        add(form, birthField);
        form.add(Box.createVerticalStrut(UI.S1));
        passwordField.setExtra(strength);
        add(form, passwordField);
        form.add(Box.createVerticalStrut(UI.S1));
        add(form, confirmField);
        form.add(Box.createVerticalStrut(UI.S4));

        submit.setName("criar-conta");
        submit.setEnabled(false);
        submit.setPreferredSize(new Dimension(10, 46));
        submit.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        submit.addActionListener(e -> submit());
        add(form, submit);
        form.add(Box.createVerticalStrut(UI.S5));

        JPanel login = new JPanel(new FlowLayout(FlowLayout.CENTER, UI.S1, 0));
        login.setOpaque(false);
        login.add(UI.secondary("Já tem conta?"));
        JButton go = UI.link("Iniciar sessão");
        go.setName("ir-entrar");
        go.addActionListener(e -> shell.show(AuthShell.LOGIN));
        login.add(go);
        add(form, login);
        return form;
    }

    private JComponent buildSuccess() {
        JPanel p = column();
        p.setName("registo-sucesso");
        p.add(Box.createVerticalGlue());
        check.setMaximumSize(check.getPreferredSize());
        add(p, check);
        p.add(Box.createVerticalStrut(UI.S5));
        add(p, UI.text("Conta criada com sucesso", Theme::display, () -> Theme.p().text));
        p.add(Box.createVerticalStrut(UI.S2));
        add(p, UI.secondary("Guarde o seu nome de utilizador: é com ele que inicia sessão."));
        p.add(Box.createVerticalStrut(UI.S5));

        JPanel chip = new JPanel(new BorderLayout(UI.S3, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.p().primarySoft);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(com.turismo.frontend.ui.theme.Palette.alpha(Theme.p().primary, 90));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        chip.setOpaque(false);
        chip.setBorder(UI.pad(UI.S3, UI.S4, UI.S3, UI.S2));
        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        texts.add(UI.stretch(UI.fieldLabel("Nome de utilizador")));
        createdUser.setName("utilizador-criado");
        texts.add(UI.stretch(createdUser));
        chip.add(texts, BorderLayout.CENTER);
        JButton copy = UI.iconButton(MaterialDesignC.CONTENT_COPY, "Copiar nome de utilizador");
        copy.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(createdUsername), null);
            copy.setIcon(Icons.of(MaterialDesignC.CHECK, 18, () -> Theme.p().success));
            UI.setTooltip(copy, "Copiado");
        });
        chip.add(copy, BorderLayout.EAST);
        add(p, chip);
        p.add(Box.createVerticalStrut(UI.S6));

        UI.PrimaryButton toLogin = UI.primary("Iniciar sessão", MaterialDesignL.LOGIN);
        toLogin.setName("sucesso-entrar");
        toLogin.setPreferredSize(new Dimension(10, 46));
        toLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        toLogin.addActionListener(e -> {
            String user = createdUsername;
            reset();
            shell.showLogin(user, UI.Tone.SUCCESS, "Conta criada. Introduza a palavra-passe para entrar.");
        });
        add(p, toLogin);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private static JPanel column() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    private static void add(JPanel form, JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        if ((c instanceof JPanel || c instanceof JLabel) && !(c instanceof Alert)) {
            UI.stretch(c);
        }
        form.add(c);
    }

    // ------------------------------------------------------------ validação

    private void wireValidation() {
        onEdit(name, () -> live(nameField));
        onEdit(password, () -> {
            strength.update(new String(password.getPassword()));
            live(passwordField);
            if (confirm.getDocument().getLength() > 0) {
                touched.add(confirmField);
                live(confirmField);
            }
        });
        onEdit(confirm, () -> {
            // Só avisa da diferença quando a confirmação já tem o tamanho da palavra-passe.
            if (confirm.getDocument().getLength() >= password.getDocument().getLength()) {
                touched.add(confirmField);
            }
            live(confirmField);
        });
        birth.setOnChange(() -> {
            if (!birth.isPartial()) {
                touched.add(birthField);
            }
            live(birthField);
        });
        blur(name, nameField);
        blur(password, passwordField);
        blur(confirm, confirmField);
        for (Component c : birth.getComponents()) {
            c.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    if (e.getOppositeComponent() == null || !birth.isAncestorOf(e.getOppositeComponent())) {
                        touched.add(birthField);
                        live(birthField);
                    }
                }
            });
        }
        name.addActionListener(e -> birth.dayBox().requestFocusInWindow());
        password.addActionListener(e -> confirm.requestFocusInWindow());
        confirm.addActionListener(e -> submit());
    }

    private void blur(JComponent input, FormField field) {
        input.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (!(input instanceof JTextField) || ((JTextField) input).getDocument().getLength() > 0) {
                    touched.add(field);
                }
                live(field);
            }
        });
    }

    private static void onEdit(JTextField f, Runnable r) {
        f.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                r.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                r.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                r.run();
            }
        });
    }

    /** Volta a validar um campo (mostra o erro só se a pessoa já lá passou) e o botão. */
    private void live(FormField field) {
        if (touched.contains(field)) {
            field.setError(errorFor(field));
        }
        submit.setEnabled(!submit.isLoading() && errorFor(nameField) == null && errorFor(birthField) == null
                && errorFor(passwordField) == null && errorFor(confirmField) == null);
    }

    private String errorFor(FormField field) {
        if (field == nameField) {
            return AccountRules.checkName(name.getText());
        }
        if (field == birthField) {
            return AccountRules.checkBirthDate(birth.getDate(), today.get());
        }
        if (field == passwordField) {
            return AccountRules.checkPassword(new String(password.getPassword()));
        }
        return AccountRules.checkConfirmation(new String(password.getPassword()), new String(confirm.getPassword()));
    }

    private void validateAll() {
        for (FormField f : new FormField[]{nameField, birthField, passwordField, confirmField}) {
            touched.add(f);
            live(f);
        }
    }

    // ------------------------------------------------------------- envio

    private void submit() {
        validateAll();
        if (submit.isLoading() || !submit.isEnabled()) {
            return;
        }
        String fullName = name.getText();
        LocalDate date = birth.getDate();
        String secret = new String(password.getPassword());
        String again = new String(confirm.getPassword());
        alert.hideMessage();
        submit.setLoading(true, "A criar conta…");
        Task.run(() -> shell.auth().register(fullName, date, secret, again), this::showSuccess, this::showError,
                () -> {
                    submit.setLoading(false, null);
                    live(nameField);
                });
    }

    private void showSuccess(AuthService.Registration r) {
        createdUsername = r.username();
        createdUser.setText(r.username());
        password.setText("");
        confirm.setText("");
        states.show(host, DONE);
        host.reveal(380, 0, 0, 16);
        check.play();
    }

    private void showError(Exception error) {
        if (error instanceof ApiException && ((ApiException) error).isUnreachable()) {
            alert.show(UI.Tone.WARNING, "Não foi possível contactar o servidor. Os dados que escreveu mantêm-se; "
                    + "tente novamente quando a API estiver em execução.");
            return;
        }
        Map<String, String> fields = error instanceof ApiException
                ? ((ApiException) error).getFields() : Map.of();
        boolean mapped = false;
        for (Map.Entry<String, String> f : fields.entrySet()) {
            FormField target = switch (f.getKey()) {
                case "nomeCompleto" -> nameField;
                case "dataNascimento" -> birthField;
                case "palavraPasse" -> passwordField;
                case "confirmacao" -> confirmField;
                default -> null;
            };
            if (target != null) {
                target.setError(f.getValue());
                mapped = true;
            }
        }
        if (!mapped) {
            alert.show(UI.Tone.DANGER, error.getMessage());
        }
    }

    /** Volta ao formulário vazio (depois de usar o nome criado para entrar). */
    private void reset() {
        name.setText("");
        password.setText("");
        confirm.setText("");
        touched.clear();
        for (FormField f : new FormField[]{nameField, birthField, passwordField, confirmField}) {
            f.setError(null);
        }
        alert.hideMessage();
        states.show(host, FORM);
    }

    @Override
    public void onShown() {
        name.requestFocusInWindow();
    }

    // Acesso para testes
    public JTextField nameInput() {
        return name;
    }

    public DateInput birthInput() {
        return birth;
    }

    public JPasswordField passwordInput() {
        return password;
    }

    public JPasswordField confirmInput() {
        return confirm;
    }

    public UI.PrimaryButton submitButton() {
        return submit;
    }

    public String errorText(String field) {
        return switch (field) {
            case "nome" -> nameField.error();
            case "nascimento" -> birthField.error();
            case "palavraPasse" -> passwordField.error();
            default -> confirmField.error();
        };
    }

    public String createdUsername() {
        return createdUsername;
    }

    public boolean isShowingSuccess() {
        for (Component c : host.getComponents()) {
            if (c.isVisible()) {
                return "registo-sucesso".equals(c.getName());
            }
        }
        return false;
    }

    public String alertText() {
        return alert.message();
    }
}
