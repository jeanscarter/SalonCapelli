package app.menu;

import app.system.FormManager;
import java.awt.Component;
import java.awt.Image;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.*;
import raven.modal.drawer.item.Item;
import raven.modal.drawer.item.MenuItem;
import raven.modal.drawer.menu.MenuAction;
import raven.modal.drawer.menu.MenuEvent;
import raven.modal.drawer.menu.MenuOption;
import raven.modal.drawer.simple.SimpleDrawerBuilder;
import raven.modal.drawer.simple.footer.SimpleFooterData;
import raven.modal.drawer.simple.header.SimpleHeaderData;

public class MyDrawerBuilder extends SimpleDrawerBuilder {

    /**
     * Constructor simplificado.
     * Pasamos el FormManager directamente al método estático 'createMenuOption'.
     * Esto evita tener que usar getOption() y previene el ClassCastException.
     */
    public MyDrawerBuilder(FormManager formManager) {
        super(createMenuOption(formManager));
    }

    @Override
    public SimpleHeaderData getSimpleHeaderData() {
        Icon headerIcon = null;
        try {
            URL imgURL = getClass().getResource("/images/CapelliPng.png");
            if (imgURL != null) {
                ImageIcon rawIcon = new ImageIcon(imgURL);
                Image scaledImage = rawIcon.getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH);
                headerIcon = new ImageIcon(scaledImage);
            }
        } catch (Exception e) {
            // Log silencioso
        }
        
        return new SimpleHeaderData()
                .setIcon(headerIcon) 
                .setTitle("Salón de Belleza Capelli")
                .setDescription("Dashboard v1.0");
    }

    @Override
    public SimpleFooterData getSimpleFooterData() {
        return new SimpleFooterData()
                .setTitle("Usuario Activo")
                .setDescription("Administrador");
    }

    /**
     * Método estático que construye el menú Y configura la navegación.
     * Recibe 'formManager' para poder conectar los eventos sin usar 'this'.
     */
    private static MenuOption createMenuOption(FormManager formManager) {
        
        // 1. Definimos el Mapa de Navegación (Routing) aquí mismo
        Map<String, Supplier<Component>> navigationMap = new HashMap<>();
        navigationMap.put("Ventas por Día", () -> new JLabel("VISTA: Ventas por Día"));
        navigationMap.put("Reporte Semanal", () -> new JLabel("VISTA: Reporte Semanal"));
        navigationMap.put("Clientes", () -> new app.view.ClientesView());
        navigationMap.put("Trabajadoras", () -> new JLabel("VISTA: Gestión de Trabajadoras"));
        navigationMap.put("Servicios", () -> new JLabel("VISTA: Catálogo de Servicios"));
        navigationMap.put("Comisiones", () -> new JLabel("VISTA: Configuración Comisiones"));
        navigationMap.put("Calcular Nómina", () -> new JLabel("VISTA: Cálculo de Nómina"));
        navigationMap.put("Facturación", () -> new JLabel("VISTA: Facturación"));

        // 2. Definimos la estructura visual
        MenuItem[] items = new MenuItem[]{
            new Item.Label("REPORTES"),
            new Item("Ventas por Día"),
            new Item("Reporte Semanal"),
            
            new Item.Label("GESTIÓN"),
            new Item("Clientes"),
            new Item("Trabajadoras"),
            new Item("Servicios"),
            
            new Item.Label("FINANZAS"),
            new Item("Comisiones"),
            new Item("Calcular Nómina"),
            new Item("Facturación")
        };

        MenuOption option = new MenuOption();
        option.setMenus(items);

       // 3. Añadimos el evento usando el formManager
        option.addMenuEvent(new MenuEvent() {
            @Override
            public void selected(MenuAction action, int[] index) {
                String itemTitle = action.getItem().getName();
                
                // Lógica de enrutamiento
                Supplier<Component> viewFactory = navigationMap.get(itemTitle);
                
                if (viewFactory != null) {
                    // Si tenemos la pantalla mapeada, la mostramos
                    formManager.showForm(viewFactory.get());
                } else {
                    // CORREGIDO: Eliminamos la comprobación de 'instanceof Label'
                    // Si el evento se disparó, es porque es un ítem válido (Item).
                    // Si no tiene fábrica, mostramos el mensaje.
                    formManager.showToast("Módulo no implementado: " + itemTitle);
                }
            }
        });

        return option;
    }
}