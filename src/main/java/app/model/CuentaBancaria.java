package app.model;

/**
 * Modelo de cuenta bancaria asociada a una Trabajadora
 */
public class CuentaBancaria {

    private int id;
    private String banco;
    private String tipoDeCuenta;
    private String numeroDeCuenta;
    private boolean esPrincipal;

    public CuentaBancaria() {
    }

    public CuentaBancaria(String banco, String tipoDeCuenta, String numeroDeCuenta, boolean esPrincipal) {
        this.banco = banco;
        this.tipoDeCuenta = tipoDeCuenta;
        this.numeroDeCuenta = numeroDeCuenta;
        this.esPrincipal = esPrincipal;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getBanco() { return banco; }
    public void setBanco(String banco) { this.banco = banco; }

    public String getTipoDeCuenta() { return tipoDeCuenta; }
    public void setTipoDeCuenta(String tipoDeCuenta) { this.tipoDeCuenta = tipoDeCuenta; }

    public String getNumeroDeCuenta() { return numeroDeCuenta; }
    public void setNumeroDeCuenta(String numeroDeCuenta) { this.numeroDeCuenta = numeroDeCuenta; }

    public boolean isEsPrincipal() { return esPrincipal; }
    public void setEsPrincipal(boolean esPrincipal) { this.esPrincipal = esPrincipal; }

    @Override
    public String toString() {
        return numeroDeCuenta + " (" + banco + ")" + (esPrincipal ? " [Principal]" : "");
    }
}
