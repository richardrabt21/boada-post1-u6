package com.tienda.pedidos;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.service.GestorPedidos;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class PruebaPedidosRunner {

    @Bean
    CommandLineRunner ejecutarPruebas(GestorPedidos gestorPedidos) {
        return args -> {
            System.out.println("\n===== CASO 1: Stock insuficiente (producto 2, pide 2, solo hay 1) =====");
            imprimir(gestorPedidos.procesarPedido(pedido(10L, "vip@correo.com", item(2L, 2))));

            System.out.println("\n===== CASO 2: Cliente inexistente (id 40) =====");
            imprimir(gestorPedidos.procesarPedido(pedido(40L, "nadie@correo.com", item(1L, 1))));

            System.out.println("\n===== CASO 3: Cliente moroso con deuda pendiente =====");
            imprimir(gestorPedidos.procesarPedido(pedido(30L, "moroso@correo.com", item(1L, 1))));

            System.out.println("\n===== CASO 4: Cliente VIP (descuento por monto) =====");
            imprimir(gestorPedidos.procesarPedido(pedido(10L, "vip@correo.com", item(3L, 2))));

            System.out.println("\n===== CASO 5: Cliente frecuente (mas de 3 pedidos previos) =====");
            imprimir(gestorPedidos.procesarPedido(pedido(20L, "frecuente@correo.com", item(1L, 1))));
        };
    }

    private PedidoRequest pedido(Long clienteId, String email, ItemPedido... items) {
        PedidoRequest request = new PedidoRequest();
        request.setClienteId(clienteId);
        request.setClienteEmail(email);
        request.setItems(List.of(items));
        return request;
    }

    private ItemPedido item(Long productoId, int cantidad) {
        ItemPedido item = new ItemPedido();
        item.setProductoId(productoId);
        item.setCantidad(cantidad);
        return item;
    }

    private void imprimir(ResultadoPedido resultado) {
        if (resultado.isConfirmado()) {
            System.out.println("CONFIRMADO - Pedido #" + resultado.getPedidoId() + " - Total: " + resultado.getTotal());
        } else {
            System.out.println("RECHAZADO - Motivo: " + resultado.getMotivoRechazo());
        }
    }
}