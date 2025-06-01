package be.kuleuven.foodrestservice.model;


public class Ingredient {

    private Integer id;

    private String ingredient;

    private Integer stock;


    public Ingredient() {}

    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public String getIngredient() {return ingredient; }
    public void setIngredient(String ingredient) {
        this.ingredient = ingredient;
    }
    public Integer getStock() {
        return stock;
    }
    public void setStock(Integer suplierid) {
        this.stock = suplierid;
    }
}
