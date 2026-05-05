package app.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Modelo de Trabajadora del salón
 * Migrado de CapelliSalesWindow y adaptado al estándar SalonCapelli
 */
public class Trabajadora {

    private int id;
    private String cedula;          // Formato normalizado: V-12345678
    private String nombres;
    private String apellidos;
    private String telefono;
    private String correoElectronico;
    private byte[] foto;            // Almacenamiento binario (PNG)
    private List<CuentaBancaria> cuentas;

    // Campos de bono
    private boolean bonoActivo;
    private double montoBono;
    private String razonBono;

    public Trabajadora() {
        this.cuentas = new ArrayList<>();
        this.bonoActivo = false;
        this.montoBono = 0.0;
        this.razonBono = "";
    }

    // ===== Datos personales =====

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getNombreCompleto() {
        return (nombres != null ? nombres : "") + " " + (apellidos != null ? apellidos : "");
    }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getCorreoElectronico() { return correoElectronico; }
    public void setCorreoElectronico(String correoElectronico) { this.correoElectronico = correoElectronico; }

    public byte[] getFoto() { return foto; }
    public void setFoto(byte[] foto) { this.foto = foto; }

    // ===== Cuentas bancarias =====

    public List<CuentaBancaria> getCuentas() { return cuentas; }
    public void setCuentas(List<CuentaBancaria> cuentas) { this.cuentas = cuentas; }

    public Optional<CuentaBancaria> getCuentaPrincipal() {
        return cuentas.stream().filter(CuentaBancaria::isEsPrincipal).findFirst();
    }

    // ===== Bono =====

    public boolean isBonoActivo() { return bonoActivo; }
    public void setBonoActivo(boolean bonoActivo) { this.bonoActivo = bonoActivo; }

    public double getMontoBono() { return montoBono; }
    public void setMontoBono(double montoBono) { this.montoBono = montoBono; }

    public String getRazonBono() { return razonBono; }
    public void setRazonBono(String razonBono) { this.razonBono = razonBono; }
}
