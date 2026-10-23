package com.turismo.frontend.ui.motion;

import javax.swing.Timer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Animações curtas da interface (opacidade e deslocamento), sempre na EDT.
 *
 * Regras de desempenho: cada animação tem o seu temporizador a ~60 fps que
 * para sozinho no fim (nada corre em ciclo permanente) e só pinta o
 * componente animado. Com "reduzir movimento" ativo — preferência do macOS
 * ou -Dturismo.reduzirMovimento=true — salta diretamente para o estado final.
 */
public final class Motion {

    /** Durações padrão (ms): micro-interações, entradas e transições de ecrã. */
    public static final int FAST = 140, NORMAL = 220, SLOW = 360;

    private static volatile Boolean reduced;
    private static int active;

    private Motion() {
    }

    /** Controlo de uma animação em curso. */
    public static final class Handle {
        private final Timer timer;
        private final Runnable finish;

        Handle(Timer timer, Runnable finish) {
            this.timer = timer;
            this.finish = finish;
        }

        public boolean isRunning() {
            return timer != null && timer.isRunning();
        }

        /** Para a animação e aplica logo o estado final. */
        public void finish() {
            if (isRunning()) {
                timer.stop();
                finish.run();
            }
        }

        public void cancel() {
            if (timer != null && timer.isRunning()) {
                timer.stop();
                active--;
            }
        }
    }

    /**
     * Anima de 0 a 1 em {@code durationMs}, depois de {@code delayMs}; {@code frame}
     * recebe o progresso já suavizado (ease-out cúbico) e {@code done} corre no fim.
     */
    public static Handle animate(int durationMs, int delayMs, Consumer<Float> frame, Runnable done) {
        if (isReduced() || durationMs <= 0) {
            frame.accept(1f);
            if (done != null) {
                done.run();
            }
            return new Handle(null, () -> { });
        }
        long[] start = {-1};
        Runnable finish = () -> {
            active--;
            frame.accept(1f);
            if (done != null) {
                done.run();
            }
        };
        Timer timer = new Timer(16, null);
        timer.addActionListener(e -> {
            long now = System.nanoTime();
            if (start[0] < 0) {
                start[0] = now;
            }
            float t = Math.min(1f, (now - start[0]) / (durationMs * 1_000_000f));
            if (t >= 1f) {
                timer.stop();
                finish.run();
            } else {
                frame.accept(easeOut(t));
            }
        });
        timer.setInitialDelay(Math.max(0, delayMs));
        timer.setCoalesce(true);
        frame.accept(0f);
        active++;
        timer.start();
        return new Handle(timer, finish);
    }

    public static Handle animate(int durationMs, Consumer<Float> frame, Runnable done) {
        return animate(durationMs, 0, frame, done);
    }

    /** Número de animações a decorrer (EDT); 0 quando a interface está parada. */
    public static int activeCount() {
        return active;
    }

    public static float easeOut(float t) {
        float u = 1 - t;
        return 1 - u * u * u;
    }

    public static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    public static boolean isReduced() {
        Boolean r = reduced;
        if (r == null) {
            r = detectReduced();
            reduced = r;
        }
        return r;
    }

    /** Só para testes e para a opção da linha de comandos. */
    public static void setReduced(boolean value) {
        reduced = value;
    }

    private static boolean detectReduced() {
        String prop = System.getProperty("turismo.reduzirMovimento");
        if (prop != null) {
            return Boolean.parseBoolean(prop);
        }
        if (!System.getProperty("os.name", "").toLowerCase().contains("mac")) {
            return false;
        }
        // Preferência de acessibilidade do macOS ("Reduzir movimento"); lida uma vez.
        try {
            Process p = new ProcessBuilder("defaults", "read", "com.apple.universalaccess", "reduceMotion")
                    .redirectErrorStream(true).start();
            try (InputStream in = p.getInputStream()) {
                if (!p.waitFor(800, TimeUnit.MILLISECONDS)) {
                    p.destroy();
                    return false;
                }
                return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim().equals("1");
            }
        } catch (IOException | InterruptedException | SecurityException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }
}
