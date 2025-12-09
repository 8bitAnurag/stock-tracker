package com.anurag.stock_tracker.Repository;

import com.anurag.stock_tracker.Entity.FavouriteStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FavouriteStockRepository extends JpaRepository<FavouriteStock, Long> {

    boolean existsBySymbol(String symbol);
}