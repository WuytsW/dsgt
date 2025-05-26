package be.kuleuven.broker.controller;

import be.kuleuven.broker.repository.RecipeRepository;
import be.kuleuven.broker.model.Recipe;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/recipes")
public class RecipeController {

    private final RecipeRepository recipeRepository;

    public RecipeController(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    @GetMapping
    public Collection<Recipe> getAllRecipes() {
        return recipeRepository.getAllRecipes();
    }

    @GetMapping("/{id}")
    public Recipe getRecipe(@PathVariable String id) {
        return recipeRepository.getRecipe(id);
    }
}

