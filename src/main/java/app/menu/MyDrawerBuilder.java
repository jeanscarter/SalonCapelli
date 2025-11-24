package app.menu;

import app.system.FormManager;
import java.awt.Component;
import java.awt.Image;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.*;
import raven.modal.Drawer;
import raven.modal.drawer.item.Item;
import raven.modal.drawer.item.MenuItem;
import raven.modal.drawer.menu.MenuAction;
import raven.modal.drawer.menu.MenuEvent;
import raven.modal.drawer.menu.MenuOption;
import raven.modal.drawer.simple.SimpleDrawerBuilder;
import raven.modal.drawer.simple.footer.SimpleFooterData;
import raven.modal.drawer.simple.header.SimpleHeaderData;

public class MyDrawerBuilder extends SimpleDrawerBuilder {

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

    private static MenuOption createMenuOption(FormManager formManager) {
        
        Map<String, Supplier<Component>> navigationMap = new HashMap<>();
        navigationMap.put("Ventas por Día", () -> new JLabel("VISTA: Ventas por Día"));
        navigationMap.put("Reporte Semanal", () -> new JLabel("VISTA: Reporte Semanal"));
        navigationMap.put("Clientes", () -> new app.view.ClientesView());
        navigationMap.put("Trabajadoras", () -> new JLabel("VISTA: Gestión de Trabajadoras"));
        navigationMap.put("Servicios", () -> new JLabel("VISTA: Catálogo de Servicios"));
        navigationMap.put("Comisiones", () -> new JLabel("VISTA: Configuración Comisiones"));
        navigationMap.put("Calcular Nómina", () -> new JLabel("VISTA: Cálculo de Nómina"));
        navigationMap.put("Facturación", () -> new JLabel("VISTA: Facturación"));

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

        option.addMenuEvent(new MenuEvent() {
            @Override
            public void selected(MenuAction action, int[] index) {
                String itemTitle = action.getItem().getName();
                
                Supplier<Component> viewFactory = navigationMap.get(itemTitle);
                
                if (viewFactory != null) {
                    formManager.showForm(viewFactory.get());
                } else {
                    formManager.showToast("Módulo no implementado: " + itemTitle);
                }
            }
        });

        return option;
    }
}