package br.com.yann.sextafeira.dto;

import java.util.Map;

public class IaRouterResponse {

    private String intent;
    private Map<String, Object> entities;
    private String lang;

    public IaRouterResponse() {}

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public Map<String, Object> getEntities() { return entities; }
    public void setEntities(Map<String, Object> entities) { this.entities = entities; }

    public String getLang() { return lang; }
    public void setLang(String lang) { this.lang = lang; }
}
