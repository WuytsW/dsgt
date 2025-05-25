package be.kuleuven.broker.domain;

import be.kuleuven.broker.model.BasketItem;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class BasketRepository {

    private final Map<String, List<BasketItem>> baskets = new HashMap<>();

    public List<BasketItem> getBasket(String userId) {
        return baskets.getOrDefault(userId, new ArrayList<>());
    }

    public void addItem(String userId, BasketItem item) {
        baskets.computeIfAbsent(userId, k -> new ArrayList<>()).add(item);
    }

    public void removeItem(String userId, BasketItem item) {
        List<BasketItem> basket = baskets.get(userId);
        if (basket != null) {
            basket.removeIf(i -> i.getRecipeId().equals(item.getRecipeId()));
        }
    }
}

