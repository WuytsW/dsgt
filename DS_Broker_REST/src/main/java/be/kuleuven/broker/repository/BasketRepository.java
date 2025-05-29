package be.kuleuven.broker.repository;

import be.kuleuven.broker.model.Basket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BasketRepository extends JpaRepository<Basket, Integer> {
    List<Basket> findByUserId(int userId);
    void deleteByUserId(int userId);
    Basket findByUserIdAndRecipeId(int userId, int recipeId);
}


