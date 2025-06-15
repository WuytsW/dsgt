package com.example.test_spring.service;

import com.example.test_spring.model.BasketItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
public class BasketService {

    private final RestTemplate restTemplate = new RestTemplate();
    private String url;
    private final RecipeService recipeService;

    public BasketService(@Value("${broker.url}") String brokerUrl, RecipeService recipeService) {
        url = brokerUrl + "/basket";
        this.recipeService = recipeService;
    }

    public List<BasketItem> getBasket(Integer userId) {
        try {
            BasketItem[] items = restTemplate.getForObject(url + "/" + userId, BasketItem[].class);
            if (items == null) return List.of();

            return Arrays.stream(items)
                    .peek(item -> item.setRecipe(recipeService.getRecipeById(item.getRecipeId())))
                    .toList();
        }catch (RestClientException e) {
            return List.of();
        }
    }


    public void addToBasket(Integer userId, BasketItem item) {
        try {
            restTemplate.postForObject(url + "/" + userId + "/add", item, Void.class);
        }catch (RestClientException e){
            e.printStackTrace();
            throw new RuntimeException("Unable to reach broker. Please try again later.");
        }
    }

    public void removeFromBasket(Integer userId, BasketItem item) {
        try {
            restTemplate.postForObject(url + "/" + userId + "/remove", item, Void.class);
        }catch (RestClientException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to reach broker. Please try again later.");
        }
    }
}

