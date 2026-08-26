package app.menu;

import app.model.Usuario;
import app.service.AuthService;
import app.system.FormManager;
import app.view.*;
import raven.modal.drawer.item.Item;
import raven.modal.drawer.item.MenuItem;
import raven.modal.drawer.menu.AbstractMenuElement;
import raven.modal.drawer.menu.MenuAction;
import raven.modal.drawer.menu.MenuEvent;
import raven.modal.drawer.menu.MenuOption;
import raven.modal.drawer.simple.SimpleDrawerBuilder;
import raven.modal.drawer.simple.footer.SimpleFooterData;
import raven.modal.drawer.simple.header.SimpleHeaderData;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class MyDrawerBuilder extends SimpleDrawerBuilder {

    private final FormManager formManager;
    private CustomDrawerFooter customFooter;
    private SimpleFooterData footerData;

    public MyDrawerBuilder(FormManager formManager) {
        super(createMenuOption(formManager));
        this.formManager = formManager;
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
    public AbstractMenuElement createFooter() {
        if (customFooter == null) {
            customFooter = new CustomDrawerFooter(formManager);
        }
        return customFooter;
    }

    @Override
    public SimpleFooterData getSimpleFooterData() {
        if (footerData == null) {
            footerData = new SimpleFooterData()
                    .setTitle("Usuario Activo")
                    .setDescription("Administrador");
        }
        return footerData;
    }

    public void updateUser(String username, String role) {
        if (customFooter != null) {
            customFooter.updateUser(username, role);
        }
    }

    private static MenuOption createMenuOption(FormManager formManager) {

        Map<String, Supplier<Component>> navigationMap = new HashMap<>();
        navigationMap.put("Dashboard", HomeView::new);
        navigationMap.put("Ventas por Día", ReporteDiarioView::new);
        navigationMap.put("Reporte Semanal", ReporteSemanalView::new);
        navigationMap.put("Clientes", ClientesView::new);
        navigationMap.put("Trabajadoras", TrabajadorasView::new);
        navigationMap.put("Servicios", ServiciosView::new);
        navigationMap.put("Comisiones", ComisionesView::new);
        navigationMap.put("Calcular Nómina", () -> new JLabel("VISTA: Cálculo de Nómina"));
        navigationMap.put("Facturación", VentaView::new);
        navigationMap.put("Cuentas por Cobrar", CuentasPorCobrarView::new);
        navigationMap.put("Usuarios", UsuariosView::new);
        navigationMap.put("Configuración del Sistema", ConfiguracionServiciosView::new);

        MenuItem[] items = new MenuItem[] {
                new Item("Dashboard"),
                
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
                new Item("Facturación"),
                new Item("Cuentas por Cobrar"),

                new Item.Label("ADMINISTRACIÓN"),
                new Item("Usuarios"),
                new Item("Configuración del Sistema")
        };

        MenuOption option = new MenuOption();
        option.setMenus(items);

        option.addMenuEvent(new MenuEvent() {
            @Override
            public void selected(MenuAction action, int[] index) {
                String itemTitle = action.getItem().getName();

                if ("Configuración del Sistema".equals(itemTitle)) {
                    Usuario u = AuthService.getCurrentUser();
                    if (u == null || !"jeanscarter".equalsIgnoreCase(u.getUsername())) {
                        formManager.showToast("Acceso restringido únicamente al usuario jeanscarter");
                        return;
                    }
                }

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