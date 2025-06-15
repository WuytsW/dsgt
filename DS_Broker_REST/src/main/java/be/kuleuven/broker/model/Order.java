package be.kuleuven.broker.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "userID")
    private Integer userId;

    private LocalDateTime timestamp;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<OrderRecipe> orderRecipes = new ArrayList<>();

    public Integer getUserId() {
        return userId;
    }
    public void setUserId(Integer userId) {
        this.userId = userId;
    }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    public List<OrderRecipe> getOrderRecipes() {
        return orderRecipes;
    }
    public void setOrderRecipes(List<OrderRecipe> orderRecipes) {
        this.orderRecipes = orderRecipes;
    }
    public Integer getId() {
        return id;
    }
}
