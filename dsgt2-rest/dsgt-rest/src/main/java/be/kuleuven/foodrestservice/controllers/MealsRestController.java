package be.kuleuven.foodrestservice.controllers;

import be.kuleuven.foodrestservice.domain.Meal;
import be.kuleuven.foodrestservice.domain.MealsRepository;
import be.kuleuven.foodrestservice.domain.Order;
import be.kuleuven.foodrestservice.domain.OrderConfirmation;
import be.kuleuven.foodrestservice.exceptions.MealNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
public class MealsRestController {

    private final MealsRepository mealsRepository;

    @Autowired
    MealsRestController(MealsRepository mealsRepository) {
        this.mealsRepository = mealsRepository;
    }

    @GetMapping("/rest/meals/{id}")
    EntityModel<Meal> getMealById(@PathVariable String id) {
        Meal meal = mealsRepository.findMeal(id).orElseThrow(() -> new MealNotFoundException(id));

        return mealToEntityModel(id, meal);
    }

    @GetMapping("/rest/meals")
    CollectionModel<EntityModel<Meal>> getMeals() {
        Collection<Meal> meals = mealsRepository.getAllMeal();

        List<EntityModel<Meal>> mealEntityModels = new ArrayList<>();
        for (Meal m : meals) {
            EntityModel<Meal> em = mealToEntityModel(m.getId(), m);
            mealEntityModels.add(em);
        }
        return CollectionModel.of(mealEntityModels,
                linkTo(methodOn(MealsRestController.class).getMeals()).withSelfRel());
    }

    private EntityModel<Meal> mealToEntityModel(String id, Meal meal) {
        return EntityModel.of(meal,
                linkTo(methodOn(MealsRestController.class).getMealById(id)).withSelfRel(),
                linkTo(methodOn(MealsRestController.class).getMeals()).withRel("rest/meals"));
    }

    @GetMapping("/rest/cheapest")
    EntityModel<Meal> getCheapestMeal() {
        Meal cheapest = mealsRepository.getCheapestMeal().orElseThrow();

        return mealToEntityModel(cheapest.getId(), cheapest);
    }

    @GetMapping("/rest/largest")
    EntityModel<Meal> getLargestMeal() {
        Meal largest = mealsRepository.getLargestMeal().orElseThrow();

        return mealToEntityModel(largest.getId(), largest);
    }

    @PostMapping("/rest/meals")
    void addMeal(@RequestBody Meal meal) {
        mealsRepository.addMeal(meal);
    }

    @PutMapping("/rest/meals/{id}")
    void updateMeal(@PathVariable String id, @RequestBody Meal meal) {
        mealsRepository.updateMeal(id, meal);
    }

    @DeleteMapping("/rest/meals/{id}")
    void deleteMeal(@PathVariable String id) {
        mealsRepository.deleteMeal(id);
    }

    private EntityModel<OrderConfirmation> orderConfirmationToEntityModel(OrderConfirmation orderConfirmation) {
        return EntityModel.of(orderConfirmation,
                linkTo(methodOn(MealsRestController.class).orderMeal(null)).withSelfRel(),
                linkTo(methodOn(MealsRestController.class).getMeals()).withRel("rest/meals"));
    }

    @PostMapping("/rest/order")
    EntityModel<OrderConfirmation> orderMeal(@RequestBody Order order) {
        OrderConfirmation orderConfirmation = mealsRepository.orderMeal(order).orElseThrow();

        return orderConfirmationToEntityModel(orderConfirmation);
    }
}
