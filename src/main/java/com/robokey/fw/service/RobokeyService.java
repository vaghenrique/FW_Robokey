package com.robokey.fw.service;

import com.robokey.fw.model.Status;
import com.robokey.fw.model.EnviarSegredoRequest;
import com.robokey.fw.model.Ponto;
import com.robokey.fw.model.RespostaProgresso;

import org.springframework.scheduling.annotation.Scheduled;
import com.fazecast.jSerialComm.SerialPort;
import jakarta.annotation.PreDestroy;
import java.io.OutputStream;
import java.rmi.server.ExportException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.util.Locale;
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

    private final Object serialLock = new Object();

    private volatile String ultimoERRO = null;

    private volatile boolean respostaOkRecebida = false;
    private volatile boolean respostaErrorRecebida = false;

    // Esses identificadores devem garantir que qualquer placa com STM32 seja detectada pela USB
    // no entanto não basta detectar a placa, para estabelecer conexão o protocolo deve ser implementado.
    private static final int PLACA_VID = 1155;  // Vendor ID da placa do STM32
    private static final int PLACA_PID = 22336; // Product ID da placa do STM32
    private static final int baud_rate = 115200;

    private static final int SERIAL_READ_TIEMEOUT_MS = 1000;
    private static final int COMMAND_TIMEOUT_S = 5;

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
                portaSerial.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, SERIAL_READ_TIEMEOUT_MS, 0);

                if(portaSerial.openPort())
                {
                    System.out.println("SUCESSO: Conectado ao controlador!");
                    this.statusAtual = Status.Espera;
                    this.saidaSerial = portaSerial.getOutputStream();
                    this.entradaSerial = portaSerial.getInputStream();

                    iniciarThreadDeLeitura();
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

    private void iniciarThreadDeLeitura()
    {
        Thread threadOuvinte = new Thread( () ->{
            System.out.println("Thread de Leitura: Iniciada");
            BufferedReader reader = new BufferedReader(new InputStreamReader(this.entradaSerial));
            while(portaSerial!= null && portaSerial.isOpen())
            {
                String linha = null;
                try{
                    linha = reader.readLine();
                }
                catch(InterruptedIOException timeout)
                {
                    continue;
                }
                catch(IOException e)
                {
                    System.out.println("Thread de Leitura: Erro de IO: "+e.getMessage());
                    break;
                }
                if(linha != null)
                {
                    interpretadorDeLinhaUSB(linha.trim());
                }
            }

            System.out.println("Thread de Leitura: Morreu");
            synchronized(serialLock){
                respostaErrorRecebida = true;
                serialLock.notifyAll();
            }
        });

        threadOuvinte.setDaemon(true);
        threadOuvinte.start();
    }

    private void interpretadorDeLinhaUSB(String linha)
    {
        System.out.println("<< "+linha);

        if(linha.contains("OK:"))
        {
            synchronized(serialLock){
                respostaOkRecebida = true;
                serialLock.notify();
            }
            return;
        }

        if(linha.contains("ERROR:"))
        {
            synchronized(serialLock)
            {
                respostaErrorRecebida = true;
                serialLock.notify();
            }
        }

        if(linha.startsWith("PROG:"))
        {
            try{
                int p = Integer.parseInt(linha.substring(6).trim());
                this.progresso.set(p);
            }
            catch(Exception e)
            {
                System.err.println("Erro ao interpretar o progresso: "+e);
            }

        }

        if(linha.startsWith("ERROR:"))
        {
            try{
                String msgErro = linha.substring(7).trim();
                this.ultimoERRO =msgErro;
                System.err.println("ERRO ASSINCRONO DA PLACA: "+msgErro);
                /**
                 * @todo : Mudar modo ?
                 */
            }
            catch(Exception e)
            {
                System.err.println("Erro ao fazer parse da mensagem de erro: " + e.getMessage());
            }
        }

    }

    public Status getStatus() {
        return statusAtual;
    }

    public void enviarSegredo(EnviarSegredoRequest request) {
        if(!isPronto())
        {
            System.err.println("Comando: 'enviarSegredo' ignorado: Placa não conectada");
            return;
        }

        if(request.Contorno() == null || request.Contorno().topEdge() == null)
        {
            System.err.println("JSON inválido. 'contour_points' ou 'top_edge' não encontrados.");
            return;
        }

        //Copia lista de pontos para a thread, isso libera as requisições HTTP
        final var pontosParaEnviar = request.Contorno().topEdge();

        Thread envioThread = new Thread( () -> {
            boolean sucesso = executarProtocoloDeEnvio(pontosParaEnviar);
            if(sucesso)
                System.out.println("Protocolo de envio de pontos terminado com SUCESSO.");
            else 
                System.out.println("Protocolo de envio de pontos falhou");

        });

        envioThread.setDaemon(true);
        envioThread.start();

        statusAtual = Status.Espera;
    }

    private boolean executarProtocoloDeEnvio(java.util.List<Ponto> pontos)
    {
        if(!enviarEEsperarOk("START RECEIVING_CUT_PATH"))return false;
        int i=0;
        for(Ponto p : pontos){
            i++;
            String comandoPonto = String.format(Locale.US, "P %.4f %.4f", (double)p.x(), (double)p.y());
            if(!enviarEEsperarOk(comandoPonto))
            {
                System.err.println("Falha ao enviar o ponto" + i+ ".Abortando.");
                return false;
            }
        }

        if(!enviarEEsperarOk("STOP RECEIVING_CUT_PATH")) return false;

        return true;

    }


    private boolean enviarEEsperarOk(String comando)
    {
        if(portaSerial == null || saidaSerial == null || entradaSerial == null)
        {
            System.err.println("Porta serial não inicializada.");
            this.statusAtual = Status.ConectandoUSB;
            return false;
        }

        System.out.println(">> " + comando);

        synchronized(serialLock)
        {
            this.respostaErrorRecebida = false;
            this.respostaOkRecebida = false;

            try
            {
                // Limpa o buffer 
                while(entradaSerial.available() > 0)
                    entradaSerial.read();

                saidaSerial.write((comando + "\n").getBytes());
                saidaSerial.flush();

            }
            catch(Exception e)
            {
                System.err.println("Falha crítica ao ESCREVER na porta");
                e.printStackTrace(); // imprime o erro completo no console
                this.statusAtual = Status.ConectandoUSB;
                this.portaSerial = null;
                return false;
            }

            long tempoParaTerminar = System.currentTimeMillis()+(1000*COMMAND_TIMEOUT_S);

            try{
                while(!respostaOkRecebida && !respostaErrorRecebida)
                {
                    long tempoRestante = tempoParaTerminar- System.currentTimeMillis();
                    if(tempoRestante <= 0)
                    {
                        System.err.println("Timeout: OK não recebido");
                        return false;
                    }

                    System.out.println("...(aguardando OK da placa, T-" + tempoRestante + "ms)....");
                    serialLock.wait(tempoRestante);

                }

                if(respostaOkRecebida)
                    return true;
                if(respostaErrorRecebida)
                    return false;
            }catch(InterruptedException e)
            {
                System.err.println("Thread de envio interrompida durante a epsera: "+ e.getMessage());
                return false;
            }
        }
        return false;

    }

    public void pausarMaquina() {
        operacao = false;
        statusAtual = Status.Pausada;
    }

    public void iniciar() {
        this.ultimoERRO = null;
        this.progresso.set(0);

        operacao = true;
        boolean sucesso = enviarEEsperarOk("START PROCESSING");
        if(!sucesso)
        {
            System.err.println("Falha ao iniciar operação na placa.");
            operacao = false;
            statusAtual = Status.Espera;
            return;
        }
        statusAtual = Status.Operacao;
       // progresso.set(0);
        //iniciarProgresso();

    }

    public void retomar() {
        operacao = true;
        statusAtual = Status.Operacao;
        iniciarProgresso(); // reinicia progresso se pausado
    }

    public RespostaProgresso progresso() {
        String erro = this.ultimoERRO;
        this.ultimoERRO = null;
        return new RespostaProgresso(this.progresso.get(), erro);
        //return progresso.get() + "%";
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

    // so garante que ja conectou a USB
    private boolean isPronto()
    {
        return this.statusAtual != Status.ConectandoUSB;
    }

    // Limpa a porta ao desligar o servidor
    @PreDestroy
    public void desconectarDaPlaca()
    {
        if(portaSerial != null && portaSerial.isOpen())
        {
            portaSerial.closePort();
            System.out.println("Porta serial fechada.");
        }
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


