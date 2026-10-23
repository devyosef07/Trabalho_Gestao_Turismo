package com.turismo.frontend.auth;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Onde fica o token quando a pessoa escolhe "manter sessão iniciada".
 *
 * Por omissão usa as preferências do utilizador do sistema operativo
 * (macOS: ~/Library/Preferences; Windows: registo, HKCU). Não é um cofre
 * cifrado: quem tiver acesso à conta do sistema pode ler o token. Por isso
 * o token expira ao fim de 7 dias, é revogado no servidor ao terminar sessão
 * e nunca dá acesso à palavra-passe.
 */
public interface TokenStore {

    String load();

    void save(String token);

    void clear();

    static TokenStore preferences() {
        return new TokenStore() {
            private static final String KEY = "sessao.token";

            private Preferences node() {
                return Preferences.userNodeForPackage(TokenStore.class);
            }

            @Override
            public String load() {
                try {
                    return node().get(KEY, null);
                } catch (SecurityException | IllegalStateException e) {
                    return null;
                }
            }

            @Override
            public void save(String token) {
                try {
                    node().put(KEY, token);
                    node().flush();
                } catch (SecurityException | IllegalStateException | BackingStoreException e) {
                    // Sem acesso às preferências: a sessão fica só até fechar a aplicação.
                }
            }

            @Override
            public void clear() {
                try {
                    node().remove(KEY);
                    node().flush();
                } catch (SecurityException | IllegalStateException | BackingStoreException e) {
                    // Nada guardado para apagar.
                }
            }
        };
    }

    /** Só em memória (testes, ou quando não se quer guardar nada no computador). */
    static TokenStore memory() {
        return new TokenStore() {
            private String token;

            @Override
            public String load() {
                return token;
            }

            @Override
            public void save(String value) {
                token = value;
            }

            @Override
            public void clear() {
                token = null;
            }
        };
    }
}
