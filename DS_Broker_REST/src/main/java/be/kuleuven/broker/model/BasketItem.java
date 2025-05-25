package be.kuleuven.broker.model;

public class BasketItem {
    private String recipeId;
    private int quantity;

    public BasketItem() {}

    public BasketItem(String recipeId, int quantity) {
        this.recipeId = recipeId;
        this.quantity = quantity;
    }

    public String getRecipeId() { return recipeId; }
    public void setRecipeId(String recipeId) { this.recipeId = recipeId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}

