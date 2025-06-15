package be.kuleuven.foodrestservice.domain;

import java.util.List;
import java.util.Objects;

public class Order {

    protected String address;
    protected List<String> meals;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(address, order.address) &&
                Objects.equals(meals, order.meals);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, meals);
    }
}
