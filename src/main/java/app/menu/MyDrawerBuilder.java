package app.menu;

import app.system.FormManager;
import java.awt.Image;
import java.net.URL;
import raven.modal.drawer.item.Item;
import raven.modal.drawer.item.MenuItem;
import raven.modal.drawer.menu.MenuAction;
import raven.modal.drawer.menu.MenuEvent;
import raven.modal.drawer.menu.MenuOption;
import raven.modal.drawer.simple.SimpleDrawerBuilder;
import raven.modal.drawer.simple.footer.SimpleFooterData;
import raven.modal.drawer.simple.header.SimpleHeaderData;

import javax.swing.*;

public class MyDrawerBuilder extends SimpleDrawerBuilder {

    public MyDrawerBuilder() {
        super(createMenuOption());
    }

    @Override
    public SimpleHeaderData getSimpleHeaderData() {
        // --- NUEVO: Cargar y redimensionar el logo para el Header ---
        Icon headerIcon = null;
        try {
            URL imgURL = getClass().getResource("/images/CapelliPng.png");
            if (imgURL != null) {
                // Cargar la imagen original
                ImageIcon rawIcon = new ImageIcon(imgURL);
                // Redimensionarla suavemente a 60x60 pixeles para que quepa bien
                Image scaledImage = rawIcon.getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH);
                // Crear el icono final
                headerIcon = new ImageIcon(scaledImage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Usamos el icono redimensionado aquí
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

    private static MenuOption createMenuOption() {
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
                switch (itemTitle) {
                    case "Ventas por Día":
                        FormManager.showForm(new JLabel("VISTA: Ventas por Día"));
                        break;
                    case "Reporte Semanal":
                        FormManager.showForm(new JLabel("VISTA: Reporte Semanal"));
                        break;
                    case "Clientes":
                        FormManager.showForm(new JLabel("VISTA: Gestión de Clientes"));
                        break;
                    case "Trabajadoras":
                        FormManager.showForm(new JLabel("VISTA: Gestión de Trabajadoras"));
                        break;
                    case "Servicios":
                        FormManager.showForm(new JLabel("VISTA: Catálogo de Servicios"));
                        break;
                    case "Comisiones":
                        FormManager.showForm(new JLabel("VISTA: Configuración Comisiones"));
                        break;
                    case "Calcular Nómina":
                        FormManager.showForm(new JLabel("VISTA: Cálculo de Nómina"));
                        break;
                    case "Facturación":
                        FormManager.showForm(new JLabel("VISTA: Facturación"));
                        break;
                }
            }
        });

        return option;
    }
}