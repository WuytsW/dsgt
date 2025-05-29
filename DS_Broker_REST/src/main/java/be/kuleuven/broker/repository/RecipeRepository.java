package be.kuleuven.broker.repository;

import be.kuleuven.broker.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Integer> {
    @Query("SELECT DISTINCT r FROM Recipe r JOIN FETCH r.ingredients")
    List<Recipe> findAllWithIngredients();

    @Query("SELECT r FROM Recipe r JOIN FETCH r.ingredients WHERE r.id = :id")
    Recipe findByIdWithIngredients(@Param("id") Integer id);
}

