package be.kuleuven.foodrestservice.controllers;

import be.kuleuven.foodrestservice.domain.Meal;
import be.kuleuven.foodrestservice.domain.MealsRepository;
import be.kuleuven.foodrestservice.domain.Order;
import be.kuleuven.foodrestservice.domain.OrderConfirmation;
import be.kuleuven.foodrestservice.exceptions.MealNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@RestController
public class MealsRestRpcStyleController {

    private final MealsRepository mealsRepository;

    @Autowired
    MealsRestRpcStyleController(MealsRepository mealsRepository) {
        this.mealsRepository = mealsRepository;
    }

    @GetMapping("/restrpc/meals/{id}")
    Meal getMealById(@PathVariable String id) {
        Optional<Meal> meal = mealsRepository.findMeal(id);

        return meal.orElseThrow(() -> new MealNotFoundException(id));
    }

    @GetMapping("/restrpc/meals")
    Collection<Meal> getMeals() {
        return mealsRepository.getAllMeal();
    }

    @GetMapping("/restrpc/cheapest")
    Meal getCheapestMeal() {
        return mealsRepository.getCheapestMeal().orElseThrow();
    }

    @GetMapping("/restrpc/largest")
    Meal getLargestMeal() {
        return mealsRepository.getLargestMeal().orElseThrow();
    }

    @PostMapping("/restrpc/meals")
    void addMeal(@RequestBody Meal meal) {
        mealsRepository.addMeal(meal);
    }

    @PutMapping("/restrpc/meals/{id}")
    void updateMeal(@PathVariable String id, @RequestBody Meal meal) {
        mealsRepository.updateMeal(id, meal);
    }

    @DeleteMapping("/restrpc/meals/{id}")
    void deleteMeal(@PathVariable String id) {
        mealsRepository.deleteMeal(id);
    }

    @PostMapping("/restrpc/order")
    OrderConfirmation orderMeal(@RequestBody Order order) {
        return mealsRepository.orderMeal(order).orElseThrow();
    }
}
