package be.kuleuven.broker.controller;

import be.kuleuven.broker.model.*;
import be.kuleuven.broker.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class OrderController {
    private final RestTemplate restTemplate = new RestTemplate();
    private final BasketRepository basketRepository;
    private final IngredientRepository ingredientRepository;
    private final SupplierRepository supplierRepository;
    private final RecipeRepository recipeRepository;
    private final OrderRepository orderRepository;

    public OrderController(BasketRepository basketRepository, IngredientRepository ingredientRepository, SupplierRepository supplierRepository, RecipeRepository recipeRepository, OrderRepository orderRepository) {
        this.basketRepository = basketRepository;
        this.ingredientRepository = ingredientRepository;
        this.supplierRepository = supplierRepository;
        this.recipeRepository = recipeRepository;
        this.orderRepository = orderRepository;
    }

    @PostMapping("/check/{userId}")
    public ResponseEntity<?> checkOrder(@PathVariable Integer userId) {
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

            for (Integer id : blockedRecipeIds) {
                basketRepository.deleteByUserIdAndRecipeId(userId, id);
            }

            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of(
                            "error", "Some recipes are not available",
                            "blockedRecipesIds", blockedRecipeIds
                    )
            );
        }


        return ResponseEntity.ok(Map.of("message", "All ingredient are available"));
    }

    @PostMapping("/pay/{userId}")
    public ResponseEntity<String> pay(@PathVariable Long userId) {
        return ResponseEntity.ok("Payment processed for user ID: " + userId);
    }

    @PostMapping("/order/{userId}")
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

            for (Integer id : blockedRecipeIds) {
                basketRepository.deleteByUserIdAndRecipeId(userId, id);
            }

            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                Map.of("error", "Some recipes cannot be ordered", "blockedRecipesIds", blockedRecipeIds)
            );
        }


        // Order
        List<OrderRequest> successfulOrders = new ArrayList<>();

        for (Map.Entry<Integer, Integer> entry : ingredientTotals.entrySet()) {
            int ingredientId = entry.getKey();
            int amount = entry.getValue();

            int supplierId = ingredientRepository.findById(ingredientId).map(Ingredient::getSupplierId).filter(Objects::nonNull).orElse(10);
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
                    revertSuccessfulOrders(successfulOrders);
                    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                        "error", "Order failed for ingredient " + ingredientId, "details", response.getBody()
                    ));
                }

                // Keep track of this successful order
                successfulOrders.add(orderRequest);

            } catch (Exception e) {
                // Revert successful ones
                revertSuccessfulOrders(successfulOrders);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                    Map.of("error", "Error placing order for ingredient " + ingredientId, "details", e.getMessage())
                );
            }
        }


        //Clear the basket
        basketRepository.deleteAll(basketItems);

        // Save Order
        Order order = new Order();
        order.setUserId(userId);
        order.setTimestamp(LocalDateTime.now());

        List<OrderRecipe> orderRecipes = new ArrayList<>();
        for (Basket basket : basketItems) {
            Recipe recipe = recipeRepository.getById(basket.getRecipeId());

            OrderRecipe or = new OrderRecipe();
            or.setOrder(order);
            or.setRecipe(recipe);
            or.setQuantity(basket.getQuantity());

            orderRecipes.add(or);
        }

        order.setOrderRecipes(orderRecipes);
        orderRepository.save(order);


        return ResponseEntity.ok(Map.of("message", "All ingredient orders placed successfully"));
    }


    private void revertSuccessfulOrders(List<OrderRequest> successfulOrders) {
        for (OrderRequest order : successfulOrders) {
            try {
                int ingredientId = order.getIngredientId();
                int supplierId = ingredientRepository.findById(ingredientId).map(Ingredient::getSupplierId).filter(Objects::nonNull).orElse(10);
                String supplierUrl = supplierRepository.findById(supplierId).get().getUrl();

                HttpEntity<OrderRequest> revertEntity = new HttpEntity<>(order);
                restTemplate.postForEntity(supplierUrl + "/revert", revertEntity, String.class);

            } catch (Exception ex) {
                System.err.println("Failed to revert order for ingredient " + order.getIngredientId() + ": " + ex.getMessage());
            }
        }
    }


    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        return ResponseEntity.ok(orders);
    }






}

