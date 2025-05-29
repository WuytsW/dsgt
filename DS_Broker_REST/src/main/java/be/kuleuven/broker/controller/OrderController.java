package be.kuleuven.broker.controller;

import be.kuleuven.broker.model.Basket;
import be.kuleuven.broker.model.Ingredient;
import be.kuleuven.broker.model.Recipe;
import be.kuleuven.broker.model.Supplier;
import be.kuleuven.broker.repository.BasketRepository;
import be.kuleuven.broker.repository.IngredientRepository;
import be.kuleuven.broker.repository.SupplierRepository;
import be.kuleuven.broker.repository.RecipeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private BasketRepository basketRepository;
    @Autowired
    private IngredientRepository ingredientRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private RecipeRepository recipeRepository;


    /*
        This needs to implemented when the suppliers are set up.
        For now it just returns a JSON with the ingredients grouped by supplier as a placeholder
     */


    @PostMapping("/{userId}")
    public ResponseEntity<?> checkAvailabilityAndOrder(@PathVariable int userId) {
        // 1. Fetch all basket items for a specific user
        List<Basket> basketItems = basketRepository.findByUserId(userId);

        // 2. Aggregate ingredients needed with their total quantities
        // Map<IngredientId, totalQuantity>
        Map<Integer, Integer> ingredientQuantities = new HashMap<>();

        for (Basket basket : basketItems) {
            int recipeId = basket.getRecipeId();
            int quantity = basket.getQuantity();

            Recipe recipe = recipeRepository.findByIdWithIngredients(recipeId);

            // Fetch ingredients for this recipe
            List<Ingredient> ingredients = recipe.getIngredients();

            for (Ingredient ingredient : ingredients) {
                ingredientQuantities.merge(ingredient.getId(), quantity, Integer::sum);
            }
        }

        // 3. Group ingredients by supplier
        // Map<Supplier, List<Ingredient and Quantity>>
        Map<Supplier, List<Map<String, Object>>> supplierIngredientsMap = new HashMap<>();

        for (Map.Entry<Integer, Integer> entry : ingredientQuantities.entrySet()) {
            int ingredientId = entry.getKey();
            int totalQuantity = entry.getValue();

            Ingredient ingredient = ingredientRepository.findById(ingredientId).orElse(null);
            if (ingredient == null) continue;

            Supplier supplier = supplierRepository.findById(ingredient.getSupplierId()).orElse(null);
            if (supplier == null) continue;

            supplierIngredientsMap.computeIfAbsent(supplier, k -> new ArrayList<>())
                    .add(Map.of(
                            "ingredientId", ingredient.getId(),
                            "ingredientName", ingredient.getIngredient(),
                            "quantity", totalQuantity
                    ));
        }

        // 4. Prepare response JSON structure
        List<Map<String, Object>> responseList = supplierIngredientsMap.entrySet().stream()
                .map(e -> Map.of(
                        "supplierId", e.getKey().getId(),
                        "supplierName", e.getKey().getName(),
                        "ingredients", e.getValue()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }
}
