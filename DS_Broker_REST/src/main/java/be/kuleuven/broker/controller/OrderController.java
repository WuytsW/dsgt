package be.kuleuven.broker.controller;

import be.kuleuven.broker.model.*;
import be.kuleuven.broker.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class OrderController {
    private final RestTemplate restTemplate = new RestTemplate();
    private final BasketRepository basketRepository;
    private final IngredientRepository ingredientRepository;
    private final SupplierRepository supplierRepository;
    private final RecipeRepository recipeRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public OrderController(BasketRepository basketRepository, IngredientRepository ingredientRepository, SupplierRepository supplierRepository, RecipeRepository recipeRepository, OrderRepository orderRepository, UserRepository userRepository) {
        this.basketRepository = basketRepository;
        this.ingredientRepository = ingredientRepository;
        this.supplierRepository = supplierRepository;
        this.recipeRepository = recipeRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/check/{userId}")
    public ResponseEntity<?> checkOrder(@PathVariable Integer userId) {
        List<BasketItem> basketItems = basketRepository.findByUserId(userId);
        Map<Integer, Integer> ingredientTotals = calculateIngredientTotals(basketItems);

        // Check stock
        List<Map<String, Object>> insufficientStock;
        try {
            insufficientStock = checkIngredientStock(ingredientTotals);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", e.getMessage()));
        }

        if (!insufficientStock.isEmpty()) {
            Set<Integer> blockedRecipeIds = getBlockedRecipeIds(basketItems, insufficientStock);
            for (Integer id : blockedRecipeIds) {
                basketRepository.deleteByUserIdAndRecipeId(userId, id);
            }
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("error", "Some recipes are not available", "blockedRecipesIds", blockedRecipeIds)
            );
        }

        return ResponseEntity.ok(Map.of("message", "All ingredient are available"));
    }

    @PostMapping("/check-guest")
    public ResponseEntity<?> checkGuestBasket(@RequestBody List<BasketItem> basketItems) {
        Map<Integer, Integer> ingredientTotals = calculateIngredientTotals(basketItems);

        List<Map<String, Object>> insufficientStock;
        try {
            insufficientStock = checkIngredientStock(ingredientTotals);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", e.getMessage()));
        }

        if (!insufficientStock.isEmpty()) {
            Set<Integer> blockedRecipeIds = getBlockedRecipeIds(basketItems, insufficientStock);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("error", "Some recipes are not available", "blockedRecipesIds", blockedRecipeIds)
            );
        }

        return ResponseEntity.ok(Map.of("message", "All ingredient are available"));
    }


    @PostMapping("/order/{userId}")
    public ResponseEntity<?> placeOrder(@PathVariable Integer userId) {
        List<BasketItem> basketItems = basketRepository.findByUserId(userId);
        User user = userRepository.findById(userId).orElse(null);
        Map<Integer, Integer> ingredientTotals = calculateIngredientTotals(basketItems);

        ResponseEntity<?> stockResponse = checkAndHandleStock(ingredientTotals, basketItems, userId);
        if (stockResponse != null) return stockResponse;

        List<OrderRequest> successfulOrders = new ArrayList<>();
        ResponseEntity<?> orderResponse = placeIngredientOrders(ingredientTotals, user, successfulOrders);
        if (orderResponse != null) return orderResponse;

        basketRepository.deleteAll(basketItems);
        saveOrder(userId, basketItems);

        return ResponseEntity.ok(Map.of("message", "All ingredient orders placed successfully"));
    }

    @PostMapping("/order-guest")
    public ResponseEntity<?> placeGuestOrder(@RequestBody GuestOrder guestOrder) {
        List<BasketItem> basketItems = guestOrder.getBasket();
        User user = guestOrder.getGuestUser();
        Map<Integer, Integer> ingredientTotals = calculateIngredientTotals(basketItems);

        ResponseEntity<?> stockResponse = checkAndHandleStock(ingredientTotals, basketItems, null);
        if (stockResponse != null) return stockResponse;

        List<OrderRequest> successfulOrders = new ArrayList<>();
        ResponseEntity<?> orderResponse = placeIngredientOrders(ingredientTotals, user, successfulOrders);
        if (orderResponse != null) return orderResponse;

        saveOrder(0, basketItems);

        return ResponseEntity.ok(Map.of("message", "All ingredient orders placed successfully"));
    }


    @PostMapping("/pay/{userId}")
    public ResponseEntity<String> pay(@PathVariable Long userId) {
        return ResponseEntity.ok("Payment processed for user ID: " + userId);
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        return ResponseEntity.ok(orders);
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
    private Map<Integer, Integer> calculateIngredientTotals(List<BasketItem> basketItemItems) {
        Map<Integer, Integer> ingredientTotals = new HashMap<>();
        for (BasketItem basketItem : basketItemItems) {
            Recipe recipe = recipeRepository.getById(basketItem.getRecipeId());
            int quantity = basketItem.getQuantity();

            for (Ingredient ingredient : recipe.getIngredients()) {
                int ingredientId = ingredient.getId();
                int totalAmount = quantity * 1; // adjust if needed
                ingredientTotals.merge(ingredientId, totalAmount, Integer::sum);
            }
        }
        return ingredientTotals;
    }
    private List<Map<String, Object>> checkIngredientStock(Map<Integer, Integer> ingredientTotals) {
        List<Map<String, Object>> insufficientStock = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newFixedThreadPool(10); // Adjust thread pool size as needed

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Map.Entry<Integer, Integer> entry : ingredientTotals.entrySet()) {
            int ingredientId = entry.getKey();
            int required = entry.getValue();

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    int supplierId = ingredientRepository.findById(ingredientId)
                            .map(Ingredient::getSupplierId)
                            .filter(Objects::nonNull)
                            .orElse(10);

                    String supplierUrl = supplierRepository.findById(supplierId)
                            .orElseThrow(() -> new RuntimeException("Supplier not found for ID: " + supplierId))
                            .getUrl();

                    Integer supplierIngredientId = ingredientRepository.findById(ingredientId)
                            .orElseThrow(() -> new RuntimeException("Ingredient not found for ID: " + ingredientId))
                            .getIngredientId();


                    ResponseEntity<Map> response = restTemplate.getForEntity(
                            supplierUrl + "/stock/" + supplierIngredientId, Map.class);

                    Integer stock = (Integer) response.getBody().get("stock");

                    if (stock == null || stock < required) {
                        insufficientStock.add(Map.of(
                                "ingredientId", ingredientId,
                                "required", required,
                                "available", stock != null ? stock : 0
                        ));
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Stock check failed for ingredient " + ingredientId + ": " + e.getMessage(), e);
                }
            }, executor);

            futures.add(future);
        }

        // Wait for all tasks to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        return insufficientStock;
    }
    private ResponseEntity<?> checkAndHandleStock(Map<Integer, Integer> ingredientTotals, List<BasketItem> basketItems, Integer userId) {
        List<Map<String, Object>> insufficientStock;
        try {
            insufficientStock = checkIngredientStock(ingredientTotals);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", e.getMessage()));
        }

        if (!insufficientStock.isEmpty()) {
            Set<Integer> blockedRecipeIds = getBlockedRecipeIds(basketItems, insufficientStock);
            if (userId != null) {
                for (Integer id : blockedRecipeIds) {
                    basketRepository.deleteByUserIdAndRecipeId(userId, id);
                }
            }
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("error", "Some recipes are not available", "blockedRecipesIds", blockedRecipeIds)
            );
        }
        return null;
    }
    private ResponseEntity<?> placeIngredientOrders(Map<Integer, Integer> ingredientTotals, User user, List<OrderRequest> successfulOrders) {
        List<String> errors = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        for (Map.Entry<Integer, Integer> entry : ingredientTotals.entrySet()) {
            int ingredientId = entry.getKey();
            int amount = entry.getValue();

            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    int supplierId = ingredientRepository.findById(ingredientId)
                            .map(Ingredient::getSupplierId).filter(Objects::nonNull).orElse(10);

                    String supplierUrl = supplierRepository.findById(supplierId)
                            .orElseThrow(() -> new RuntimeException("Supplier not found")).getUrl();

                    OrderRequest orderRequest = new OrderRequest();
                    orderRequest.setIngredientId(
                            ingredientRepository.findById(ingredientId)
                                    .orElseThrow(() -> new RuntimeException("Ingredient not found"))
                                    .getIngredientId()
                    );
                    orderRequest.setAmount(amount);
                    orderRequest.setUser(user);

                    HttpEntity<OrderRequest> requestEntity = new HttpEntity<>(orderRequest);
                    ResponseEntity<String> response = restTemplate.postForEntity(
                            supplierUrl + "/order/", requestEntity, String.class);

                    if (response.getStatusCode().is2xxSuccessful()) {
                        synchronized (successfulOrders) {
                            successfulOrders.add(orderRequest);
                        }
                        return true;
                    } else {
                        errors.add("Order failed for ingredient " + ingredientId + ": " + response.getBody());
                        return false;
                    }
                } catch (Exception e) {
                    errors.add("Error placing order for ingredient " + ingredientId + ": " + e.getMessage());
                    return false;
                }
            }, executor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        if (futures.stream().anyMatch(f -> !f.join())) {
            revertSuccessfulOrders(successfulOrders);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                    Map.of("error", "Some ingredient orders failed", "details", errors)
            );
        }

        return null;
    }
    private void saveOrder(int userId, List<BasketItem> basketItems) {
        Order order = new Order();
        order.setUserId(userId);
        order.setTimestamp(LocalDateTime.now());

        List<OrderRecipe> orderRecipes = new ArrayList<>();
        for (BasketItem basketItem : basketItems) {
            Recipe recipe = recipeRepository.getById(basketItem.getRecipeId());

            OrderRecipe or = new OrderRecipe();
            or.setOrder(order);
            or.setRecipe(recipe);
            or.setQuantity(basketItem.getQuantity());

            orderRecipes.add(or);
        }

        order.setOrderRecipes(orderRecipes);
        orderRepository.save(order);
    }
    private Set<Integer> getBlockedRecipeIds(List<BasketItem> basketItemItems, List<Map<String, Object>> insufficientStock) {
        Set<Integer> blockedRecipeIds = new HashSet<>();

        for (BasketItem basketItem : basketItemItems) {
            Recipe recipe = recipeRepository.getById(basketItem.getRecipeId());
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

        return blockedRecipeIds;
    }
}