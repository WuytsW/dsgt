package be.kuleuven.broker.model;

import java.util.List;

public class GuestOrder {
    private List<BasketItem> basket;
    private User guestUser;

    public GuestOrder(List<BasketItem> basket, User guestUser){
        this.basket = basket;
        this.guestUser = guestUser;
    }


    public List<BasketItem> getBasket() {
        return basket;
    }
    public void setBasket(List<BasketItem> basket) {
        this.basket = basket;
    }
    public User getGuestUser() {
        return guestUser;
    }
    public void setGuestUser(User guestUser) {
        this.guestUser = guestUser;
    }
}
