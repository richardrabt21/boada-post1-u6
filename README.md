# Post-contenido — Unidad 6: Antipatrones de Diseño

## Descripción
Repositorio del post-contenido de la Unidad 6 de Patrones de Diseño de Software.
Proyecto Spring Boot (pedidos-service/) con un sistema de gestión de pedidos
de un comercio electrónico. Parte 1: diagnóstico y refactorización de un
antipatrón combinado en GestorPedidos.

## Decisiones de diseño

### Parte 1 - GestorPedidos

Antipatrón identificado: God Object y Spaghetti Code combinados.

GestorPedidos.procesarPedido() (líneas 33-163, más de 130 líneas en un único
método) mezcla 6 responsabilidades con razones de cambio independientes entre sí:

- Líneas 36-51: validación de stock (consulta SQL embebida directamente en la
  lógica de negocio).
- Líneas 52-73: validación de cliente y mora, con una excepción de horario
  (LocalTime.of(20, 0)) anidada 3 niveles de profundidad: tipo de cliente ->
  existencia de deuda -> horario de corte.
- Líneas 74-81: cálculo de subtotal, con una consulta SQL individual por cada
  ítem del pedido dentro del propio cálculo de precio.
- Líneas 82-111: cálculo de descuento con 2 niveles de anidamiento condicional
  según el tipo de cliente (VIP: 3 rangos de monto; FRECUENTE: conteo de
  pedidos previos vía una tercera consulta SQL más).
- Líneas 112-139: persistencia directa vía JDBC (2 INSERT y 1 UPDATE), sin
  repositorio ni transacción explícita.
- Líneas 140-163: construcción y envío de la notificación por correo, con el
  cuerpo del mensaje armado a mano con StringBuilder en el mismo método.

Esto implica que el método opera simultáneamente en 3 niveles de abstracción
distintos (SQL crudo, reglas de negocio, formato de texto del correo) sin
ninguna separación entre ellos. Si se necesitara agregar un nuevo tipo de
cliente con su propia regla de descuento, habría que modificar el mismo
bloque if/else if de las líneas 82-111, arriesgando romper las reglas de
VIP y FRECUENTE que ya funcionan, porque las tres comparten el mismo bloque
condicional sin ningún aislamiento entre ellas.

Patrón aplicado: Chain of Responsibility para las validaciones (tienen una
dependencia real de orden: si el stock falla, no tiene sentido consultar la
mora del cliente) y Strategy para el cálculo de descuento (no depende de un
orden, sino de qué regla aplica según el tipo de cliente).

Alternativa descartada: una lista de métodos booleanos (Predicate) invocados
en secuencia para las validaciones. Se descartó porque evaluaría todas las
validaciones aunque la primera ya haya fallado, y no permite que un validador
decida no delegar al siguiente - el corte anticipado que sí ofrece Chain of
Responsibility.

## Como ejecutar

mvnw.cmd spring-boot:run

## Herramientas utilizadas
- Java 17, Spring Boot, Spring JDBC, Maven, H2 Database
- VS Code, Git, GitHub