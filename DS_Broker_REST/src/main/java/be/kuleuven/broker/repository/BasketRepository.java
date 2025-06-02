package be.kuleuven.broker.repository;

import be.kuleuven.broker.model.Basket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface BasketRepository extends JpaRepository<Basket, Integer> {
    List<Basket> findByUserId(int userId);
    void deleteByUserId(int userId);
    Basket findByUserIdAndRecipeId(int userId, int recipeId);

    @Transactional
    @Modifying
    @Query("DELETE FROM Basket b WHERE b.userId = :userId AND b.recipeId = :recipeId")
    void deleteByUserIdAndRecipeId(@Param("userId") Integer userId, @Param("recipeId") Integer recipeId);

}


