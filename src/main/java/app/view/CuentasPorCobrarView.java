package app.view;

import app.exception.DatabaseException;
import app.model.CuentaPorCobrar;
import app.repository.CuentaPorCobrarRepository;
import app.repository.CuentaPorCobrarRepositorySQLite;
import app.util.ToastNotification;
import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CuentasPorCobrarView extends JPanel {

    private final CuentaPorCobrarRepository repository;
    private JTable tblCxc;
    private DefaultTableModel tblModel;

    public CuentasPorCobrarView() {
        this.repository = new CuentaPorCobrarRepositorySQLite();
        init();
        cargarDatos();
    }

    private void init() {
        setLayout(new MigLayout("fill, insets 20", "[grow, fill]", "[][grow, fill][]"));
        
        JLabel lblTitle = new JLabel("Cuentas por Cobrar");
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +10");
        add(lblTitle, "wrap");

        tblModel = new DefaultTableModel(new String[]{
            "ID", "Cliente", "Factura", "Fecha", "Monto Original", "Monto Pendiente", "Estatus"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        tblCxc = new JTable(tblModel);
        tblCxc.setRowHeight(30);
        JScrollPane scroll = new JScrollPane(tblCxc);
        add(scroll, "wrap");

        JPanel pnlBotones = new JPanel(new MigLayout("insets 0", "[]push[]", ""));
        
        JButton btnActualizar = new JButton("Actualizar");
        btnActualizar.addActionListener(e -> cargarDatos());
        pnlBotones.add(btnActualizar);
        
        JButton btnAbonar = new JButton("Registrar Abono");
        btnAbonar.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor; foreground:#fff");
        btnAbonar.addActionListener(e -> registrarAbono());
        pnlBotones.add(btnAbonar);
        
        add(pnlBotones);
    }

    private void cargarDatos() {
        tblModel.setRowCount(0);
        try {
            List<CuentaPorCobrar> pendientes = repository.findPendientes();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            for (CuentaPorCobrar cxc : pendientes) {
                tblModel.addRow(new Object[]{
                    cxc.getId(),
                    cxc.getNombreCliente(),
                    cxc.getNumeroFactura(),
                    cxc.getFechaCreacion().format(dtf),
                    String.format("$%.2f", cxc.getMontoOriginal()),
                    String.format("$%.2f", cxc.getMontoPendiente()),
                    cxc.getEstatus()
                });
            }
        } catch (DatabaseException ex) {
            ToastNotification.showError(this, "Error", "No se pudieron cargar las CxC.");
        }
    }

    private void registrarAbono() {
        int row = tblCxc.getSelectedRow();
        if (row < 0) {
            ToastNotification.showWarning(this, "Seleccione una cuenta por cobrar.");
            return;
        }

        int id = (int) tblModel.getValueAt(row, 0);
        String cliente = (String) tblModel.getValueAt(row, 1);
        String pendienteStr = (String) tblModel.getValueAt(row, 5);
        
        String input = JOptionPane.showInputDialog(this, 
            "Cliente: " + cliente + "\nSaldo Pendiente: " + pendienteStr + "\nIngrese monto a abonar ($):");
            
        if (input != null && !input.trim().isEmpty()) {
            try {
                double abono = Double.parseDouble(input.replace(",", "."));
                if (abono <= 0) throw new NumberFormatException();
                
                CuentaPorCobrar cxc = repository.findById(id).orElse(null);
                if (cxc != null) {
                    if (abono > cxc.getMontoPendiente()) {
                        ToastNotification.showWarning(this, "El abono no puede superar el monto pendiente.");
                        return;
                    }
                    
                    cxc.setMontoPendiente(cxc.getMontoPendiente() - abono);
                    cxc.setFechaUltimoAbono(java.time.LocalDateTime.now());
                    
                    if (cxc.getMontoPendiente() <= 0.01) {
                        cxc.setEstatus("PAGADA");
                    } else {
                        cxc.setEstatus("PARCIAL");
                    }
                    
                    repository.update(cxc);
                    ToastNotification.showSuccess(this, "Abono Registrado", "Se abonó $" + abono);
                    cargarDatos();
                }
            } catch (NumberFormatException ex) {
                ToastNotification.showError(this, "Monto Inválido", "Ingrese un número válido mayor a 0.");
            } catch (DatabaseException ex) {
                ToastNotification.showError(this, "Error DB", "No se pudo registrar el abono.");
            }
        }
    }
}
