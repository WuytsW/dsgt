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


    /*
        This needs to implemented when the suppliers are set up.
        For now it just returns a JSON with the ingredients grouped by supplier as a placeholder
     */



    @PostMapping("/{userId}")
    public ResponseEntity<?> placeOrder(@PathVariable Integer userId) {
        // 1. Get user's basket
        List<Basket> basketItems = basketRepository.findByUserId(userId);
        if (basketItems.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Basket is empty"));
        }

        // 2. Aggregate ingredient needs
        Map<Integer, Integer> ingredientTotals = new HashMap<>();
        for (Basket basket : basketItems) {
            Recipe recipe = recipeRepository.getById(basket.getRecipeId());
            int quantity = basket.getQuantity();

            for (Ingredient ingredient : recipe.getIngredients()) {
                int ingredientId = ingredient.getId();
                //int ingredientId = 1; //FOR NOW!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                int amountPerRecipe = 1; // replace with actual per-recipe logic if needed
                int totalAmount = quantity * amountPerRecipe;

                ingredientTotals.merge(ingredientId, totalAmount, Integer::sum);
            }
        }

        // 3. Check stock availability for ALL ingredients
        for (Map.Entry<Integer, Integer> entry : ingredientTotals.entrySet()) {
            int ingredientId = entry.getKey();
            int required = entry.getValue();

            int supplierId = ingredientRepository.findById(ingredientId)
                    .map(Ingredient::getSupplierId)
                    .filter(Objects::nonNull)
                    .orElse(10);

            String supplierUrl = supplierRepository.findById(supplierId).get().getUrl();

            try {
                ResponseEntity<Map> response = restTemplate.getForEntity(
                        supplierUrl + "/stock/" + ingredientId, Map.class);

                Integer stock = (Integer) response.getBody().get("stock");
                if (stock == null || stock < required) {
                    return ResponseEntity.badRequest().body(
                            Map.of("error", "Insufficient stock for ingredient " + ingredientId)
                    );
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                        Map.of("error", "Stock check failed for ingredient " + ingredientId, "details", e.getMessage())
                );
            }
        }

        // 4. All stock is OK — now place orders one by one
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

        // 5. Return success if all orders placed
        return ResponseEntity.ok(Map.of("message", "All ingredient orders placed successfully"));
    }



    /*
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
            if (recipe == null) {
                continue;
            }


            // Fetch ingredients for this recipe
            List<Ingredient> ingredients = recipe.getIngredients();


            for (Ingredient ingredient : ingredients) {
                Integer id = ingredient.getId();
                if (id == null) continue; // Skip ingredients without ID
                ingredientQuantities.merge(id, quantity, Integer::sum);
            }
        }

        // 3. Group ingredients by supplier
        // Map<Supplier, List<Ingredient and Quantity>>
        Map<Supplier, List<Map<String, Object>>> supplierIngredientsMap = new HashMap<>();

        for (Map.Entry<Integer, Integer> entry : ingredientQuantities.entrySet()) {
            Integer ingredientId = entry.getKey();
            if (ingredientId == null) continue;

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
    */
}

