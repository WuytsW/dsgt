package be.kuleuven.broker.controller;

import be.kuleuven.broker.model.Basket;
import be.kuleuven.broker.repository.BasketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/basket")
public class BasketController {

    @Autowired
    private BasketRepository basketRepository;

    // Get all basket items for a user
    @GetMapping("/{userId}")
    public List<Basket> getBasketByUserId(@PathVariable int userId) {
        return basketRepository.findByUserId(userId);
    }

    @PostMapping("/{userId}/add")
    public Basket addToBasket(@PathVariable int userId, @RequestBody Basket incoming) {
        Basket existing = basketRepository.findByUserIdAndRecipeId(userId, incoming.getRecipeId());

        if (existing != null) {
            // If already in basket, increase quantity
            existing.setQuantity(existing.getQuantity() + incoming.getQuantity());
            return basketRepository.save(existing);
        } else {
            // Else, insert new item
            Basket newItem = new Basket();
            newItem.setUserId(userId);
            newItem.setRecipeId(incoming.getRecipeId());
            newItem.setQuantity(incoming.getQuantity());
            return basketRepository.save(newItem);
        }
    }

    @PostMapping("/{userId}/remove")
    public ResponseEntity<Void> removeFromBasket(@PathVariable int userId, @RequestBody Basket incoming) {
        Basket existing = basketRepository.findByUserIdAndRecipeId(userId, incoming.getRecipeId());

        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        int newQuantity = existing.getQuantity() - 1;
        if (newQuantity <= 0) {
            basketRepository.delete(existing);
        } else {
            existing.setQuantity(newQuantity);
            basketRepository.save(existing);
        }

        return ResponseEntity.ok().build();
    }







    // Clear all basket items for a user
    @DeleteMapping("/{userId}")
    public void deleteBasketByUserId(@PathVariable int userId) {
        basketRepository.deleteByUserId(userId);
    }
}




