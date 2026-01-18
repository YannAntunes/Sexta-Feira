package br.com.yann.sextafeira.dto;

public class WhatsappIncomingMessageDTO {

    private String from;     // número do usuário
    private String message;  // texto enviado

    public WhatsappIncomingMessageDTO() {
    }

    public WhatsappIncomingMessageDTO(String from, String message) {
        this.from = from;
        this.message = message;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
