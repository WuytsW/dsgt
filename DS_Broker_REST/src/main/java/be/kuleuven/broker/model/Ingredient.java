package be.kuleuven.broker.model;

import javax.persistence.*;

@Entity
@Table(name = "ingredients")
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String ingredient;

    @Column(name = "supplierID")
    private Integer supplierId;

    @Column(name = "ingredientId")
    private Integer ingredientId;


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
    public Integer getIngredientId() {
        return ingredientId;
    }
    public void setIngredientId(Integer ingredientId) {
        this.ingredientId = ingredientId;
    }
}
