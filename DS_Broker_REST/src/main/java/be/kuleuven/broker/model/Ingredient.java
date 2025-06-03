package be.kuleuven.broker.model;

import javax.persistence.*;

@Entity
@Table(name = "ingredients")
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String ingredient;

    private Integer supplierId;

    private Integer ingredientId_S;


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
    public Integer getSupplierId() {
        return supplierId;
    }
    public void setSupplierId(Integer suplierid) {
        this.supplierId = suplierid;
    }
    public Integer getIngredientId_S() {
        return ingredientId_S;
    }
    public void setIngredientId_S(Integer ingredientId_S) {
        this.ingredientId_S = ingredientId_S;
    }
}
