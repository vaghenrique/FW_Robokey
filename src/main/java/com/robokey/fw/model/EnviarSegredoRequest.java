package com.robokey.fw.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EnviarSegredoRequest(

    // Mapeia contour_points em ContourPoints
    @JsonProperty("contour_points")
    PontosContorno Contorno
)
{

}
