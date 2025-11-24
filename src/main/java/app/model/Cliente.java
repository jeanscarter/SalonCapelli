package app.model;

import java.time.LocalDate;

public class Cliente {

    private int id; // Identificador único interno
    private String cedula;
    private String nombreCompleto;
    private String telefono;
    private String direccion;
    
    private TipoCabello tipoCabello;
    private String tipoExtensiones; // Opcional, solo si aplica
    
    private LocalDate fechaCumpleanos;
    private LocalDate fechaUltimoTinte;
    private LocalDate fechaUltimoQuimico;
    private LocalDate fechaUltimaKeratina;
    private LocalDate fechaUltimoMantenimiento;

    public Cliente() {
    }

    public Cliente(int id, String cedula, String nombreCompleto, String telefono, String direccion, TipoCabello tipoCabello, String tipoExtensiones, LocalDate fechaCumpleanos, LocalDate fechaUltimoTinte, LocalDate fechaUltimoQuimico, LocalDate fechaUltimaKeratina, LocalDate fechaUltimoMantenimiento) {
        this.id = id;
        this.cedula = cedula;
        this.nombreCompleto = nombreCompleto;
        this.telefono = telefono;
        this.direccion = direccion;
        this.tipoCabello = tipoCabello;
        this.tipoExtensiones = tipoExtensiones;
        this.fechaCumpleanos = fechaCumpleanos;
        this.fechaUltimoTinte = fechaUltimoTinte;
        this.fechaUltimoQuimico = fechaUltimoQuimico;
        this.fechaUltimaKeratina = fechaUltimaKeratina;
        this.fechaUltimoMantenimiento = fechaUltimoMantenimiento;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public TipoCabello getTipoCabello() { return tipoCabello; }
    public void setTipoCabello(TipoCabello tipoCabello) { this.tipoCabello = tipoCabello; }

    public String getTipoExtensiones() { return tipoExtensiones; }
    public void setTipoExtensiones(String tipoExtensiones) { this.tipoExtensiones = tipoExtensiones; }

    public LocalDate getFechaCumpleanos() { return fechaCumpleanos; }
    public void setFechaCumpleanos(LocalDate fechaCumpleanos) { this.fechaCumpleanos = fechaCumpleanos; }

    public LocalDate getFechaUltimoTinte() { return fechaUltimoTinte; }
    public void setFechaUltimoTinte(LocalDate fechaUltimoTinte) { this.fechaUltimoTinte = fechaUltimoTinte; }

    public LocalDate getFechaUltimoQuimico() { return fechaUltimoQuimico; }
    public void setFechaUltimoQuimico(LocalDate fechaUltimoQuimico) { this.fechaUltimoQuimico = fechaUltimoQuimico; }

    public LocalDate getFechaUltimaKeratina() { return fechaUltimaKeratina; }
    public void setFechaUltimaKeratina(LocalDate fechaUltimaKeratina) { this.fechaUltimaKeratina = fechaUltimaKeratina; }

    public LocalDate getFechaUltimoMantenimiento() { return fechaUltimoMantenimiento; }
    public void setFechaUltimoMantenimiento(LocalDate fechaUltimoMantenimiento) { this.fechaUltimoMantenimiento = fechaUltimoMantenimiento; }
    
    // Método helper para verificar si es cumpleaños hoy (opcional pero útil para el dashboard)
    public boolean esCumpleanosHoy() {
        if (fechaCumpleanos == null) return false;
        LocalDate hoy = LocalDate.now();
        return hoy.getMonth() == fechaCumpleanos.getMonth() && hoy.getDayOfMonth() == fechaCumpleanos.getDayOfMonth();
    }
}