-- Productos
INSERT INTO productos (id, nombre, precio) VALUES (1, 'Teclado mecanico', 250000);
INSERT INTO productos (id, nombre, precio) VALUES (2, 'Mouse inalambrico', 80000);
INSERT INTO productos (id, nombre, precio) VALUES (3, 'Monitor 24 pulgadas', 600000);

-- Inventario (stock disponible)
INSERT INTO inventario (producto_id, stock) VALUES (1, 10);
INSERT INTO inventario (producto_id, stock) VALUES (2, 1);
INSERT INTO inventario (producto_id, stock) VALUES (3, 5);

-- Clientes: 10 = VIP, 20 = FRECUENTE, 30 = MOROSO con deuda (id 40 no existe, para probar "cliente no registrado")
INSERT INTO clientes (id, nombre, tipo_cliente) VALUES (10, 'Cliente VIP', 'VIP');
INSERT INTO clientes (id, nombre, tipo_cliente) VALUES (20, 'Cliente Frecuente', 'FRECUENTE');
INSERT INTO clientes (id, nombre, tipo_cliente) VALUES (30, 'Cliente Moroso', 'MOROSO');

-- Factura pendiente del cliente moroso
INSERT INTO facturas (cliente_id, monto, pagada) VALUES (30, 150000, false);

-- Pedidos previos del cliente frecuente (para que pedidosPrevios > 3)
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (20, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO');
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (20, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO');
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (20, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO');
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (20, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO');