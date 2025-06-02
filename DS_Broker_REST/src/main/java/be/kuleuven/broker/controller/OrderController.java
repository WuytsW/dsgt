package be.kuleuven.broker.controller;

import be.kuleuven.broker.model.*;
import be.kuleuven.broker.repository.BasketRepository;
import be.kuleuven.broker.repository.IngredientRepository;
import be.kuleuven.broker.repository.SupplierRepository;
import be.kuleuven.broker.repository.RecipeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/order")
public class OrderController {
    private final RestTemplate restTemplate = new RestTemplate();
    private final BasketRepository basketRepository;
    private final IngredientRepository ingredientRepository;
    private final SupplierRepository supplierRepository;
    private final RecipeRepository recipeRepository;

    public OrderController( BasketRepository basketRepository, IngredientRepository ingredientRepository, SupplierRepository supplierRepository, RecipeRepository recipeRepository) {
        this.basketRepository = basketRepository;
        this.ingredientRepository = ingredientRepository;
        this.supplierRepository = supplierRepository;
        this.recipeRepository = recipeRepository;
    }


    @PostMapping("/{userId}")
    public ResponseEntity<?> placeOrder(@PathVariable Integer userId) {
        List<Basket> basketItems = basketRepository.findByUserId(userId);
        if (basketItems.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Basket is empty"));
        }

        // Group ingredient quantities
        Map<Integer, Integer> ingredientTotals = new HashMap<>();
        for (Basket basket : basketItems) {
            Recipe recipe = recipeRepository.getById(basket.getRecipeId());
            int quantity = basket.getQuantity();

            for (Ingredient ingredient : recipe.getIngredients()) {
                int ingredientId = ingredient.getId();
                int amountPerRecipe = 1; // replace with actual per-recipe logic if needed
                int totalAmount = quantity * amountPerRecipe;

                ingredientTotals.merge(ingredientId, totalAmount, Integer::sum);
            }
        }

        // Check stock
        List<Map<String, Object>> insufficientStock = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : ingredientTotals.entrySet()) {
            int ingredientId = entry.getKey();
            int required = entry.getValue();

            Optional<Ingredient> optionalIngredient = ingredientRepository.findById(ingredientId);
            int supplierId = 10; // default fallback

            if (optionalIngredient.isPresent()) {
                Integer possibleSupplierId = optionalIngredient.get().getSupplierId();
                if (possibleSupplierId != null) {
                    supplierId = possibleSupplierId;
                }
            }


            String supplierUrl = supplierRepository.findById(supplierId).get().getUrl();

            try {
                ResponseEntity<Map> response = restTemplate.getForEntity(supplierUrl + "/stock/" + ingredientId, Map.class);

                Integer stock = (Integer) response.getBody().get("stock");
                if (stock == null || stock < required) {
                    insufficientStock.add(Map.of(
                            "ingredientId", ingredientId,
                            "required", required,
                            "available", stock != null ? stock : 0
                    ));
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                        Map.of("error", "Stock check failed for ingredient " + ingredientId, "details", e.getMessage())
                );
            }
        }

        if (!insufficientStock.isEmpty()) {
            Set<Integer> blockedRecipeIds = new HashSet<>();

            for (Basket basket : basketItems) {
                Recipe recipe = recipeRepository.getById(basket.getRecipeId());
                for (Ingredient ingredient : recipe.getIngredients()) {
                    int ingId = ingredient.getId();
                    for (Map<String, Object> issue : insufficientStock) {
                        if ((int) issue.get("ingredientId") == ingId) {
                            blockedRecipeIds.add(recipe.getId());
                            break;
                        }
                    }
                }
            }

            List<RecipeDTO> blockedRecipeDTOs = new ArrayList<>();
            for (Integer id : blockedRecipeIds) {
                Recipe recipe = recipeRepository.getById(id);
                basketRepository.deleteByUserIdAndRecipeId(userId, id);
                blockedRecipeDTOs.add(new RecipeDTO(recipe.getId(), recipe.getRecipe()));
            }


            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of(
                            "error", "Some recipes cannot be ordered",
                            "blockedRecipes", blockedRecipeDTOs
                    )
            );
        }


        // Order
        for (Map.Entry<Integer, Integer> entry : ingredientTotals.entrySet()) {
            int ingredientId = entry.getKey();
            int amount = entry.getValue();

            int supplierId = ingredientRepository.findById(ingredientId)
                    .map(Ingredient::getSupplierId)
                    .filter(Objects::nonNull)
                    .orElse(10);
            String supplierUrl = supplierRepository.findById(supplierId).get().getUrl();

            try {
                OrderRequest orderRequest = new OrderRequest();
                orderRequest.setIngredientId(ingredientId);
                orderRequest.setAmount(amount);

                HttpEntity<OrderRequest> requestEntity = new HttpEntity<>(orderRequest);
                ResponseEntity<String> response = restTemplate.postForEntity(
                        supplierUrl + "/order/", requestEntity, String.class
                );

                if (!response.getStatusCode().is2xxSuccessful()) {
                    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                            Map.of("error", "Order failed for ingredient " + ingredientId, "details", response.getBody())
                    );
                }

            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                        Map.of("error", "Error placing order for ingredient " + ingredientId, "details", e.getMessage())
                );
            }
        }

        basketRepository.deleteAll(basketItems);
        return ResponseEntity.ok(Map.of("message", "All ingredient orders placed successfully"));
    }
}

