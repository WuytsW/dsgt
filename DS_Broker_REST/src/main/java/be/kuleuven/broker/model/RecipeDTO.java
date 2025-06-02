package be.kuleuven.broker.model;

public class RecipeDTO {
    private int id;
    private String recipe;

    public RecipeDTO(int id, String recipe) {
        this.id = id;
        this.recipe = recipe;
    }

    public int getId() {
        return id;
    }

    public String getRecipe() {
        return recipe;
    }
}

