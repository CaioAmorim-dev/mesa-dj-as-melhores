package br.com.cesar.dj;

public class Instrumentos {
    private String nome;
    private String som;
    private int bpm;
    private EstadoFaixa estado;
    private Thread thread;

    public Instrumentos(String nome, String som, int bpm) {
        this.nome = nome;
        this.bpm = bpm;
        this.som = som;
        this.estado = PARADO;
        
    }

    public void iniciar() {
        
    }
}
