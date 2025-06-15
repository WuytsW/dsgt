package be.kuleuven.foodrestservice.domain;

import java.util.List;
import java.util.Objects;

public class OrderConfirmation {

    protected String address;
    protected List<String> meals;
    protected Double totalPrice;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<String> getMeals() {
        return meals;
    }

    public void setMeals(List<String> meals) {
        this.meals = meals;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderConfirmation orderConfirmation = (OrderConfirmation) o;
        return Objects.equals(address, orderConfirmation.address) &&
                Objects.equals(meals, orderConfirmation.meals) &&
                Objects.equals(totalPrice, orderConfirmation.totalPrice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, meals, totalPrice);
    }
}
