package br.com.yann.sextafeira.core.dto;

public class HistoricoMensagemDTO {

    private String role; // "user" ou "assistant"
    private String content;

    public HistoricoMensagemDTO() {}

    public HistoricoMensagemDTO(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public String role() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public String content() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
