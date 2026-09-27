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

### Comparación antes/después (verificación de comportamiento)

Los 5 pedidos de prueba producen el mismo resultado con GestorPedidos ya
refactorizado que con la versión original de más de 130 líneas en un solo
método:

| Caso | Descripción | Resultado |
|---|---|---|
| 1 | Stock insuficiente (producto 2) | RECHAZADO |
| 2 | Cliente inexistente (id 40) | RECHAZADO |
| 3 | Cliente moroso con deuda | RECHAZADO (o CONFIRMADO fuera del horario de corte) |
| 4 | Cliente VIP | CONFIRMADO |
| 5 | Cliente frecuente | CONFIRMADO |

GestorPedidos.procesarPedido() pasó de mezclar 6 responsabilidades en un
único método de más de 130 líneas a coordinar 4 colaboradores independientes
(ValidadorPedido, SelectorEstrategiaDescuento, PedidoRepository,
NotificacionPedidoService) en un método de menos de 20 líneas.

## Cómo ejecutar

mvnw.cmd spring-boot:run

## Herramientas utilizadas
- Java 17, Spring Boot, Spring JDBC, Maven, H2 Database
- VS Code, Git, GitHub

### Parte 2 - Crecimiento del proyecto (3 campanas de descuento)

Antipatron identificado: Golden Hammer.

Las tres campanas de descuento nuevas (Black Friday, Corporativo, Volumen)
se implementaron como eslabones adicionales de la cadena de validacion
existente (PromocionBlackFriday, PromocionCorporativo, PromocionVolumen,
encadenados en GestorPedidos junto a ValidadorStock y ValidadorCliente),
en lugar de evaluar si el problema seguia teniendo la forma de una cadena.

Evidencia:

- Ninguna de las tres clases nuevas depende de un orden de ejecucion entre
  si ni frente a ValidadorStock/ValidadorCliente: pueden encadenarse en
  cualquier orden y el resultado es identico, porque cada una solo lee el
  request o consulta la BD y escribe en el campo compartido
  descuentoCampana, sin leer nada que haya escrito la anterior. Esto
  contrasta con ValidadorStock y ValidadorCliente, que si tienen una
  dependencia real de orden y de corte anticipado (si el stock falla, no
  tiene sentido validar la mora del cliente).
- Ninguna de las tres clases nuevas llama nunca a contexto.rechazar(...),
  a pesar de heredar de una clase llamada ValidadorPedido cuyo contrato es
  "decidir si el pedido continua o se rechaza". Usan la infraestructura de
  la cadena (el encadenamiento) sin usar lo que la hace valiosa: la
  capacidad de cortar el flujo.
- El campo descuentoCampana solo puede quedarse con el mayor valor escrito
  (aplicarDescuentoCampana usa Math.max implicito). Si dos campanas
  necesitaran combinarse (por ejemplo, sumarse) en vez de competir, la
  cadena no ofrece ningun mecanismo para expresarlo sin reescribir ese
  metodo.
- Las tres campanas tienen exactamente la misma forma que DescuentoVip o
  DescuentoFrecuente de la Parte 1: calculan un porcentaje a partir de
  datos del pedido o del cliente, sin depender de un orden de evaluacion.
  Se reutilizo Chain of Responsibility unicamente porque "ya funciono" en
  la Parte 1 para las validaciones, sin evaluar si el nuevo problema tenia
  esa misma forma.

Patron aplicado: Strategy. Las tres campanas se migran a EstrategiaDescuento
(DescuentoBlackFriday, DescuentoCorporativo, DescuentoVolumen) y se combinan
con el descuento por tipo de cliente en un CalculadorDescuentoFinal, que
toma el mayor entre ambos. PromocionBlackFriday, PromocionCorporativo,
PromocionVolumen y el campo descuentoCampana se eliminan del codigo (no se
comentan, para no dejar un Lava Flow).

Alternativa descartada: mantener las tres campanas como eslabones de la
cadena, ajustando ContextoPedido para soportar combinaciones distintas al
maximo. Se descarto porque es precisamente la causa del antipatron
diagnosticado: seguir forzando una herramienta que no corresponde a la
forma del problema, en vez de reconocer que el calculo de descuento ya
tiene su propio mecanismo de extension (Strategy) desde la Parte 1.

### Comparacion antes/despues - Parte 2

Los 8 casos de prueba (incluidas las 3 campanas) producen exactamente el
mismo total con CalculadorDescuentoFinal que con los 3 eslabones de Golden
Hammer eliminados:

| Caso | Descripcion | Total |
|---|---|---|
| 6 | Black Friday activa | $535.500 |
| 7 | Cliente corporativo (NIT) | $223.125 |
| 8 | Descuento por volumen (>20 unidades) | $4.685.625 |

PromocionBlackFriday, PromocionCorporativo, PromocionVolumen y el campo
descuentoCampana ya no existen en el codigo (se eliminaron, no se
comentaron). ValidadorPedido conserva unicamente ValidadorStock y
ValidadorCliente como eslabones, que son los dos que si tienen dependencia
de orden y necesidad de corte anticipado.

## Conclusiones

Este laboratorio mostro que no todo problema que "se parece" a uno ya
resuelto tiene la misma forma: God Object y Spaghetti Code se originaron
por concentrar responsabilidades sin razon de cambio compartida, y se
corrigieron separando esas responsabilidades en Chain of Responsibility
(para lo que si dependia de un orden) y Strategy (para lo que no). El
antipatron de la Parte 2, Golden Hammer, no fue un problema de complejidad
sino de evaluacion: la cadena de responsabilidad funciono bien para las
validaciones porque tenian dependencia de orden y corte anticipado, pero
las campanas de descuento no compartian esa propiedad, y aun asi se
modelaron igual solo porque el patron "ya estaba ahi y funciono". La
leccion practica es que un patron de diseno no se elige por precedente,
sino por si la forma del nuevo problema coincide con la del que el patron
resuelve.