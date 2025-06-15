package com.example.test_spring.controller;

import com.example.test_spring.model.Order;
import com.example.test_spring.model.Recipe;
import com.example.test_spring.service.RecipeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class MainController {

    private final RecipeService recipeService;
    private final String brokerUrl;
    RestTemplate restTemplate = new RestTemplate();

    public MainController(RecipeService recipeService, @Value("${broker.url}") String brokerUrl) {
        this.recipeService = recipeService;
        this.brokerUrl = brokerUrl;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("recipes", recipeService.getAllRecipes());
        return "index";
    }

    @GetMapping("/recipe/{id}")
    public String recipeDetail(@PathVariable int id, Model model) {
        Recipe recipe = recipeService.getRecipeById(id);
        model.addAttribute("recipe", recipe);
        return "recipe";
    }

    @GetMapping("/admin")
    public String Admin(Model model){
        String url = brokerUrl + "/orders";

        try {
            ResponseEntity<Order[]> response = restTemplate.getForEntity(url, Order[].class);
            List<Order> allOrders = List.of(response.getBody());
            List<Order> sortedOrders =
                    allOrders.stream()
                    .sorted(Comparator.comparing(Order::getTimestamp).reversed())
                    .collect(Collectors.toList());
            model.addAttribute("orders", sortedOrders);
        } catch (Exception e) {
            model.addAttribute("orders", Collections.emptyList());
        }

        return "admin";
    }
}
