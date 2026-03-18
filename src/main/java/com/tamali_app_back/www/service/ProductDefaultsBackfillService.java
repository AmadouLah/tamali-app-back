package com.tamali_app_back.www.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Backfill non destructif pour compatibilité ascendante :
 * - anciens produits sans type/unité -> UNIT/PIECE
 * - anciennes quantités null -> 0
 *
 * Objectif: garantir que les anciens produits restent visibles après mise à jour.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Order(4)
public class ProductDefaultsBackfillService implements CommandLineRunner {

    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(String... args) {
        // Important: ne jamais faire échouer le démarrage. On ne met à jour que si la colonne existe.
        try {
            transactionTemplate.executeWithoutResult(status -> {
                int updatedType = runIfColumnExists("products", "product_type",
                        "UPDATE products SET product_type = 'UNIT' WHERE product_type IS NULL");
                int updatedUnit = runIfColumnExists("products", "unit",
                        "UPDATE products SET unit = 'PIECE' WHERE unit IS NULL");
                int updatedStockQty = runIfColumnExists("stocks", "quantity",
                        "UPDATE stocks SET quantity = 0 WHERE quantity IS NULL");
                int updatedMovQty = runIfColumnExists("stock_movements", "quantity",
                        "UPDATE stock_movements SET quantity = 0 WHERE quantity IS NULL");
                int updatedSaleItemQty = runIfColumnExists("sale_items", "quantity",
                        "UPDATE sale_items SET quantity = 0 WHERE quantity IS NULL");

                if (updatedType + updatedUnit + updatedStockQty + updatedMovQty + updatedSaleItemQty > 0) {
                    log.info("Backfill produits/quantités: product_type={}, unit={}, stocks.qty={}, stock_movements.qty={}, sale_items.qty={}",
                            updatedType, updatedUnit, updatedStockQty, updatedMovQty, updatedSaleItemQty);
                } else {
                    log.info("Backfill produits/quantités: rien à mettre à jour.");
                }
                entityManager.flush();
            });
        } catch (Exception e) {
            log.warn("Backfill produits/quantités ignoré (non bloquant): {}", e.getMessage());
        }
    }

    private int runIfColumnExists(String table, String column, String sql) {
        if (!columnExists(table, column)) return 0;
        return entityManager.createNativeQuery(sql).executeUpdate();
    }

    private boolean columnExists(String table, String column) {
        @SuppressWarnings("unchecked")
        List<Object> rows = entityManager.createNativeQuery(
                        "SELECT 1 FROM information_schema.columns " +
                                "WHERE table_schema = current_schema() " +
                                "AND table_name = :table " +
                                "AND column_name = :column " +
                                "LIMIT 1")
                .setParameter("table", table)
                .setParameter("column", column)
                .getResultList();
        return rows != null && !rows.isEmpty();
    }
}

