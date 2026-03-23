package com.ecom.inventory_service.repository;

import com.ecom.inventory_service.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    /**
     * This is DB-level locking — and this is an important interview concept.
     *
     * Remember Redis handles high concurrency with atomic operations. But when inventory-service updates the DB, we need to ensure two instances don't update the same row simultaneously. Pessimistic locking tells PostgreSQL — "lock this row for me until my transaction completes, nobody else can modify it."
     *
     * This is different from Redis locking. Redis lock protects the order creation flow. DB lock protects the inventory record update. Two layers of protection, two different concerns.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
    Optional<Inventory> findByProductIdWithLock(@Param("productId") UUID productId);

    Optional<Inventory> findByProductId(UUID productId);
}