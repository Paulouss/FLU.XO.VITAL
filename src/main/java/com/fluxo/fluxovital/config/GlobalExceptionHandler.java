package com.fluxo.fluxovital.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Rede de segurança: se alguma regra só existir como CHECK/trigger no banco
 * (ex.: sqlCodigos/*.sql) e a validação em Java não tiver pego o problema
 * antes, o MySQL rejeita o INSERT/UPDATE e o Spring lança uma
 * DataIntegrityViolationException. Sem isso aqui, o usuário caía numa
 * página de erro genérica (Whitelabel Error Page). Agora ele volta pro
 * formulário com uma mensagem legível.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String tratarErroBanco(DataIntegrityViolationException ex,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {

        String mensagem = extrairMensagem(ex);
        redirectAttributes.addFlashAttribute("erro", mensagem);

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            return "redirect:" + referer;
        }
        return "redirect:/doador/home";
    }

    private String extrairMensagem(DataIntegrityViolationException ex) {
        String causa = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();

        if (causa != null) {
            // Triggers do MySQL (SIGNAL SQLSTATE '45000') colocam a mensagem
            // de negócio dentro do texto da exceção, geralmente depois de "MESSAGE_TEXT".
            int idx = causa.indexOf("MESSAGE_TEXT");
            if (idx >= 0) {
                String trecho = causa.substring(idx);
                int aspasInicio = trecho.indexOf('\'');
                int aspasFim = trecho.lastIndexOf('\'');
                if (aspasInicio >= 0 && aspasFim > aspasInicio) {
                    return trecho.substring(aspasInicio + 1, aspasFim);
                }
            }
        }
        return "Não foi possível salvar os dados: uma das regras de validação do sistema não foi atendida. " +
               "Revise as informações do formulário (RG, data de nascimento, CPF/e-mail) e tente novamente.";
    }
}
