package be.kuleuven.broker.controller;

import be.kuleuven.broker.model.BasketItem;
import be.kuleuven.broker.repository.BasketRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/basket")
public class BasketController {

    private final BasketRepository basketRepository;

    public BasketController(BasketRepository basketRepository) {
        this.basketRepository = basketRepository;
    }

    @GetMapping("/{userId}")
    public List<BasketItem> getBasketByUserId(@PathVariable int userId) {
        return basketRepository.findByUserId(userId);
    }

    @PostMapping("/{userId}/add")
    public BasketItem addToBasket(@PathVariable int userId, @RequestBody BasketItem incoming) {
        BasketItem existing = basketRepository.findByUserIdAndRecipeId(userId, incoming.getRecipeId());

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + incoming.getQuantity());
            return basketRepository.save(existing);
        } else {
            BasketItem newItem = new BasketItem(userId, incoming.getRecipeId(), incoming.getQuantity());
            return basketRepository.save(newItem);
        }
    }

    @PostMapping("/{userId}/remove")
    public ResponseEntity<Void> removeFromBasket(@PathVariable int userId, @RequestBody BasketItem incoming) {
        BasketItem existing = basketRepository.findByUserIdAndRecipeId(userId, incoming.getRecipeId());

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

    @DeleteMapping("/{userId}")
    public void deleteBasketByUserId(@PathVariable int userId) {
        basketRepository.deleteByUserId(userId);
    }
}
