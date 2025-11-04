package com.robokey.fw.service;

import com.robokey.fw.model.Status;
import com.robokey.fw.model.EnviarSegredoRequest;

import org.springframework.scheduling.annotation.Scheduled;
import com.fazecast.jSerialComm.SerialPort;
import jakarta.annotation.PreDestroy;
import java.io.OutputStream;
import java.io.InputStream;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

@Service
public class RobokeyService {

    private boolean operacao = false;
    private Status statusAtual = Status.ConectandoUSB;
    private AtomicInteger progresso = new AtomicInteger(0);

    private SerialPort portaSerial;
    private OutputStream saidaSerial;
    private InputStream entradaSerial;

    // Esses identificadores devem garantir que qualquer placa com STM32 seja detectada pela USB
    // no entanto não basta detectar a placa, para estabelecer conexão o protocolo deve ser implementado.
    private static final int PLACA_VID = 1155;  // Vendor ID da placa do STM32
    private static final int PLACA_PID = 22336; // Product ID da placa do STM32
    private static final int baud_rate = 115200;

    @Scheduled(fixedDelay = 3000)
    public void detectarPlacaUSB()
    {
        if(portaSerial != null && portaSerial.isOpen())
            return;

        this.statusAtual = Status.ConectandoUSB;
        this.portaSerial = null;

        for(SerialPort port : SerialPort.getCommPorts())
        {
            int vid = port.getVendorID();
            int pid = port.getProductID();

            if(vid == PLACA_VID && pid == PLACA_PID)
            {
                portaSerial = port;
                portaSerial.setBaudRate(baud_rate);

                if(portaSerial.openPort())
                {
                    System.out.println("SUCESSO: Conectado ao controlador!");
                    this.statusAtual = Status.Espera;
                    this.saidaSerial = portaSerial.getOutputStream();
                    break;
                }
                else
                {
                    System.err.println("Falha ao abrir a porta(talvez em uso ? ) tentando novamente em 3s");
                    this.portaSerial = null;
                }

            }

        }

        

    }

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
