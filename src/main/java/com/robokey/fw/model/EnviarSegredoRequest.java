package com.robokey.fw.model;

import java.util.List;

public class EnviarSegredoRequest {
    private List<String> pontos;
    private String modeloChave;

    public List<String> getPontos() {
        return pontos;
    }

    public void setPontos(List<String> pontos) {
        this.pontos = pontos;
    }

    public String getModeloChave() {
        return modeloChave;
    }

    public void setModeloChave(String modeloChave) {
        this.modeloChave = modeloChave;
    }
}
