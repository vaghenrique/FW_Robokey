package com.robokey.fw.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RespostaProgresso(
    int progresso,
    String ultimoErro
) {
}
