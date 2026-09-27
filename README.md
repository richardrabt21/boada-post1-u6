# Post-contenido â€” Unidad 6: Antipatrones de DiseÃ±o

## DescripciÃ³n
Repositorio del post-contenido de la Unidad 6 de Patrones de DiseÃ±o de Software.
Proyecto Spring Boot (pedidos-service/) con un sistema de gestiÃ³n de pedidos
de un comercio electrÃ³nico. Parte 1: diagnÃ³stico y refactorizaciÃ³n de un
antipatrÃ³n combinado en GestorPedidos.

## Decisiones de diseÃ±o

### Parte 1 - GestorPedidos

AntipatrÃ³n identificado: God Object y Spaghetti Code combinados.

GestorPedidos.procesarPedido() (lÃ­neas 33-163, mÃ¡s de 130 lÃ­neas en un Ãºnico
mÃ©todo) mezcla 6 responsabilidades con razones de cambio independientes entre sÃ­:

- LÃ­neas 36-51: validaciÃ³n de stock (consulta SQL embebida directamente en la
  lÃ³gica de negocio).
- LÃ­neas 52-73: validaciÃ³n de cliente y mora, con una excepciÃ³n de horario
  (LocalTime.of(20, 0)) anidada 3 niveles de profundidad: tipo de cliente ->
  existencia de deuda -> horario de corte.
- LÃ­neas 74-81: cÃ¡lculo de subtotal, con una consulta SQL individual por cada
  Ã­tem del pedido dentro del propio cÃ¡lculo de precio.
- LÃ­neas 82-111: cÃ¡lculo de descuento con 2 niveles de anidamiento condicional
  segÃºn el tipo de cliente (VIP: 3 rangos de monto; FRECUENTE: conteo de
  pedidos previos vÃ­a una tercera consulta SQL mÃ¡s).
- LÃ­neas 112-139: persistencia directa vÃ­a JDBC (2 INSERT y 1 UPDATE), sin
  repositorio ni transacciÃ³n explÃ­cita.
- LÃ­neas 140-163: construcciÃ³n y envÃ­o de la notificaciÃ³n por correo, con el
  cuerpo del mensaje armado a mano con StringBuilder en el mismo mÃ©todo.

Esto implica que el mÃ©todo opera simultÃ¡neamente en 3 niveles de abstracciÃ³n
distintos (SQL crudo, reglas de negocio, formato de texto del correo) sin
ninguna separaciÃ³n entre ellos. Si se necesitara agregar un nuevo tipo de
cliente con su propia regla de descuento, habrÃ­a que modificar el mismo
bloque if/else if de las lÃ­neas 82-111, arriesgando romper las reglas de
VIP y FRECUENTE que ya funcionan, porque las tres comparten el mismo bloque
condicional sin ningÃºn aislamiento entre ellas.

PatrÃ³n aplicado: Chain of Responsibility para las validaciones (tienen una
dependencia real de orden: si el stock falla, no tiene sentido consultar la
mora del cliente) y Strategy para el cÃ¡lculo de descuento (no depende de un
orden, sino de quÃ© regla aplica segÃºn el tipo de cliente).

Alternativa descartada: una lista de mÃ©todos booleanos (Predicate) invocados
en secuencia para las validaciones. Se descartÃ³ porque evaluarÃ­a todas las
validaciones aunque la primera ya haya fallado, y no permite que un validador
decida no delegar al siguiente - el corte anticipado que sÃ­ ofrece Chain of
Responsibility.

## Como ejecutar

mvnw.cmd spring-boot:run

## Herramientas utilizadas
- Java 17, Spring Boot, Spring JDBC, Maven, H2 Database
- VS Code, Git, GitHub

### Comparacion antes/despues (verificacion de comportamiento)

Los 5 pedidos de prueba producen el mismo resultado con GestorPedidos ya
refactorizado que con la version original de mas de 130 lineas en un solo
metodo:

| Caso | Descripcion | Resultado |
|---|---|---|
| 1 | Stock insuficiente (producto 2) | RECHAZADO |
| 2 | Cliente inexistente (id 40) | RECHAZADO |
| 3 | Cliente moroso con deuda | RECHAZADO (o CONFIRMADO fuera del horario de corte) |
| 4 | Cliente VIP | CONFIRMADO |
| 5 | Cliente frecuente | CONFIRMADO |

GestorPedidos.procesarPedido() paso de mezclar 6 responsabilidades en un
unico metodo de mas de 130 lineas a coordinar 4 colaboradores independientes
(ValidadorPedido, SelectorEstrategiaDescuento, PedidoRepository,
NotificacionPedidoService) en un metodo de menos de 20 lineas.