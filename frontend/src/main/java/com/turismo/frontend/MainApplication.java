package com.turismo.frontend;

import com.turismo.frontend.api.ApiClient;
import com.turismo.frontend.api.ClientConfig;
import com.turismo.frontend.modules.Format;
import com.turismo.frontend.ui.theme.Theme;

import javax.swing.SwingUtilities;
import java.util.Locale;

/**
 * Main application class for the Turismo Management System
 * Launches the main application window
 *
 * A aplicação comunica com a base de dados através da API REST do backend
 * (com.turismo.api.TurismoApiServer), cujo endereço está em config/turismo.properties.
 */
public class MainApplication {

    /**
     * Main method to launch the application
     * @param args command line arguments
     */
    public static void main(String[] args) {
        Locale.setDefault(Format.PT);
        System.setProperty("apple.awt.application.name", "Gestão de Turismo");
        System.setProperty("flatlaf.uiScale.allowScaleDown", "true");
        ClientConfig config = ClientConfig.load();
        ApiClient api = new ApiClient(config);

        // Use the event dispatch thread for Swing components
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            new MainWindow(api).setVisible(true);
        });
    }
}
