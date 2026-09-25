package app.util;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.Normalizer;

/**
 * Gestor de selección rápida por teclado para JComboBox.
 * Permite buscar y seleccionar elementos escribiendo letras rápidamente
 * (ej: "bel" -> Belkis, "day" -> Dayana, "sec" -> Secado, "ond" -> Ondas).
 * 
 * Características:
 * - Búsqueda incremental por buffer de caracteres (1.2s de expiración).
 * - Búsqueda por prefijo prioritario y fallback por coincidencia interna (substring).
 * - Normalización de acentos y caracteres especiales (ignora tildes y mayúsculas/minúsculas).
 * - Ciclado automático al presionar la misma tecla repetidamente.
 * - Soporte para borrar caracteres con Backspace.
 */
public class FastKeySelectionManager implements JComboBox.KeySelectionManager {

    private final JComboBox<?> comboBox;
    private final StringBuilder buffer = new StringBuilder();
    private long lastTime = 0;
    private static final long TIMEOUT_MS = 1200;

    public FastKeySelectionManager(JComboBox<?> comboBox) {
        this.comboBox = comboBox;
    }

    /**
     * Instala el gestor de selección rápida en un JComboBox.
     */
    public static void install(JComboBox<?> comboBox) {
        if (comboBox == null) return;
        FastKeySelectionManager manager = new FastKeySelectionManager(comboBox);
        comboBox.setKeySelectionManager(manager);

        comboBox.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    manager.handleBackspace();
                }
            }
        });
    }

    public void handleBackspace() {
        if (buffer.length() > 0) {
            buffer.deleteCharAt(buffer.length() - 1);
            if (buffer.length() > 0) {
                String query = normalize(buffer.toString());
                int idx = findMatch(query, comboBox.getModel(), 0);
                if (idx != -1) {
                    comboBox.setSelectedIndex(idx);
                }
            }
        }
    }

    @Override
    public int selectionForKey(char aKey, ComboBoxModel<?> aModel) {
        if (Character.isISOControl(aKey) || aKey == KeyEvent.CHAR_UNDEFINED) {
            return -1;
        }

        long now = System.currentTimeMillis();
        boolean timeoutExpired = (now - lastTime > TIMEOUT_MS);
        lastTime = now;

        char keyLower = Character.toLowerCase(aKey);

        // Caso especial: si es la misma tecla repetida con buffer de 1 caracter,
        // cicla al siguiente elemento que inicie con esa letra
        if (!timeoutExpired && buffer.length() == 1 && buffer.charAt(0) == keyLower) {
            int current = comboBox.getSelectedIndex();
            int next = findNextPrefix(String.valueOf(keyLower), aModel, current + 1);
            if (next != -1) {
                return next;
            }
            // Si llegó al final, buscar desde el principio
            next = findNextPrefix(String.valueOf(keyLower), aModel, 0);
            if (next != -1) {
                return next;
            }
            return current;
        }

        if (timeoutExpired) {
            buffer.setLength(0);
        }

        buffer.append(keyLower);
        String query = normalize(buffer.toString());

        // 1. Intentar coincidencia por prefijo completo
        int match = findNextPrefix(query, aModel, 0);
        if (match != -1) {
            return match;
        }

        // 2. Intentar coincidencia por contención (substring)
        match = findNextContains(query, aModel, 0);
        if (match != -1) {
            return match;
        }

        // 3. Si no hay coincidencia con el buffer acumulado, reiniciar con solo la tecla actual
        if (buffer.length() > 1) {
            buffer.setLength(0);
            buffer.append(keyLower);
            query = normalize(buffer.toString());

            match = findNextPrefix(query, aModel, 0);
            if (match != -1) return match;

            match = findNextContains(query, aModel, 0);
            if (match != -1) return match;
        }

        return -1;
    }

    private int findMatch(String query, ComboBoxModel<?> model, int startIndex) {
        int idx = findNextPrefix(query, model, startIndex);
        if (idx != -1) return idx;
        return findNextContains(query, model, startIndex);
    }

    private int findNextPrefix(String query, ComboBoxModel<?> model, int startIndex) {
        int size = model.getSize();
        for (int i = startIndex; i < size; i++) {
            Object elem = model.getElementAt(i);
            if (elem != null) {
                String text = normalize(elem.toString());
                if (text.startsWith(query)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int findNextContains(String query, ComboBoxModel<?> model, int startIndex) {
        int size = model.getSize();
        for (int i = startIndex; i < size; i++) {
            Object elem = model.getElementAt(i);
            if (elem != null) {
                String text = normalize(elem.toString());
                if (text.contains(query)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static String normalize(String str) {
        if (str == null) return "";
        String nfd = Normalizer.normalize(str.toLowerCase().trim(), Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{M}", "");
    }
}
