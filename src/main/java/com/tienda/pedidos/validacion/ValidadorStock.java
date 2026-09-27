package com.tienda.pedidos.validacion;

import com.tienda.pedidos.dto.ItemPedido;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// Eslabon 1: existencia de stock (un solo motivo de rechazo, una sola responsabilidad)
@Component
public class ValidadorStock extends ValidadorPedido {

    private final JdbcTemplate jdbcTemplate;

    public ValidadorStock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected void ejecutar(ContextoPedido contexto) {
        for (ItemPedido item : contexto.getRequest().getItems()) {
            Integer stock = consultarOpcional(item.getProductoId());
            if (stock == null || stock < item.getCantidad()) {
                contexto.rechazar("Stock insuficiente: producto " + item.getProductoId());
                return;
            }
        }
    }

    private Integer consultarOpcional(Long productoId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT stock FROM inventario WHERE producto_id = ?", Integer.class, productoId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}