package com.robokey.fw.service;

import com.robokey.fw.model.Status;
import com.robokey.fw.model.EnviarSegredoRequest;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

@Service
public class RobokeyService {

    private boolean operacao = false;
    private Status statusAtual = Status.Espera;
    private AtomicInteger progresso = new AtomicInteger(0);

    public Status getStatus() {
        return statusAtual;
    }

    public void enviarSegredo(EnviarSegredoRequest request) {
        System.out.println("Modelo: " + request.getModeloChave());
        System.out.println("Pontos: " + request.getPontos());
        statusAtual = Status.Espera;
    }

    public void pausarMaquina() {
        operacao = false;
        statusAtual = Status.Pausada;
    }

    public void iniciar() {
        operacao = true;
        statusAtual = Status.Operacao;
        progresso.set(0); // Zera progresso
        iniciarProgresso(); // Inicia thread de progresso
    }

    public void retomar() {
        operacao = true;
        statusAtual = Status.Operacao;
        iniciarProgresso(); // reinicia progresso se pausado
    }

    public String progresso() {
        return progresso.get() + "%";
    }

    public void movChaveDireita() {
        System.out.println("Movendo chave para a direita");
    }

    public void movChaveEsquerda() {
        System.out.println("Movendo chave para a esquerda");
    }

    public void chaveInserida() {
        statusAtual = Status.ChaveON;
    }

    private void iniciarProgresso() {
        Thread progressoThread = new Thread(() -> {
            Random random = new Random();
            int tempoTotalMs = 3 * 60 * 1000; // 3 minutos
            int tempoDecorrido = 0;

            while (progresso.get() < 100 && operacao) {
                int incremento = 1 + random.nextInt(5); // 1 a 5%
                progresso.addAndGet(incremento);
                if (progresso.get() >= 100) {
                    progresso.set(100);
                    statusAtual = Status.Pronta;
                    break;
                }
                try {
                    int tempoEntrePassos = tempoTotalMs / (100 / incremento);
                    Thread.sleep(tempoEntrePassos);
                    tempoDecorrido += tempoEntrePassos;
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        progressoThread.setDaemon(true);
        progressoThread.start();
    }

}
