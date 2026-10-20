package com.turismo.frontend.modules;

import com.google.gson.JsonObject;
import com.turismo.frontend.ui.async.Task;
import com.turismo.frontend.ui.components.Toast;
import com.turismo.frontend.ui.components.UI;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignB;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;

import javax.swing.JOptionPane;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.turismo.frontend.modules.ColumnSpec.Type.DATE;
import static com.turismo.frontend.modules.ColumnSpec.Type.INTEGER;
import static com.turismo.frontend.modules.ColumnSpec.Type.MONEY;
import static com.turismo.frontend.modules.ColumnSpec.Type.RATING;
import static com.turismo.frontend.modules.ColumnSpec.Type.TEXT;

/**
 * Os 15 módulos da aplicação, um por tabela da base de dados.
 *
 * Os nomes dos campos são os do JSON da API (models do backend); as regras de
 * validação seguem tabelas.sql (tamanhos, obrigatórios, intervalos) e os
 * valores das listas vêm das constraints CHECK (/api/dominios).
 */
public final class Modules {

    private Modules() {
    }

    // -------------------------------------------- textos das opções (FK)

    static String turista(JsonObject r) {
        return Format.str(r, "nomeTurista") + " (" + Format.str(r, "idTurista") + ")";
    }

    static String destino(JsonObject r) {
        return Format.str(r, "nomeDestino") + " (" + Format.str(r, "idDestino") + ")";
    }

    static String alojamento(JsonObject r) {
        return Format.str(r, "nomeAlojamento") + " (" + Format.str(r, "idAlojamento") + ")";
    }

    static String operador(JsonObject r) {
        return Format.str(r, "nomeOperador") + " (" + Format.str(r, "idOperador") + ")";
    }

    static String transporte(JsonObject r) {
        return Format.str(r, "tipoTransporte") + ", " + Format.str(r, "capTransporte") + " lugares ("
                + Format.str(r, "idTransporte") + ")";
    }

    static String reserva(JsonObject r) {
        return Format.str(r, "idReserva") + " de " + Format.str(r, "nomeTurista") + ", chegada "
                + Format.date(Format.parseIsoDate(Format.str(r, "dataChegada")));
    }

    static String tipoAlojamento(JsonObject r) {
        return Format.str(r, "tipoAlojamento") + " (" + Format.str(r, "capAlojamento") + " pessoas)";
    }

    // -------------------------------------------------- tons de estado

    static UI.Tone reservaTone(String s) {
        switch (s) {
            case "PENDENTE": return UI.Tone.WARNING;
            case "CONFIRMADA": return UI.Tone.INFO;
            case "CONCLUIDA": return UI.Tone.SUCCESS;
            default: return UI.Tone.NEUTRAL;
        }
    }

    static UI.Tone pagamentoTone(String s) {
        switch (s) {
            case "CONCLUIDO": return UI.Tone.SUCCESS;
            case "FALHADO": return UI.Tone.DANGER;
            case "PENDENTE": return UI.Tone.WARNING;
            default: return UI.Tone.NEUTRAL;
        }
    }

    // ---------------------------------------------------------- módulos

    public static ModuleSpec destinos() {
        return new ModuleSpec("destinos", "Destinos", "destino", false, MaterialDesignM.MAP_MARKER_OUTLINE, "/destinos")
                .description("Locais turísticos, a sua região e a capacidade de visitantes.")
                .key("idDestino")
                .titleOf(r -> Format.str(r, "nomeDestino"))
                .column("idDestino", "Código", TEXT, 90)
                .column("nomeDestino", "Nome")
                .column("regiao", "Região", TEXT, 130)
                .column("capacidadeVisitantes", "Capacidade", INTEGER, 110)
                .column("totalAtracoes", "Atrações", INTEGER, 90)
                .column("totalAlojamentos", "Alojamentos", INTEGER, 110)
                .field(FieldSpec.text("nomeDestino", "Nome do destino", 80).required())
                .field(FieldSpec.domain("regiao", "Região", "DESTINO.REGIAO").required())
                .field(FieldSpec.integer("capacidadeVisitantes", "Capacidade de visitantes", 10).required()
                        .help("Número máximo de visitantes por dia (usado pela função Regista_Visita)."))
                .action(new ActionSpec("Ver ficha do destino", MaterialDesignF.FILE_DOCUMENT_OUTLINE,
                        "Texto das funções Retorna_Info_Destino e Lista_Atracoes_Destino", r -> true,
                        (page, row) -> showText(page, "/destinos/" + Format.str(row, "idDestino") + "/info",
                                "Ficha de " + Format.str(row, "nomeDestino"), "info", "atracoes")))
                .deleteWarning((api, row) -> {
                    JsonObject d = api.getObject("/destinos/" + Format.str(row, "idDestino") + "/dependencias");
                    int at = d.get("atracoes").getAsInt();
                    int al = d.get("alojamentos").getAsInt();
                    int vi = d.get("visitas").getAsInt();
                    if (at + al + vi == 0) {
                        return "O destino não tem atrações, alojamentos nem visitas associados. Esta ação não pode ser desfeita.";
                    }
                    return "O trigger deleteDestino apaga também " + Format.plural(at, "atração", "atrações") + ", "
                            + Format.plural(al, "alojamento", "alojamentos") + " (com as respetivas reservas e parcerias) e "
                            + Format.plural(vi, "visita", "visitas") + ". Esta ação não pode ser desfeita.";
                })
                .report("v_total_atracoes_destino", "Atrações por destino", "Quantas atrações tem cada destino (inclui os que têm zero).")
                .report("v_total_alojamentos_destino", "Alojamentos por destino", "Quantos alojamentos tem cada destino.");
    }

    public static ModuleSpec atracoes() {
        return new ModuleSpec("atracoes", "Atrações", "atração", true, MaterialDesignC.CAMERA_OUTLINE, "/atracoes")
                .description("Atrações de cada destino e os seus horários, incluindo horários especiais.")
                .key("idAtracao")
                .titleOf(r -> Format.str(r, "descricao") != null ? Format.str(r, "descricao") : Format.str(r, "tipoAtracao"))
                .column("idAtracao", "Código", TEXT, 90)
                .column("tipoAtracao", "Tipo", TEXT, 160)
                .column("descricao", "Descrição")
                .column("nomeDestino", "Destino", TEXT, 180)
                .column("horaAbertura", "Abre", TEXT, 70)
                .column("horaFecho", "Fecha", TEXT, 70)
                .detail("horaAberturaEspecial", "Abertura especial", TEXT)
                .detail("horaFechoEspecial", "Fecho especial", TEXT)
                .detail("detalhesHorarioEspecial", "Motivo do horário especial", TEXT)
                .field(FieldSpec.fk("idDestino", "Destino", "/destinos", "idDestino", Modules::destino).required())
                .field(FieldSpec.domain("tipoAtracao", "Tipo de atração", "ATRACAO.TIPO_ATRACAO").required())
                .field(FieldSpec.textArea("descricao", "Descrição", 200))
                .field(FieldSpec.time("horaAbertura", "Hora de abertura").required())
                .field(FieldSpec.time("horaFecho", "Hora de fecho").required())
                .field(FieldSpec.time("horaAberturaEspecial", "Abertura especial")
                        .help("Opcional. Se preencher o horário especial, indique as duas horas e o motivo."))
                .field(FieldSpec.time("horaFechoEspecial", "Fecho especial"))
                .field(FieldSpec.text("detalhesHorarioEspecial", "Motivo do horário especial", 200)
                        .help("Por exemplo, um feriado ou evento."))
                .crossValidation(Modules::validateAtracao)
                .report("v_atracoes_destino", "Atrações e horários", "Cada atração com o destino, a região e os horários.");
    }

    public static ModuleSpec alojamentos() {
        return new ModuleSpec("alojamentos", "Alojamentos", "alojamento", false, MaterialDesignB.BED_OUTLINE, "/alojamentos")
                .description("Hotéis, pousadas e outros alojamentos de cada destino.")
                .key("idAlojamento")
                .titleOf(r -> Format.str(r, "nomeAlojamento"))
                .column("idAlojamento", "Código", TEXT, 90)
                .column("nomeAlojamento", "Nome")
                .column("tipoAlojamento", "Tipo", TEXT, 110)
                .column("capAlojamento", "Capacidade", INTEGER, 100)
                .column("classAlojamento", "Classificação", RATING, 110)
                .column("mediaAvaliacoes", "Média das avaliações", RATING, 150)
                .column("nomeDestino", "Destino", TEXT, 180)
                .field(FieldSpec.text("nomeAlojamento", "Nome do alojamento", 50).required())
                .field(FieldSpec.fk("tipoAlojamento", "Tipo de alojamento", "/tipoAlojamentos", "tipoAlojamento",
                        Modules::tipoAlojamento).required())
                .field(FieldSpec.rating("classAlojamento", "Classificação"))
                .field(FieldSpec.fk("idDestino", "Destino", "/destinos", "idDestino", Modules::destino).required())
                .deleteWarning((api, row) -> "O trigger deleteAlojamento apaga também as associações deste alojamento a "
                        + "reservas e operadores. Esta ação não pode ser desfeita.")
                .report("v_alojamentos_detalhe", "Alojamentos em detalhe", "Tipo, capacidade, classificação, destino e região.")
                .report("v_media_avaliacao_alojamento", "Média das avaliações", "Média das avaliações recebidas nas reservas.")
                .report("v_alojamentos_acima_media", "Acima da média", "Alojamentos com classificação acima da média geral.");
    }

    public static ModuleSpec tiposAlojamento() {
        return new ModuleSpec("tiposAlojamento", "Tipos de alojamento", "tipo de alojamento", false,
                MaterialDesignS.SHAPE_OUTLINE, "/tipoAlojamentos")
                .description("Categorias de alojamento e o número de pessoas que cada uma acomoda.")
                .key("tipoAlojamento")
                .titleOf(r -> Format.str(r, "tipoAlojamento"))
                .column("tipoAlojamento", "Tipo")
                .column("capAlojamento", "Capacidade (pessoas)", INTEGER, 170)
                .column("totalAlojamentos", "Alojamentos deste tipo", INTEGER, 180)
                .field(FieldSpec.text("tipoAlojamento", "Nome do tipo", 11).required().createOnly()
                        .help("Até 11 caracteres. Não pode ser alterado depois, porque os alojamentos o referenciam."))
                .field(FieldSpec.integer("capAlojamento", "Capacidade (pessoas)", 2).required())
                .deleteWarning((api, row) -> "Só é possível eliminar um tipo que nenhum alojamento use. "
                        + "Esta ação não pode ser desfeita.");
    }

    public static ModuleSpec transportes() {
        return new ModuleSpec("transportes", "Transportes", "transporte", false, MaterialDesignB.BUS, "/transportes")
                .description("Meios de transporte, a sua capacidade e disponibilidade.")
                .key("idTransporte")
                .titleOf(r -> Format.str(r, "tipoTransporte") + " " + Format.str(r, "idTransporte"))
                .column("idTransporte", "Código", TEXT, 90)
                .column("tipoTransporte", "Tipo")
                .column("capTransporte", "Capacidade", INTEGER, 110)
                .status("disponibilidade", "Disponível", s -> "Sim".equals(s) ? UI.Tone.SUCCESS : UI.Tone.NEUTRAL)
                .column("totalOperadores", "Operadores", INTEGER, 110)
                .field(FieldSpec.text("tipoTransporte", "Tipo de transporte", 15).required()
                        .help("Por exemplo: Autocarro, Barco, Avioneta."))
                .field(FieldSpec.integer("capTransporte", "Capacidade (lugares)", 9).required())
                .field(FieldSpec.domain("disponibilidade", "Disponível", "TRANSPORTE.DISPONIBILIDADE").required()
                        .defaultValue("Sim"))
                .deleteWarning((api, row) -> "O trigger deleteTransporte apaga também as associações a operadores. "
                        + "Esta ação não pode ser desfeita.")
                .report("v_transportes_disponiveis", "Transportes disponíveis", "Só os transportes com disponibilidade 'Sim'.");
    }

    public static ModuleSpec turistas() {
        return new ModuleSpec("turistas", "Turistas", "turista", false, MaterialDesignA.ACCOUNT_OUTLINE, "/turistas")
                .description("Clientes da agência. O código é gerado pela função Insere_Turista.")
                .key("idTurista")
                .titleOf(r -> Format.str(r, "nomeTurista"))
                .column("idTurista", "Código", TEXT, 90)
                .column("nomeTurista", "Nome")
                .column("nacionalidade", "Nacionalidade", TEXT, 160)
                .column("totalReservas", "Reservas", INTEGER, 100)
                .field(FieldSpec.text("nomeTurista", "Nome completo", 80).required())
                .field(FieldSpec.text("nacionalidade", "Nacionalidade", 50).required()
                        .help("Por exemplo: Mocambicana, Portuguesa, Sul-Africana."))
                .deleteWarning((api, row) -> "A função Elimina_Turista recusa a eliminação se o turista tiver reservas. "
                        + "As preferências, visitas e associações a operadores do turista também são apagadas.")
                .report("v_turistas_sem_reserva", "Turistas sem reserva", "Turistas que ainda não fizeram nenhuma reserva.")
                .report("v_receita_nacionalidade", "Receita por nacionalidade", "Total pago (pagamentos concluídos) por nacionalidade.");
    }

    public static ModuleSpec preferencias() {
        return new ModuleSpec("preferencias", "Preferências de turistas", "preferência", true,
                MaterialDesignH.HEART_OUTLINE, "/turistaPreferencias")
                .description("Preferências de viagem de cada turista. Um turista pode ter várias.")
                .key("idTurista", "preferencia")
                .titleOf(r -> Format.str(r, "nomeTurista") + " prefere " + Format.str(r, "preferencia"))
                .derived("turista", "Turista", Modules::turista)
                .column("preferencia", "Preferência", TEXT, 180)
                .field(FieldSpec.fk("idTurista", "Turista", "/turistas", "idTurista", Modules::turista).required().createOnly())
                .field(FieldSpec.domain("preferencia", "Preferência", "TURISTA_PREFERENCIA.PREFERENCIA").required())
                .report("v_turista_preferencias", "Preferências por turista", "Cada turista com as suas preferências.");
    }

    public static ModuleSpec operadores() {
        return new ModuleSpec("operadores", "Operadores", "operador", false, MaterialDesignB.BRIEFCASE_OUTLINE, "/operadores")
                .description("Operadores turísticos, a sua especialidade e classificação.")
                .key("idOperador")
                .titleOf(r -> Format.str(r, "nomeOperador"))
                .column("idOperador", "Código", TEXT, 90)
                .column("nomeOperador", "Nome")
                .column("especialidade", "Especialidade", TEXT, 150)
                .column("classOperador", "Classificação", RATING, 110)
                .column("mediaAvaliacao", "Média das avaliações", RATING, 150)
                .column("totalTuristas", "Turistas", INTEGER, 90)
                .field(FieldSpec.text("nomeOperador", "Nome do operador", 25).required())
                .field(FieldSpec.text("especialidade", "Especialidade", 15).required()
                        .help("Por exemplo: Mergulho, Safari."))
                .field(FieldSpec.rating("classOperador", "Classificação"))
                .deleteWarning((api, row) -> "O trigger deleteOperador apaga também as associações a turistas, "
                        + "transportes e alojamentos. Esta ação não pode ser desfeita.")
                .report("v_avaliacao_operador", "Avaliação dos operadores", "Turistas acompanhados e média das avaliações.");
    }

    public static ModuleSpec reservas() {
        return new ModuleSpec("reservas", "Reservas", "reserva", true, MaterialDesignC.CALENDAR_CHECK_OUTLINE, "/reservas")
                .description("Reservas dos turistas, com o total pago e se já têm alojamento e pagamento.")
                .key("idReserva")
                .titleOf(r -> "Reserva de " + Format.str(r, "nomeTurista"))
                .column("idReserva", "Código", TEXT, 90)
                .column("nomeTurista", "Turista")
                .column("dataChegada", "Chegada", DATE, 110)
                .column("dataPartida", "Partida", DATE, 110)
                .column("noites", "Noites", INTEGER, 80)
                .status("statusReserva", "Estado", Modules::reservaTone)
                .column("totalPago", "Total pago", MONEY, 130)
                .status("estadoCompletude", "Completude", s -> "Completa".equals(s) ? UI.Tone.SUCCESS : UI.Tone.WARNING)
                .field(FieldSpec.fk("idTurista", "Turista", "/turistas", "idTurista", Modules::turista).required())
                .field(FieldSpec.date("dataChegada", "Data de chegada").required()
                        .help("Não pode ser anterior a hoje."))
                .field(FieldSpec.date("dataPartida", "Data de partida").required()
                        .help("Tem de ser depois da chegada."))
                .field(FieldSpec.domain("statusReserva", "Estado", "RESERVA.STATUS_RESERVA").required().defaultValue("PENDENTE"))
                .field(FieldSpec.multiFk("alojamentos", "Alojamentos", "/alojamentos", "idAlojamento", Modules::alojamento)
                        .createOnly()
                        .help("Opcional. São gravados com a reserva, na mesma transação."))
                .crossValidation(v -> {
                    Map<String, String> e = new LinkedHashMap<>();
                    LocalDate in = (LocalDate) v.get("dataChegada");
                    LocalDate out = (LocalDate) v.get("dataPartida");
                    if (in != null && out != null && !out.isAfter(in)) {
                        e.put("dataPartida", "Tem de ser depois da data de chegada.");
                    }
                    return e;
                })
                .action(new ActionSpec("Cancelar reserva", MaterialDesignC.CANCEL,
                        "Usa a procedure cancelar_reserva",
                        r -> List.of("PENDENTE", "CONFIRMADA").contains(Format.str(r, "statusReserva")),
                        (page, row) -> {
                            if (confirm(page, "Cancelar a reserva " + Format.str(row, "idReserva") + "?",
                                    "O estado passa a CANCELADA. A reserva e os pagamentos continuam registados.",
                                    "Cancelar reserva")) {
                                page.request(() -> page.lookups().api().post(
                                        "/reservas/" + Format.str(row, "idReserva") + "/cancelar", null),
                                        "Reserva cancelada.", row);
                            }
                        }))
                .deleteWarning((api, row) -> "O trigger deleteReserva apaga também os pagamentos e os alojamentos "
                        + "desta reserva. Esta ação não pode ser desfeita.")
                .report("v_reservas_detalhe", "Reservas em detalhe", "Turista, nacionalidade, datas, noites e estado.")
                .report("v_reservas_ativas", "Reservas ativas", "Só as reservas PENDENTE ou CONFIRMADA.")
                .report("v_total_pago_reserva", "Total pago por reserva", "Soma dos pagamentos concluídos de cada reserva.")
                .report("v_reservas_sem_alojamento", "Sem alojamento", "Verificação: reservas que ainda não têm alojamento.")
                .report("v_reservas_sem_pagamento", "Sem pagamento", "Verificação: reservas que ainda não têm pagamento.");
    }

    public static ModuleSpec pagamentos() {
        return new ModuleSpec("pagamentos", "Pagamentos", "pagamento", false, MaterialDesignC.CASH_MULTIPLE, "/pagamentos")
                .description("Pagamentos das reservas. O recibo é emitido pela função Concluir_Pagamento.")
                .key("idPagamento")
                .titleOf(r -> Format.money(Format.dec(r, "valor")) + " de " + Format.str(r, "nomeTurista"))
                .column("idPagamento", "Código", TEXT, 90)
                .column("idReserva", "Reserva", TEXT, 90)
                .column("nomeTurista", "Turista")
                .column("valor", "Valor", MONEY, 140)
                .column("dataPagamento", "Data", DATE, 110)
                .column("metodoPagamento", "Método", TEXT, 130)
                .status("statusPagamento", "Estado", Modules::pagamentoTone)
                .column("idRecibo", "Recibo", TEXT, 90)
                .detail("detalhesTransacao", "Detalhes da transação", TEXT)
                .field(FieldSpec.fk("idReserva", "Reserva", "/reservas", "idReserva", Modules::reserva).required())
                .field(FieldSpec.money("valor", "Valor").required())
                .field(FieldSpec.date("dataPagamento", "Data do pagamento").required()
                        .help("Não pode ser no futuro nem ter mais de 12 meses."))
                .field(FieldSpec.domain("metodoPagamento", "Método", "PAGAMENTO.METODO_PAGAMENTO").required())
                .field(FieldSpec.domain("statusPagamento", "Estado", "PAGAMENTO.STATUS_PAGAMENTO").required()
                        .exclude("CONCLUIDO").defaultValue("PENDENTE")
                        .help("Para concluir use a ação \"Concluir e emitir recibo\", que gera o recibo."))
                .field(FieldSpec.textArea("detalhesTransacao", "Detalhes da transação", 200))
                .editableWhen(r -> !"CONCLUIDO".equals(Format.str(r, "statusPagamento")),
                        "Um pagamento concluído já tem recibo e não pode ser alterado.")
                .crossValidation(v -> {
                    Map<String, String> e = new LinkedHashMap<>();
                    LocalDate d = (LocalDate) v.get("dataPagamento");
                    if (d != null && d.isAfter(LocalDate.now())) {
                        e.put("dataPagamento", "Não pode ser no futuro.");
                    } else if (d != null && d.isBefore(LocalDate.now().minusMonths(12))) {
                        e.put("dataPagamento", "Não pode ter mais de 12 meses.");
                    }
                    return e;
                })
                .action(new ActionSpec("Concluir e emitir recibo", MaterialDesignR.RECEIPT,
                        "Usa a função Concluir_Pagamento, que gera o número do recibo",
                        r -> "PENDENTE".equals(Format.str(r, "statusPagamento")),
                        (page, row) -> {
                            if (confirm(page, "Concluir o pagamento " + Format.str(row, "idPagamento") + "?",
                                    "O estado passa a CONCLUIDO e é gerado um recibo. Depois disso o pagamento não pode ser alterado.",
                                    "Concluir pagamento")) {
                                page.request(() -> page.lookups().api().post(
                                        "/pagamentos/" + Format.str(row, "idPagamento") + "/concluir", null),
                                        "Pagamento concluído.", row);
                            }
                        }))
                .action(new ActionSpec("Converter para dólares", MaterialDesignC.CURRENCY_USD,
                        "Usa a função Valor_Em_Dolar com a cotação que indicar", r -> true, Modules::convertToUsd))
                .deleteWarning((api, row) -> "CONCLUIDO".equals(Format.str(row, "statusPagamento"))
                        ? "Este pagamento já tem o recibo " + Format.str(row, "idRecibo") + ", que deixa de existir. "
                        + "Esta ação não pode ser desfeita."
                        : "Esta ação não pode ser desfeita.")
                .report("v_recibos", "Recibos emitidos", "Pagamentos que já têm recibo.")
                .report("v_receita_mensal", "Receita mensal", "Total recebido em cada mês (pagamentos concluídos).")
                .report("v_aud_pagamento_data_futura", "Pagamentos com data futura", "Verificação: não deve devolver linhas.")
                .report("v_aud_recibo_sem_concluido", "Recibos sem conclusão", "Verificação: recibos em pagamentos não concluídos.");
    }

    public static ModuleSpec visitas() {
        return new ModuleSpec("visitas", "Visitas a destinos", "visita", true, MaterialDesignM.MAP_MARKER_PATH, "/turistaDestinos")
                .description("Visitas registadas pela função Regista_Visita, que respeita a capacidade do destino.")
                .key("idDestino", "idTurista", "dataVisita")
                .titleOf(r -> Format.str(r, "nomeTurista") + " em " + Format.str(r, "nomeDestino"))
                .column("dataVisita", "Data da visita", DATE, 130)
                .derived("turista", "Turista", Modules::turista)
                .derived("destino", "Destino", Modules::destino)
                .field(FieldSpec.fk("idTurista", "Turista", "/turistas", "idTurista", Modules::turista).required().createOnly())
                .field(FieldSpec.fk("idDestino", "Destino", "/destinos", "idDestino", Modules::destino).required().createOnly())
                .field(FieldSpec.date("dataVisita", "Data da visita").required().createOnly())
                .deleteLabel("Remover")
                .report("v_turista_destinos", "Destinos visitados", "Que destinos cada turista visitou e quando.");
    }

    public static ModuleSpec reservaAlojamentos() {
        return new ModuleSpec("reservaAlojamentos", "Alojamentos das reservas", "associação", true,
                MaterialDesignH.HOME_CITY_OUTLINE, "/reservaAlojamentos")
                .description("Alojamentos escolhidos em cada reserva e a avaliação que receberam.")
                .key("idReserva", "idAlojamento")
                .titleOf(r -> Format.str(r, "nomeAlojamento") + " na reserva " + Format.str(r, "idReserva"))
                .column("idReserva", "Reserva", TEXT, 90)
                .column("nomeTurista", "Turista")
                .derived("alojamento", "Alojamento", Modules::alojamento)
                .column("avaliacaoAlojamento", "Avaliação", RATING, 100)
                .field(FieldSpec.fk("idReserva", "Reserva", "/reservas", "idReserva", Modules::reserva).required().createOnly())
                .field(FieldSpec.fk("idAlojamento", "Alojamento", "/alojamentos", "idAlojamento", Modules::alojamento)
                        .required().createOnly())
                .field(FieldSpec.rating("avaliacaoAlojamento", "Avaliação do alojamento").editOnly()
                        .help("De 0,0 a 5,0. Deixe vazio para retirar a avaliação (procedure avaliar_alojamento)."))
                .editLabel("Avaliar")
                .deleteLabel("Remover")
                .report("v_reservas_alojamento", "Alojamentos por reserva", "Reserva, turista, alojamento e avaliação.");
    }

    public static ModuleSpec operadorTuristas() {
        return new ModuleSpec("operadorTuristas", "Operadores e turistas", "associação", true,
                MaterialDesignA.ACCOUNT_TIE_OUTLINE, "/operadorTuristas")
                .description("Operadores que acompanham cada turista e a avaliação que receberam.")
                .key("idOperador", "idTurista")
                .titleOf(r -> Format.str(r, "nomeOperador") + " com " + Format.str(r, "nomeTurista"))
                .derived("operador", "Operador", Modules::operador)
                .derived("turista", "Turista", Modules::turista)
                .column("avaliacaoOperador", "Avaliação", RATING, 100)
                .field(FieldSpec.fk("idOperador", "Operador", "/operadores", "idOperador", Modules::operador).required().createOnly())
                .field(FieldSpec.fk("idTurista", "Turista", "/turistas", "idTurista", Modules::turista).required().createOnly())
                .field(FieldSpec.rating("avaliacaoOperador", "Avaliação do operador").editOnly()
                        .help("De 0,0 a 5,0. Deixe vazio para retirar a avaliação (procedure avaliar_operador)."))
                .editLabel("Avaliar")
                .deleteLabel("Remover")
                .report("v_avaliacao_operador", "Avaliação dos operadores", "Turistas acompanhados e média das avaliações.");
    }

    public static ModuleSpec operadorTransportes() {
        return new ModuleSpec("operadorTransportes", "Operadores e transportes", "associação", true,
                MaterialDesignB.BUS_MULTIPLE, "/operadorTransportes")
                .description("Transportes geridos por cada operador.")
                .key("idOperador", "idTransporte")
                .titleOf(r -> Format.str(r, "nomeOperador") + " com " + Format.str(r, "tipoTransporte"))
                .derived("operador", "Operador", Modules::operador)
                .column("idTransporte", "Transporte", TEXT, 110)
                .column("tipoTransporte", "Tipo", TEXT, 160)
                .field(FieldSpec.fk("idOperador", "Operador", "/operadores", "idOperador", Modules::operador).required().createOnly())
                .field(FieldSpec.fk("idTransporte", "Transporte", "/transportes", "idTransporte", Modules::transporte)
                        .required().createOnly())
                .deleteLabel("Remover")
                .report("v_operadores_transportes", "Transportes por operador", "Capacidade e disponibilidade de cada transporte.");
    }

    public static ModuleSpec operadorAlojamentos() {
        return new ModuleSpec("operadorAlojamentos", "Operadores e alojamentos", "associação", true,
                MaterialDesignL.LINK_VARIANT, "/operadorAlojamentos")
                .description("Alojamentos que cada operador pode vender.")
                .key("idOperador", "idAlojamento")
                .titleOf(r -> Format.str(r, "nomeOperador") + " com " + Format.str(r, "nomeAlojamento"))
                .derived("operador", "Operador", Modules::operador)
                .derived("alojamento", "Alojamento", Modules::alojamento)
                .field(FieldSpec.fk("idOperador", "Operador", "/operadores", "idOperador", Modules::operador).required().createOnly())
                .field(FieldSpec.fk("idAlojamento", "Alojamento", "/alojamentos", "idAlojamento", Modules::alojamento)
                        .required().createOnly())
                .deleteLabel("Remover")
                .report("v_operadores_alojamentos", "Alojamentos por operador", "Que alojamentos cada operador vende.");
    }

    /** Os 15 módulos, pela ordem da navegação. */
    public static List<ModuleSpec> all() {
        return List.of(destinos(), atracoes(), alojamentos(), tiposAlojamento(), transportes(),
                turistas(), preferencias(), operadores(),
                reservas(), pagamentos(),
                visitas(), reservaAlojamentos(), operadorTuristas(), operadorTransportes(), operadorAlojamentos());
    }

    // ------------------------------------------------------- utilitários

    static Map<String, String> validateAtracao(Map<String, Object> v) {
        Map<String, String> e = new LinkedHashMap<>();
        String abre = (String) v.get("horaAbertura");
        String fecha = (String) v.get("horaFecho");
        if (abre != null && fecha != null && fecha.compareTo(abre) <= 0) {
            e.put("horaFecho", "Tem de ser depois da hora de abertura.");
        }
        String abreEsp = (String) v.get("horaAberturaEspecial");
        String fechaEsp = (String) v.get("horaFechoEspecial");
        String motivo = (String) v.get("detalhesHorarioEspecial");
        boolean any = abreEsp != null || fechaEsp != null || motivo != null;
        if (any) {
            if (abreEsp == null) {
                e.put("horaAberturaEspecial", "Indique também a abertura especial.");
            }
            if (fechaEsp == null) {
                e.put("horaFechoEspecial", "Indique também o fecho especial.");
            }
            if (motivo == null) {
                e.put("detalhesHorarioEspecial", "Indique o motivo do horário especial.");
            }
            if (abreEsp != null && fechaEsp != null && fechaEsp.compareTo(abreEsp) <= 0) {
                e.put("horaFechoEspecial", "Tem de ser depois da abertura especial.");
            }
        }
        return e;
    }

    static boolean confirm(CrudPage page, String question, String detail, String confirmLabel) {
        Object[] options = {confirmLabel, "Voltar"};
        int choice = JOptionPane.showOptionDialog(page,
                "<html><b>" + CrudPage.escape(question) + "</b><br><br><div style='width:340px'>"
                        + CrudPage.escape(detail) + "</div></html>",
                confirmLabel, JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
        return choice == 0;
    }

    /** Mostra o texto devolvido por funções PL/SQL (ex.: ficha de um destino). */
    static void showText(CrudPage page, String path, String title, String... keys) {
        Task.run(() -> page.lookups().api().getObject(path), result -> {
            StringBuilder sb = new StringBuilder();
            for (String k : keys) {
                if (sb.length() > 0) {
                    sb.append("\n\n");
                }
                sb.append(Format.str(result, k));
            }
            javax.swing.JTextArea area = new javax.swing.JTextArea(sb.toString(), 10, 40);
            area.setEditable(false);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            JOptionPane.showMessageDialog(page, new javax.swing.JScrollPane(area), title, JOptionPane.PLAIN_MESSAGE);
        }, error -> Toast.error(page, error.getMessage()));
    }

    static void convertToUsd(CrudPage page, JsonObject row) {
        String input = (String) JOptionPane.showInputDialog(page,
                "Cotação (meticais por 1 dólar):", "Converter para dólares", JOptionPane.QUESTION_MESSAGE,
                null, null, null);
        if (input == null) {
            return;
        }
        BigDecimal cotacao;
        try {
            cotacao = new BigDecimal(input.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            Toast.error(page, "Cotação inválida: indique um número, por exemplo 64,5.");
            return;
        }
        if (cotacao.signum() <= 0) {
            Toast.error(page, "A cotação tem de ser maior que zero.");
            return;
        }
        String path = "/pagamentos/" + Format.str(row, "idPagamento") + "/dolar?cotacao=" + cotacao.toPlainString();
        Task.run(() -> page.lookups().api().getObject(path), result -> {
            BigDecimal usd = Format.dec(result, "valorUsd");
            JOptionPane.showMessageDialog(page,
                    Format.money(Format.dec(row, "valor")) + " correspondem a US$ " + Format.number(usd)
                            + "\n(cotação " + Format.number(cotacao) + " MT por dólar, calculado pela função Valor_Em_Dolar).",
                    "Valor em dólares", JOptionPane.INFORMATION_MESSAGE);
        }, error -> Toast.error(page, error.getMessage()));
    }

}
