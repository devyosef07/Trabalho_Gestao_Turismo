package com.turismo.frontend.auth;

import com.google.gson.JsonObject;
import com.turismo.frontend.api.ApiClient;
import com.turismo.frontend.api.ApiException;

import java.time.LocalDate;

/**
 * Registo, início e fim de sessão contra a API (/api/auth). A validação das
 * credenciais e o hash das palavras-passe são feitos no servidor; aqui só se
 * guarda o token da sessão e se configura o {@link ApiClient} para o enviar.
 *
 * Todos os métodos fazem pedidos HTTP: chamar fora da EDT.
 */
public class AuthService {

    /** Resultado do registo: o nome de utilizador é gerado pela API. */
    public record Registration(String username, String fullName) {
    }

    private final ApiClient api;
    private final TokenStore store;
    private volatile Session current;

    public AuthService(ApiClient api, TokenStore store) {
        this.api = api;
        this.store = store;
    }

    public ApiClient api() {
        return api;
    }

    public Session current() {
        return current;
    }

    public Registration register(String fullName, LocalDate birthDate, String password, String confirmation)
            throws ApiException {
        JsonObject body = new JsonObject();
        body.addProperty("nomeCompleto", AccountRules.normalizeName(fullName));
        body.addProperty("dataNascimento", birthDate == null ? null : birthDate.toString());
        body.addProperty("palavraPasse", password);
        body.addProperty("confirmacao", confirmation);
        JsonObject data = api.post("/auth/registo", body).getAsJsonObject("data");
        return new Registration(data.get("nomeUtilizador").getAsString(), data.get("nomeCompleto").getAsString());
    }

    public Session login(String username, String password, boolean remember) throws ApiException {
        JsonObject body = new JsonObject();
        body.addProperty("nomeUtilizador", username == null ? "" : username.trim());
        body.addProperty("palavraPasse", password);
        body.addProperty("manterSessao", remember);
        Session session = Session.fromJson(api.post("/auth/sessao", body), null);
        if (remember) {
            store.save(session.token());
        } else {
            store.clear();
        }
        activate(session);
        return session;
    }

    /**
     * Retoma a sessão guardada, se ainda for válida no servidor. Devolve null se
     * não houver token ou se a API o recusar (o token é então apagado). Se a API
     * estiver inacessível lança a exceção e mantém o token para outra tentativa.
     */
    public Session restore() throws ApiException {
        String token = store.load();
        if (token == null || token.isBlank()) {
            return null;
        }
        api.setToken(token);
        try {
            Session session = Session.fromJson(api.getObject("/auth/sessao"), token);
            activate(session);
            return session;
        } catch (ApiException e) {
            api.setToken(null);
            if (e.getStatus() == 401) {
                store.clear();
                return null;
            }
            throw e;
        }
    }

    /** Dados atualizados da conta da sessão em curso. */
    public Session refresh() throws ApiException {
        Session s = current;
        if (s == null) {
            throw new ApiException(401, "SESSAO_INVALIDA", "Não há sessão iniciada.", null, null);
        }
        Session fresh = Session.fromJson(api.getObject("/auth/sessao"), s.token());
        current = fresh;
        return fresh;
    }

    /**
     * Termina a sessão: apaga já o token local (a interface pode sair de
     * imediato) e depois revoga-o no servidor. Se a API não responder, o token
     * deixa de existir no computador e expira sozinho no servidor.
     *
     * @return true se o servidor confirmou o fim da sessão
     */
    public boolean logout() {
        Session s = current;
        current = null;
        store.clear();
        api.setToken(null);
        if (s == null) {
            return true;
        }
        ApiClient revoker = api.withToken(s.token());
        try {
            revoker.delete("/auth/sessao");
            return true;
        } catch (ApiException e) {
            return e.getStatus() == 401;
        }
    }

    /** Usa uma sessão já obtida (ex.: testes de interface com uma API falsa). */
    public void resume(Session session) {
        activate(session);
    }

    /** Esquece a sessão localmente (ex.: a API respondeu que expirou). */
    public void forget() {
        current = null;
        store.clear();
        api.setToken(null);
    }

    private void activate(Session session) {
        current = session;
        api.setToken(session.token());
    }
}
