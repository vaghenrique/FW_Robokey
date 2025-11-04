package com.robokey.fw.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Ponto(double x, double y){
    /**
     * Feito pra criar cada um dos pontos que será enviado para o microcontrolador.
     */

}

