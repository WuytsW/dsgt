package com.example.test_spring.service;

import com.example.test_spring.model.Recipe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
public class RecipeService {
    private final RestTemplate restTemplate = new RestTemplate();
    private String url;

    public RecipeService(@Value("${broker.url}") String brokerUrl) {
        url = brokerUrl + "/recipes";
    }

    public List<Recipe> getAllRecipes() {
        try {
            Recipe[] recipes = restTemplate.getForObject(url, Recipe[].class);
            return recipes != null ? Arrays.asList(recipes) : List.of();
        }catch (RestClientException e) {
            return List.of();
        }
    }

    public Recipe getRecipeById(int id) {
        try {
            return restTemplate.getForObject(url + "/" + id, Recipe.class);
        }catch (RestClientException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to reach broker. Please try again later.");
        }
    }
}
