package be.kuleuven.broker.model;

import java.util.List;

public class Recipe {
    private String id;
    private String name;
    private String description;
    private String imageUrl;
    private List<String> ingredients;

    public Recipe(String id, String name, String description, String imageUrl, List<String> ingredients) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.ingredients = ingredients;
    }

    public Recipe() {}

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public List<String> getIngredients() { return ingredients; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }
}

