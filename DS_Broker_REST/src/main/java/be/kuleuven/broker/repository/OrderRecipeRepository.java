package be.kuleuven.broker.repository;

import be.kuleuven.broker.model.OrderRecipe;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRecipeRepository extends JpaRepository<OrderRecipe, Integer> {
}
