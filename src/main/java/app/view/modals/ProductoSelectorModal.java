package app.view.modals;

import app.component.Modal;
import app.exception.DatabaseException;
import app.model.Marca;
import app.model.Producto;
import app.repository.MarcaRepository;
import app.repository.MarcaRepositorySQLite;
import app.repository.ProductoRepository;
import app.repository.ProductoRepositorySQLite;
import app.system.ModalManager;
import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Modal en 2 pasos:
 * 1. Selecciona la Marca
 * 2. Selecciona el Producto de esa marca
 */
public class ProductoSelectorModal extends Modal {

    private final Consumer<Producto> onSelected;
    private final MarcaRepository marcaRepository;
    private final ProductoRepository productoRepository;
    
    private JPanel container;
    
    public ProductoSelectorModal(Consumer<Producto> onSelected) {
        this.onSelected = onSelected;
        this.marcaRepository = new MarcaRepositorySQLite();
        this.productoRepository = new ProductoRepositorySQLite();
        init();
    }
    
    private void init() {
        setLayout(new BorderLayout());
        
        container = new JPanel(new BorderLayout());
        container.putClientProperty(FlatClientProperties.STYLE, "background:$Panel.background");
        add(container, BorderLayout.CENTER);
        
        showMarcasStep();
    }
    
    private void showMarcasStep() {
        container.removeAll();
        
        JPanel header = new JPanel(new MigLayout("insets 20", "[grow]", "[]"));
        JLabel title = new JLabel("Paso 1: Seleccione una Marca");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +5");
        header.add(title);
        
        JPanel body = new JPanel(new MigLayout("wrap 3, insets 20", "[fill, 120::][fill, 120::][fill, 120::]", "[]"));
        try {
            List<Marca> marcas = marcaRepository.findAll();
            for (Marca m : marcas) {
                JButton btn = new JButton(m.getNombre());
                btn.putClientProperty(FlatClientProperties.STYLE, "arc:15; margin:15,15,15,15; font:bold");
                btn.addActionListener(e -> showProductosStep(m));
                body.add(btn);
            }
            if (marcas.isEmpty()) {
                body.add(new JLabel("No hay marcas disponibles"));
            }
        } catch (DatabaseException e) {
            body.add(new JLabel("Error cargando marcas"));
        }
        
        container.add(header, BorderLayout.NORTH);
        container.add(new JScrollPane(body), BorderLayout.CENTER);
        
        JPanel footer = new JPanel(new MigLayout("insets 10, fillx", "[grow]", "[]"));
        JButton btnCancel = new JButton("Cancelar");
        btnCancel.addActionListener(e -> ModalManager.closeModal("producto_selector"));
        footer.add(btnCancel, "right");
        container.add(footer, BorderLayout.SOUTH);
        
        container.revalidate();
        container.repaint();
    }
    
    private void showProductosStep(Marca marca) {
        container.removeAll();
        
        JPanel header = new JPanel(new MigLayout("insets 20", "[grow][]", "[]"));
        JLabel title = new JLabel("Paso 2: Productos de " + marca.getNombre());
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +5");
        
        JButton btnBack = new JButton("Volver");
        btnBack.addActionListener(e -> showMarcasStep());
        
        header.add(title, "grow");
        header.add(btnBack);
        
        JPanel body = new JPanel(new MigLayout("wrap 1, insets 20, fillx", "[grow, fill]", "[]"));
        try {
            List<Producto> productos = productoRepository.findByMarca(marca.getId());
            for (Producto p : productos) {
                if (!p.isActivo() || p.getStockActual() <= 0) continue;
                
                JButton btn = new JButton(String.format("<html><b>%s</b><br>Stock: %d | Unidad: %s</html>", 
                        p.getNombre(), p.getStockActual(), p.getUnidadMedida()));
                btn.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:10,10,10,10; textAlignment:left");
                btn.addActionListener(e -> {
                    onSelected.accept(p);
                    ModalManager.closeModal("producto_selector");
                });
                body.add(btn);
            }
            if (body.getComponentCount() == 0) {
                body.add(new JLabel("No hay productos con stock para esta marca."));
            }
        } catch (DatabaseException e) {
            body.add(new JLabel("Error cargando productos"));
        }
        
        container.add(header, BorderLayout.NORTH);
        container.add(new JScrollPane(body), BorderLayout.CENTER);
        
        container.revalidate();
        container.repaint();
    }
}
