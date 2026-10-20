package com.turismo.frontend;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/** Utilitários para conduzir a interface real a partir dos testes, sempre na EDT. */
final class SwingTestSupport {

    private SwingTestSupport() {
    }

    static <T> T edt(Callable<T> work) {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    result.set(work.call());
                } catch (Exception e) {
                    error.set(e);
                }
            });
        } catch (InterruptedException | InvocationTargetException e) {
            throw new AssertionError(e);
        }
        if (error.get() != null) {
            throw new AssertionError(error.get());
        }
        return result.get();
    }

    static void edt(Runnable work) {
        edt(() -> {
            work.run();
            return null;
        });
    }

    /** Espera (fora da EDT) até a condição, avaliada na EDT, ser verdadeira. */
    static void waitFor(String what, long timeoutMs, BooleanSupplier condition) {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            if (edt(condition::getAsBoolean)) {
                return;
            }
            try {
                Thread.sleep(40);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        throw new AssertionError("Tempo esgotado à espera de: " + what);
    }

    @SuppressWarnings("unchecked")
    static <T extends Component> T find(Container root, String name, Class<T> type) {
        for (Component c : root.getComponents()) {
            if (name.equals(c.getName()) && type.isInstance(c) && c.isShowing()) {
                return (T) c;
            }
            if (c instanceof Container) {
                T found = find((Container) c, name, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    static boolean containsText(Container root, String text) {
        for (Component c : root.getComponents()) {
            if (!c.isShowing()) {
                continue;
            }
            if (c instanceof javax.swing.JLabel && ((javax.swing.JLabel) c).getText() != null
                    && ((javax.swing.JLabel) c).getText().contains(text)) {
                return true;
            }
            if (c instanceof Container && containsText((Container) c, text)) {
                return true;
            }
        }
        return false;
    }

    static void screenshot(JComponent c, String name) {
        // Espera pelo fim de animações (ex.: indicador da navegação) para a captura ser estável.
        waitFor("animações", 2_000, () -> {
            java.awt.Window w = SwingUtilities.getWindowAncestor(c);
            return !(w instanceof MainWindow) || !((MainWindow) w).sidebar().isAnimating();
        });
        File dir = new File("target/screenshots");
        dir.mkdirs();
        BufferedImage img = edt(() -> {
            // paint() não aplica layouts pendentes; validate() garante a imagem que o utilizador vê.
            c.validate();
            BufferedImage b = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = b.createGraphics();
            c.paint(g);
            g.dispose();
            return b;
        });
        try {
            ImageIO.write(img, "png", new File(dir, name + ".png"));
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
