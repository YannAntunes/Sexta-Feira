package br.com.yann.sextafeira.dto;

public class WhatsappOutgoingMessageDTO {

    private String to;       // número do usuário
    private String message;  // texto da SEXTA-FEIRA

    public WhatsappOutgoingMessageDTO() {
    }

    public WhatsappOutgoingMessageDTO(String to, String message) {
        this.to = to;
        this.message = message;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
