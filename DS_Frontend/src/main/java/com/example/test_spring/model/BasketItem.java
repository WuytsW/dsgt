package com.example.test_spring.model;

public class BasketItem {
    private int id;
    private int recipeId;
    private int quantity;
    private Recipe recipe;

    public BasketItem() {}

    public BasketItem(int recipeId, int quantity) {
        this.recipeId = recipeId;
        this.quantity = quantity;
    }

    public BasketItem(Recipe recipe, int quantity) {
        this.recipe = recipe;
        this.quantity = quantity;

        this.recipeId = recipe.getId();
    }

    public int getRecipeId() { return recipeId; }
    public void setRecipeId(int recipeId) { this.recipeId = recipeId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public Recipe getRecipe() { return recipe; }
    public void setRecipe(Recipe recipe) { this.recipe = recipe; }
}


