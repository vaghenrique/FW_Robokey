package com.robokey.fw.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PontosContorno(
    /**
     * Quando a chave top_edge for encontrada no JSON,
     * os dados(i.e uma lista de pontos) serão colocados 
     * dentro da variável top_Edge
     */
    @JsonProperty("top_edge")
    List<Ponto> topEdge
    ) {
    
}
