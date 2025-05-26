package be.kuleuven.broker.controller;

import be.kuleuven.broker.repository.BasketRepository;
import be.kuleuven.broker.model.BasketItem;
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
    public List<BasketItem> getBasket(@PathVariable String userId) {
        return basketRepository.getBasket(userId);
    }

    @PostMapping("/{userId}/add")
    public void addToBasket(@PathVariable String userId, @RequestBody BasketItem item) {
        basketRepository.addItem(userId, item);
    }

    @PostMapping("/{userId}/remove")
    public void removeFromBasket(@PathVariable String userId, @RequestBody BasketItem item) {
        basketRepository.removeItem(userId, item);
    }
}



