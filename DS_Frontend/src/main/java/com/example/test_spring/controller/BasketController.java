package com.example.test_spring.controller;

import com.example.test_spring.model.BasketItem;
import com.example.test_spring.model.Recipe;
import com.example.test_spring.service.BasketService;
import com.example.test_spring.service.RecipeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.naming.InsufficientResourcesException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class BasketController {

    private final BasketService basketService;
    private final RecipeService recipeService;

    public BasketController(BasketService basketService, RecipeService recipeService) {
        this.basketService = basketService;
        this.recipeService = recipeService;
    }

    @GetMapping("/basket")
    public String viewBasket(Model model, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("user");
        List<BasketItem> basket;
        if (userId == null) {
            basket = getOrCreateBasket(session);
        }else{
            basket = basketService.getBasket(userId);
        }

        model.addAttribute("basket", basket != null ? basket : new ArrayList<>());
        return "basket";
    }

    @PostMapping("/add-recipe-to-basket")
    public String addRecipeToBasket(@RequestParam int recipeId, @RequestParam int quantity, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("user");
        if (userId == null) {
            List<BasketItem> basket = getOrCreateBasket(session);
            Recipe recipe = recipeService.getRecipeById(recipeId);
            basket.add(new BasketItem(recipe, quantity));
            session.setAttribute("basket", basket);
        }else {
            basketService.addToBasket(userId, new BasketItem(recipeId, quantity));
        }
        return "redirect:/basket";
    }

    @PostMapping("/remove-recipe-from-basket")
    public String removeRecipeFromBasket(@RequestParam int recipeId, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("user");
        if (userId == null) {
            List<BasketItem> basket = getOrCreateBasket(session);
            Iterator<BasketItem> iterator = basket.iterator();

            while (iterator.hasNext()) {
                BasketItem basketItem = iterator.next();
                Integer id = Integer.valueOf(recipeId);

                if (basketItem.getRecipe().getId().equals(id)) {
                    basketItem.setQuantity(basketItem.getQuantity() - 1);
                    if (basketItem.getQuantity() <= 0) {
                        iterator.remove();
                    }
                    break;
                }
            }
            session.setAttribute("basket", basket);
        }else {
            basketService.removeFromBasket(userId, new BasketItem(recipeId, 1));
        }
        return "redirect:/basket";
    }

    @SuppressWarnings("unchecked")
    private List<BasketItem> getOrCreateBasket(HttpSession session) {
        List<BasketItem> basket = (List<BasketItem>) session.getAttribute("basket");
        if (basket == null) {
            basket = new ArrayList<>();
            session.setAttribute("basket", basket);
        }
        return basket;
    }
}
