-- =====================================================================
-- SALÓN CAPELLI - SCRIPT DE POBLACIÓN INICIAL DE DATOS (SEED DATA)
-- =====================================================================
-- Contiene la información extraída de la base de datos real y las reglas
-- de negocio (trabajadoras, cuentas bancarias, catálogo de servicios, precios
-- y comisiones detalladas) para poblar la base de datos sin lógica hardcoded en código.
-- =====================================================================

-- =====================================================================
-- 1. TRABAJADORAS
-- =====================================================================
INSERT OR IGNORE INTO trabajadoras (id, cedula, nombres, apellidos, telefono, correo, bono_activo, monto_bono, razon_bono, metodo_pago_preferido)
VALUES
(1, 'V-18522231', 'Dayana', 'Govea', '04127915851', NULL, 0, 0.0, '', 'BANCO'),
(2, 'V-31085005', 'Maria Virginia', 'Romero', '04143604499', NULL, 0, 0.0, '', 'BANCO'),
(3, 'V-5562378', 'Pascualina', 'Gutierrez', '04146638330', NULL, 0, 0.0, '', 'BANCO'),
(4, 'V-27683374', 'Aurora Sofia', 'Exposito', '04242092890', NULL, 0, 0.0, '', 'BANCO'),
(5, 'V-18921264', 'Jeimy', 'Añez', '04246695087', NULL, 0, 0.0, '', 'EFECTIVO'),
(6, 'V-9395233', 'Belkis', 'Gutierrez', '04146126300', NULL, 0, 0.0, '', 'BANCO'),
(7, 'V-24342800', 'Milagros', 'Gutierrez', '04246194365', NULL, 0, 0.0, '', 'EFECTIVO'),
(8, 'V-7774946', 'Maria', 'Diaz', '04246464683', NULL, 0, 0.0, '', 'BANCO'),
(9, 'V-9200133', 'Rosa Maria', 'Gutierrez', '04246889337', NULL, 0, 0.0, '', 'EFECTIVO'),
(10, 'V-24734839', 'Jaqueline', 'Añez', '04246703185', NULL, 0, 0.0, '', 'BANCO'),
(121, 'J-298312564', 'Salón', 'Capelli', '', NULL, 0, 0.0, '', 'BANCO');

-- =====================================================================
-- 2. CUENTAS BANCARIAS DE LAS TRABAJADORAS
-- =====================================================================
INSERT OR IGNORE INTO cuentas_bancarias (id, trabajadora_id, banco, tipo_cuenta, numero_cuenta, es_principal)
VALUES
(1, 1, 'Banco Provincial', 'Corriente', '01080511200100296908', 1),
(2, 2, 'Banesco', 'Corriente', '01340039330391056651', 1),
(3, 3, 'Banco Provincial', 'Ahorro', '01080086240200360744', 1),
(4, 4, 'Banesco', 'Corriente', '01340039330391055077', 1),
(5, 5, 'Banco Provincial', 'Corriente', '01080059550100393036', 1),
(6, 6, 'Banco Provincial', 'Corriente', '01080086260100175613', 1),
(7, 7, 'Banco Provincial', 'Ahorro', '01080302190200068019', 1),
(8, 9, 'Banesco', 'Corriente', '01340946380001307454', 1),
(9, 9, 'Banco Nacional de Crédito (BNC)', 'Corriente', '01160148150014749505', 0),
(10, 9, 'Bancamiga', 'Corriente', '01720112381125322600', 0),
(11, 9, 'Banesco', 'Corriente', '01340077650773172568', 0),
(12, 10, 'Banco Provincial', 'Corriente', '01080059500100533199', 1);

-- =====================================================================
-- 3. SERVICIOS Y PRECIOS
-- =====================================================================
INSERT OR IGNORE INTO servicios (id, nombre, categoria, precio_corto, precio_mediano, precio_largo, precio_extensiones, permite_cliente_producto, precio_cliente_producto, is_active)
VALUES
(1, 'Lavado', 'Lavado', 10.0, 0.0, 0.0, 0.0, 1, 8.0, 1),
(2, 'Secado', 'Peluqueria', 18.0, 20.0, 25.0, 30.0, 0, 0.0, 1),
(3, 'Ondas', 'Peluqueria', 15.0, 20.0, 35.0, 40.0, 0, 0.0, 1),
(4, 'Corte Puntas', 'Peluqueria', 20.0, 0.0, 0.0, 0.0, 0, 0.0, 1),
(5, 'Corte Elaborado', 'Peluqueria', 25.0, 0.0, 0.0, 0.0, 0, 0.0, 1),
(6, 'Maquillaje', 'Peluqueria', 60.0, 70.0, 80.0, 0.0, 0, 0.0, 1),
(7, 'Peinados', 'Peluqueria', 40.0, 50.0, 60.0, 0.0, 0, 0.0, 1),
(8, 'Color (Tinte)', 'Quimico', 40.0, 50.0, 0.0, 0.0, 1, 25.0, 1),
(9, 'Mechas', 'Quimico', 80.0, 100.0, 120.0, 150.0, 0, 0.0, 1),
(10, 'Cejas', 'Peluqueria', 5.0, 8.0, 0.0, 0.0, 0, 0.0, 1),
(11, 'Bozo', 'Peluqueria', 5.0, 8.0, 0.0, 0.0, 0, 0.0, 1),
(12, 'Manicure Tradicional', 'Manos/Pies', 12.0, 0.0, 0.0, 0.0, 0, 0.0, 1),
(13, 'Pedicure Tradicional', 'Manos/Pies', 12.0, 0.0, 0.0, 0.0, 0, 0.0, 1),
(14, 'Keratina', 'Quimico', 60.0, 120.0, 0.0, 0.0, 0, 0.0, 1),
(15, 'Hidratación solo', 'Quimico', 15.0, 20.0, 25.0, 0.0, 0, 0.0, 1),
(16, 'Hidratación Fusio-Dose', 'Quimico', 35.0, 0.0, 0.0, 0.0, 0, 0.0, 1),
(17, 'Secado con extensiones', 'Peluqueria', 25.0, 30.0, 45.0, 0.0, 0, 0.0, 1),
(18, 'Planchado', 'Peluqueria', 15.0, 20.0, 0.0, 0.0, 0, 0.0, 1),
(19, 'Pestañas', 'Peluqueria', 10.0, 0.0, 0.0, 0.0, 0, 0.0, 1),
(20, 'Manicure Gelish', 'Manos/Pies', 15.0, 0.0, 0.0, 0.0, 0, 0.0, 1),
(21, 'Manicure Rubber', 'Manos/Pies', 18.0, 0.0, 0.0, 0.0, 0, 0.0, 1),
(22, 'Manicure Polygel', 'Manos/Pies', 20.0, 0.0, 0.0, 0.0, 0, 0.0, 1),
(23, 'Pedicure Gelish', 'Manos/Pies', 15.0, 0.0, 0.0, 0.0, 0, 0.0, 1),
(24, 'Hidratación M/P', 'Manos/Pies', 15.0, 0.0, 0.0, 0.0, 0, 0.0, 1),
(25, 'Extensiones (Medio Paquete)', 'Extensiones', 60.0, 0.0, 0.0, 0.0, 0, 0.0, 1),
(26, 'Extensiones (1 Paquete)', 'Extensiones', 120.0, 0.0, 0.0, 0.0, 0, 0.0, 1),
(27, 'Extensiones (2 Paquetes)', 'Extensiones', 140.0, 0.0, 0.0, 0.0, 0, 0.0, 1),
(28, 'Extensiones (3 Paquetes)', 'Extensiones', 160.0, 0.0, 0.0, 0.0, 0, 0.0, 1),
(29, 'Extensiones (4 Paquetes)', 'Extensiones', 180.0, 0.0, 0.0, 0.0, 0, 0.0, 1),
(30, 'Productos', 'Otros', 35.0, 0.0, 0.0, 0.0, 0, 0.0, 1),
(31, 'Abono Manual Staff', 'PAGO-MANUAL', 0.0, 0.0, 0.0, 0.0, 0, 0.0, 1);

-- =====================================================================
-- 4. REGLAS DE COMISIÓN BÁSICAS (CATEGORÍA POR TRABAJADORA - LEGACY)
-- =====================================================================
INSERT OR IGNORE INTO reglas_comision (id, trabajadora_id, categoria_servicio, porcentaje_comision)
VALUES
(1, 8, 'Manos/Pies', 0.70),
(2, 10, 'Peluqueria', 0.50),
(3, 10, 'Quimico', 0.40),
(4, 1, 'Peluqueria', 0.50),
(5, 1, 'Quimico', 0.40),
(6, 2, 'Peluqueria', 0.50),
(7, 2, 'Quimico', 0.40),
(8, 6, 'Peluqueria', 0.65),
(9, 6, 'Quimico', 0.50),
(10, 4, 'Peluqueria', 0.60),
(11, 4, 'Quimico', 0.50),
(12, 5, 'Peluqueria', 0.60),
(13, 5, 'Quimico', 0.50),
(14, 3, 'Peluqueria', 0.60),
(15, 3, 'Quimico', 0.50),
(16, 7, 'Peluqueria', 0.60),
(17, 7, 'Quimico', 0.50);

-- =====================================================================
-- 5. REGLAS DE COMISIÓN DETALLADAS (MOTOR AVANZADO)
-- Reemplaza la lógica hardcoded que antes residía en PayrollService.java
-- =====================================================================
INSERT OR IGNORE INTO reglas_comision_detalladas 
(trabajadora_id, servicio_id, categoria_servicio, cliente_trae_producto, tipo_comision, valor_comision, precio_condicion, prioridad, activo, descripcion)
VALUES
-- 5.1 Reglas Generales de Categoría por Trabajadora (Prioridad 10)
(8, NULL, 'Manos/Pies', NULL, 'PORCENTAJE', 0.70, NULL, 10, 1, 'Maria Diaz - Manos/Pies 70%'),
(10, NULL, 'Peluqueria', NULL, 'PORCENTAJE', 0.50, NULL, 10, 1, 'Jaqueline Añez - Peluqueria 50%'),
(10, NULL, 'Quimico', NULL, 'PORCENTAJE', 0.40, NULL, 10, 1, 'Jaqueline Añez - Quimico 40%'),
(1, NULL, 'Peluqueria', NULL, 'PORCENTAJE', 0.50, NULL, 10, 1, 'Dayana Govea - Peluqueria 50%'),
(1, NULL, 'Quimico', NULL, 'PORCENTAJE', 0.40, NULL, 10, 1, 'Dayana Govea - Quimico 40%'),
(2, NULL, 'Peluqueria', NULL, 'PORCENTAJE', 0.50, NULL, 10, 1, 'Maria Virginia Romero - Peluqueria 50%'),
(2, NULL, 'Quimico', NULL, 'PORCENTAJE', 0.40, NULL, 10, 1, 'Maria Virginia Romero - Quimico 40%'),
(6, NULL, 'Peluqueria', NULL, 'PORCENTAJE', 0.65, NULL, 10, 1, 'Belkis Gutierrez - Peluqueria 65%'),
(6, NULL, 'Quimico', NULL, 'PORCENTAJE', 0.50, NULL, 10, 1, 'Belkis Gutierrez - Quimico 50%'),
(4, NULL, 'Peluqueria', NULL, 'PORCENTAJE', 0.60, NULL, 10, 1, 'Aurora Sofia Exposito - Peluqueria 60%'),
(4, NULL, 'Quimico', NULL, 'PORCENTAJE', 0.50, NULL, 10, 1, 'Aurora Sofia Exposito - Quimico 50%'),
(5, NULL, 'Peluqueria', NULL, 'PORCENTAJE', 0.60, NULL, 10, 1, 'Jeimy Añez - Peluqueria 60%'),
(5, NULL, 'Quimico', NULL, 'PORCENTAJE', 0.50, NULL, 10, 1, 'Jeimy Añez - Quimico 50%'),
(3, NULL, 'Peluqueria', NULL, 'PORCENTAJE', 0.60, NULL, 10, 1, 'Pascualina Gutierrez - Peluqueria 60%'),
(3, NULL, 'Quimico', NULL, 'PORCENTAJE', 0.50, NULL, 10, 1, 'Pascualina Gutierrez - Quimico 50%'),
(7, NULL, 'Peluqueria', NULL, 'PORCENTAJE', 0.60, NULL, 10, 1, 'Milagros Gutierrez - Peluqueria 60%'),
(7, NULL, 'Quimico', NULL, 'PORCENTAJE', 0.50, NULL, 10, 1, 'Milagros Gutierrez - Quimico 50%'),

-- 5.2 Caso Especial: PAGO-MANUAL (Prioridad 200)
(NULL, NULL, 'PAGO-MANUAL', NULL, 'PORCENTAJE', 1.00, NULL, 200, 1, 'Abono Manual Staff - 100% passthrough'),

-- 5.3 Reglas Globales: Color (Tinte - ID 8)
(NULL, 8, 'Quimico', 1, 'MONTO_FIJO', 12.50, NULL, 85, 1, 'Color (Tinte) - Cliente trae producto ($12.50 fijo)'),
(NULL, 8, 'Quimico', 0, 'PORCENTAJE', 0.25, NULL, 80, 1, 'Color (Tinte) - Salón pone producto (25%)'),
(NULL, 8, 'Quimico', NULL, 'PORCENTAJE', 0.25, NULL, 75, 1, 'Color (Tinte) - Fallback general (25%)'),

-- 5.4 Excepciones: Belkis Gutierrez (ID 6 - Prioridad 120)
(6, 9, 'Quimico', NULL, 'PORCENTAJE', 0.36, NULL, 120, 1, 'Belkis Gutierrez - Mechas 36%'),
(6, 14, 'Quimico', NULL, 'PORCENTAJE', 0.70, NULL, 120, 1, 'Belkis Gutierrez - Keratina 70%'),
(6, 10, 'Peluqueria', NULL, 'PORCENTAJE', 0.50, NULL, 120, 1, 'Belkis Gutierrez - Cejas 50%'),
(6, 11, 'Peluqueria', NULL, 'PORCENTAJE', 0.50, NULL, 120, 1, 'Belkis Gutierrez - Bozo 50%'),

-- 5.5 Excepciones: Jeimy Añez (ID 5 - Prioridad 120)
(5, 9, 'Quimico', NULL, 'PORCENTAJE', 0.36, NULL, 120, 1, 'Jeimy Añez - Mechas 36%'),
(5, 26, 'Extensiones', NULL, 'MONTO_FIJO', 20.00, NULL, 120, 1, 'Jeimy Añez - Extensiones 1 Paquete ($20 fijo)'),
(5, 27, 'Extensiones', NULL, 'MONTO_FIJO', 30.00, NULL, 120, 1, 'Jeimy Añez - Extensiones 2 Paquetes ($30 fijo)'),
(5, 28, 'Extensiones', NULL, 'MONTO_FIJO', 40.00, NULL, 120, 1, 'Jeimy Añez - Extensiones 3 Paquetes ($40 fijo)'),
(5, 29, 'Extensiones', NULL, 'MONTO_FIJO', 40.00, NULL, 120, 1, 'Jeimy Añez - Extensiones 4 Paquetes ($40 fijo)'),

-- 5.6 Excepciones: Aurora Sofia Exposito (ID 4 - Prioridad 120)
(4, 10, 'Peluqueria', NULL, 'PORCENTAJE', 0.50, NULL, 120, 1, 'Aurora Sofia Exposito - Cejas 50%'),
(4, 11, 'Peluqueria', NULL, 'PORCENTAJE', 0.50, NULL, 120, 1, 'Aurora Sofia Exposito - Bozo 50%'),

-- 5.7 Excepciones: Dayana Govea (ID 1)
(1, 1, 'Lavado', NULL, 'MONTO_FIJO', 3.00, 8.00, 150, 1, 'Dayana Govea - Lavado de $8 paga $3 fijo'),
(1, NULL, 'Lavado', NULL, 'PORCENTAJE', 0.40, NULL, 70, 1, 'Dayana Govea - Lavado general 40%'),
(1, 16, 'Quimico', NULL, 'MONTO_FIJO', 8.00, NULL, 120, 1, 'Dayana Govea - Hidratación Fusio-Dose ($8 fijo)'),
(1, 26, 'Extensiones', NULL, 'MONTO_FIJO', 10.00, NULL, 120, 1, 'Dayana Govea - Extensiones 1 Paquete ($10 fijo)'),
(1, 27, 'Extensiones', NULL, 'MONTO_FIJO', 20.00, NULL, 120, 1, 'Dayana Govea - Extensiones 2 Paquetes ($20 fijo)'),
(1, 28, 'Extensiones', NULL, 'MONTO_FIJO', 15.00, NULL, 120, 1, 'Dayana Govea - Extensiones 3 Paquetes ($15 fijo)'),

-- 5.8 Excepciones: Maria Virginia Romero (ID 2)
(2, 1, 'Lavado', NULL, 'MONTO_FIJO', 3.00, 8.00, 150, 1, 'Maria Virginia Romero - Lavado de $8 paga $3 fijo'),
(2, NULL, 'Lavado', NULL, 'PORCENTAJE', 0.40, NULL, 70, 1, 'Maria Virginia Romero - Lavado general 40%'),
(2, 16, 'Quimico', NULL, 'MONTO_FIJO', 8.00, NULL, 120, 1, 'Maria Virginia Romero - Hidratación Fusio-Dose ($8 fijo)'),
(2, 26, 'Extensiones', NULL, 'MONTO_FIJO', 10.00, NULL, 120, 1, 'Maria Virginia Romero - Extensiones 1 Paquete ($10 fijo)'),
(2, 27, 'Extensiones', NULL, 'MONTO_FIJO', 20.00, NULL, 120, 1, 'Maria Virginia Romero - Extensiones 2 Paquetes ($20 fijo)'),
(2, 28, 'Extensiones', NULL, 'MONTO_FIJO', 15.00, NULL, 120, 1, 'Maria Virginia Romero - Extensiones 3 Paquetes ($15 fijo)'),
(2, 10, 'Peluqueria', NULL, 'PORCENTAJE', 0.50, NULL, 120, 1, 'Maria Virginia Romero - Cejas 50%'),
(2, 11, 'Peluqueria', NULL, 'PORCENTAJE', 0.50, NULL, 120, 1, 'Maria Virginia Romero - Bozo 50%'),

-- 5.9 Excepciones: Jaqueline Añez (ID 10)
(10, 1, 'Lavado', NULL, 'MONTO_FIJO', 3.00, 8.00, 150, 1, 'Jaqueline Añez - Lavado de $8 paga $3 fijo'),
(10, NULL, 'Lavado', NULL, 'PORCENTAJE', 0.40, NULL, 70, 1, 'Jaqueline Añez - Lavado general 40%'),
(10, 16, 'Quimico', NULL, 'MONTO_FIJO', 8.00, NULL, 120, 1, 'Jaqueline Añez - Hidratación Fusio-Dose ($8 fijo)'),
(10, 26, 'Extensiones', NULL, 'MONTO_FIJO', 10.00, NULL, 120, 1, 'Jaqueline Añez - Extensiones 1 Paquete ($10 fijo)'),
(10, 27, 'Extensiones', NULL, 'MONTO_FIJO', 20.00, NULL, 120, 1, 'Jaqueline Añez - Extensiones 2 Paquetes ($20 fijo)'),
(10, 28, 'Extensiones', NULL, 'MONTO_FIJO', 15.00, NULL, 120, 1, 'Jaqueline Añez - Extensiones 3 Paquetes ($15 fijo)');
