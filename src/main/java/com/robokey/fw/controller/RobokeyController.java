package com.robokey.fw.controller;

import com.robokey.fw.model.EnviarSegredoRequest;
import com.robokey.fw.model.RespostaProgresso;
import com.robokey.fw.model.Status;
import com.robokey.fw.service.RobokeyService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class RobokeyController {

    private final RobokeyService service;

    public RobokeyController(RobokeyService service) {
        this.service = service;
    }

    @GetMapping("/status")  
    public Status status() {
        return service.getStatus();
    }

    @PostMapping("/enviarSegredo")
    public void enviarSegredo(@RequestBody EnviarSegredoRequest request) {
        service.enviarSegredo(request);
    }

    @PostMapping("/pausar")
    public void pausarMaquina() {
        service.pausarMaquina();
    }

    @PostMapping("/iniciar")
    public void iniciar() {
        service.iniciar();
    }

    @PostMapping("/lerSegredo")
    public void lerSegredo() {
        service.lerSegredo();
    }

    @PostMapping("/retomar")
    public void retomar() {
        service.retomar();
    }

    @GetMapping("/progresso")
    public RespostaProgresso progresso() {
        return service.progresso();
    }

    @GetMapping("/chaveInserida")
    public boolean chaveInserida() {
        return service.chaveInserida();
    }

    @PostMapping("/movGraus")
    public void girarGraus(@RequestParam double graus) {
        service.girarGraus(graus);
    }
}
