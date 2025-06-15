package com.example.test_spring.model;

import java.util.List;

public class Recipe {
    private Integer id;
    private String recipe;
    private String description;
    private List<Ingredient> ingredients;
    private String image;


    public Recipe(Integer id, String recipe, String description, List<Ingredient> ingredients, String image) {
        this.id = id;
        this.recipe = recipe;
        this.description = description;
        this.ingredients = ingredients;
        this.image = image;
    }

    public Integer getId() { return id; }
    public String getRecipe() { return recipe; }
    public String getDescription() { return description; }
    public List<Ingredient> getIngredients() { return ingredients; }
    public String getImage() {
        return image;
    }
}
