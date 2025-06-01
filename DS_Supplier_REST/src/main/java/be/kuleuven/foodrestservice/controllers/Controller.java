package be.kuleuven.foodrestservice.controllers;

import be.kuleuven.foodrestservice.model.Ingredient;
import be.kuleuven.foodrestservice.model.OrderRequest;
import be.kuleuven.foodrestservice.repository.IngredientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;



@RestController
public class Controller {

    private final Map<Integer, Integer> stockMap = new HashMap<>();

    public Controller() {
        for (int i = 1; i <= 40; i++) {
            stockMap.put(i, 100);
        }
    }

    @GetMapping("/stock")
    public ResponseEntity<?> getAllStock() {
        return ResponseEntity.ok(stockMap);
    }


    @GetMapping("/stock/{ingredientId}")
    public ResponseEntity<?> getStock(@PathVariable Integer ingredientId) {
        Integer stock = stockMap.getOrDefault(ingredientId, 0);
        return ResponseEntity.ok(Map.of("stock", stock));
    }

    @PostMapping("/order")
    public ResponseEntity<?> placeOrder(@RequestBody OrderRequest request) {
        Integer ingredientId = request.getIngredientId();
        int amount = request.getAmount();

        System.out.println("Received order amount: " + amount);

        Integer currentStock = stockMap.get(ingredientId);
        if (currentStock == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Ingredient not found"));
        }

        if (currentStock < amount) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Not enough stock for ingredient " + ingredientId));
        }

        stockMap.put(ingredientId, currentStock - amount);

        return ResponseEntity.ok(Map.of(
                "message", "Order received and stock updated",
                "remainingStock", stockMap.get(ingredientId)
        ));
    }
}



/*
@RestController
public class Controller {

    IngredientRepository ingredientRepository;

    Controller(IngredientRepository ingredientRepository){
        this.ingredientRepository = ingredientRepository;
    }



    @GetMapping("/stock/{ingredientId}")
    @ResponseBody
    public ResponseEntity<?> getStock(@PathVariable Integer ingredientId) {
        Optional<Ingredient> optionalIngredient = ingredientRepository.findById(ingredientId);
        if (optionalIngredient.isPresent()) {
            Ingredient ingredient = optionalIngredient.get();
            return ResponseEntity.ok(
                Map.of(
                        "ingredient", ingredient.getIngredient(),
                        "stock", ingredient.getStock()
                )
            );
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Ingredient not found"));
        }
    }

    @PostMapping("/order")
    @ResponseBody
    public ResponseEntity<?> reduceStock(@RequestBody OrderRequest orderRequest) {
        Optional<Ingredient> optionalIngredient = ingredientRepository.findById(orderRequest.getIngredientId());
        if (optionalIngredient.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Ingredient not found"));
        }

        Ingredient ingredient = optionalIngredient.get();
        int currentStock = ingredient.getStock();
        int requested = orderRequest.getAmount();

        if (requested > currentStock) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Insufficient stock"));
        }

        ingredient.setStock(currentStock - requested);
        ingredientRepository.save(ingredient); // persists the updated stock

        return ResponseEntity.ok(Map.of(
                "ingredient", ingredient.getIngredient(),
                "remainingStock", ingredient.getStock()
        ));
    }





}

 */
