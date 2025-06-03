package be.kuleuven.broker.model;

import  javax.persistence.*;

@Entity
@Table(name = "basket")
public class BasketItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "userID")
    private int userId;

    @Column(name = "recipeID")
    private int recipeId;

    private int quantity;


    public BasketItem(){}
    public BasketItem(int userId, int recipeId, int quantity){
        this.userId = userId;
        this.recipeId = recipeId;
        this.quantity = quantity;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getRecipeId() { return recipeId; }
    public void setRecipeId(int recipeId) { this.recipeId = recipeId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
