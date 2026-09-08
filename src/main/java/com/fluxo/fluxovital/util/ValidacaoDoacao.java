package com.fluxo.fluxovital.util;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * Validações de negócio compartilhadas pelos 3 formulários de doação
 * (sangue, medula, leite) e pelo cadastro do doador.
 *
 * Isso existia SOMENTE como CHECK/trigger no banco (sqlCodigos/09_...sql),
 * o que tem dois problemas: (1) o script não roda sozinho — precisa ser
 * executado manualmente no MySQL — e (2) quando o banco rejeita, a
 * aplicação não tinha tratamento nenhum e caía numa tela de erro genérica.
 *
 * Movendo a validação pra cá, ela funciona independente do banco ter sido
 * configurado ou não, e o erro chega ao usuário como mensagem amigável.
 * O banco continua com as mesmas regras como segunda camada de defesa
 * (ver sqlCodigos/10_correcoes_validacao.sql).
 */
public final class ValidacaoDoacao {

    private ValidacaoDoacao() {}

    public static final double IMC_MINIMO = 18.5;

    /** RG é opcional, mas se for informado tem que ter exatamente 12 dígitos numéricos. */
    public static String validarRg(String rg) {
        if (rg == null || rg.isBlank()) return null;
        String limpo = rg.replaceAll("\\D", "");
        if (!limpo.matches("\\d{12}")) {
            return "RG inválido: informe exatamente 12 números (ou deixe o campo em branco).";
        }
        return null;
    }

    /** CPF: 11 dígitos numéricos. */
    public static String validarCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) return "CPF é obrigatório.";
        String limpo = cpf.replaceAll("\\D", "");
        if (!limpo.matches("\\d{11}")) {
            return "CPF inválido: informe 11 números.";
        }
        return null;
    }

    /** Idade calculada a partir da data de nascimento, dentro da faixa [min, max] anos. */
    public static String validarIdade(LocalDate nascimento, int minAnos, int maxAnos, String rotulo) {
        if (nascimento == null) return "Informe a data de nascimento.";
        if (nascimento.isAfter(LocalDate.now())) return "Data de nascimento não pode ser no futuro.";
        int idade = Period.between(nascimento, LocalDate.now()).getYears();
        if (idade < minAnos || idade > maxAnos) {
            return String.format("%s: a idade deve estar entre %d e %d anos (idade informada: %d).",
                    rotulo, minAnos, maxAnos, idade);
        }
        return null;
    }

    /** IMC = peso(kg) / altura(m)^2. Precisa ser >= 18.5 para ser considerado apto. */
    public static String validarImc(Double pesoKg, Double alturaCm) {
        if (pesoKg == null || alturaCm == null) return "Informe peso e altura.";
        if (pesoKg < 30 || pesoKg > 300) return "Peso informado parece inválido.";
        if (alturaCm < 100 || alturaCm > 250) return "Altura informada parece inválida (em centímetros).";
        double alturaM = alturaCm / 100.0;
        double imc = pesoKg / (alturaM * alturaM);
        if (imc < IMC_MINIMO) {
            return String.format(
                "IMC calculado (%.1f) está abaixo do mínimo considerado apto para doação (%.1f). " +
                "Peso e altura informados não atingem o IMC ideal.", imc, IMC_MINIMO);
        }
        return null;
    }

    /**
     * Doador já logado: o CPF digitado neste formulário tem que ser o
     * mesmo já registrado para essa conta (se ainda não tiver CPF salvo,
     * este é o que fica valendo dali pra frente).
     */
    public static String validarConsistenciaCpf(String cpfConta, String cpfDigitado) {
        if (cpfConta == null || cpfConta.isBlank()) return null; // primeira vez, nada a comparar
        String limpoConta = cpfConta.replaceAll("\\D", "");
        String limpoDigitado = cpfDigitado == null ? "" : cpfDigitado.replaceAll("\\D", "");
        if (!limpoConta.equals(limpoDigitado)) {
            return "O CPF informado é diferente do CPF já cadastrado na sua conta. " +
                   "Use sempre o mesmo CPF em todos os formulários.";
        }
        return null;
    }

    /** Mesma ideia, mas para e-mail (que já é definido no cadastro do doador). */
    public static String validarConsistenciaEmail(String emailConta, String emailDigitado) {
        if (emailConta == null || emailConta.isBlank()) return null;
        if (emailDigitado == null || !emailConta.trim().equalsIgnoreCase(emailDigitado.trim())) {
            return "O e-mail informado é diferente do e-mail cadastrado na sua conta. " +
                   "Use sempre o mesmo e-mail em todos os formulários.";
        }
        return null;
    }

    /** Junta várias validações, descartando nulos, pra montar a mensagem de erro final. */
    public static List<String> coletarErros(String... resultados) {
        List<String> erros = new ArrayList<>();
        for (String r : resultados) {
            if (r != null) erros.add(r);
        }
        return erros;
    }
}
