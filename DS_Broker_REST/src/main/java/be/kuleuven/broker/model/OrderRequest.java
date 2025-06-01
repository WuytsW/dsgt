package be.kuleuven.broker.model;

public class OrderRequest {
    private Integer ingredientId;
    private int amount;


    public Integer getIngredientId() {
        return ingredientId;
    }
    public void setIngredientId(Integer ingredientId) {
        this.ingredientId = ingredientId;
    }
    public int getAmount() {
        return amount;
    }
    public void setAmount(int amount) {
        this.amount = amount;
    }
}


