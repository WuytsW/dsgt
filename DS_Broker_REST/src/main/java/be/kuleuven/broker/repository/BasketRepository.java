package be.kuleuven.broker.repository;

import be.kuleuven.broker.model.BasketItem;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class BasketRepository {

    private final Map<String, List<BasketItem>> baskets = new HashMap<>();

    public List<BasketItem> getBasket(String userId) {
        return baskets.getOrDefault(userId, new ArrayList<>());
    }

    public void addItem(String userId, BasketItem newItem) {
        List<BasketItem> basket = baskets.computeIfAbsent(userId, k -> new ArrayList<>());

        for (BasketItem item : basket) {
            if (item.getRecipeId().equals(newItem.getRecipeId())) {
                item.setQuantity(item.getQuantity() + newItem.getQuantity());
                return;
            }
        }

        basket.add(newItem);
    }


    public void removeItem(String userId, BasketItem oldItem) {
        List<BasketItem> basket = baskets.get(userId);
        if (basket != null) {
            for (int i = 0; i < basket.size(); i++) {
                BasketItem item = basket.get(i);
                if (item.getRecipeId().equals(oldItem.getRecipeId())) {
                    if (item.getQuantity() > 1) {
                        item.setQuantity(item.getQuantity() - 1);
                    } else {
                        basket.remove(i);
                    }
                    break;
                }
            }
        }
    }
}

