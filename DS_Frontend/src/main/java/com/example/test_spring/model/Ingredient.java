package com.example.test_spring.model;

public class Ingredient {
    private int id;
    private String ingredient;

    Ingredient(int id, String ingredient) {
        this.id = id;
        this.ingredient = ingredient;
    }

    public String getIngredient() {
        return ingredient;
    }
    public void setIngredient(String ingredient) {
        this.ingredient = ingredient;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
}

