package be.kuleuven.broker.domain;

import be.kuleuven.broker.model.Recipe;
import org.springframework.stereotype.Repository;
import javax.annotation.PostConstruct;
import org.springframework.web.client.RestTemplate;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

@Repository
public class RecipeRepository {

    private final Map<String, Recipe> recipes = new HashMap<>();

    @PostConstruct
    public void initData() {
        String url = "https://www.themealdb.com/api/json/v1/1/search.php?s=";
        String response = new RestTemplate().getForObject(url, String.class);
        JSONObject json = new JSONObject(response);
        JSONArray meals = json.getJSONArray("meals");

        for (int i = 0; i < meals.length(); i++) {
            JSONObject meal = meals.getJSONObject(i);
            String id = meal.getString("idMeal");
            String name = meal.getString("strMeal");
            String description = meal.optString("strInstructions", "");
            String imageUrl = meal.getString("strMealThumb");

            List<String> ingredients = new ArrayList<>();
            for (int j = 1; j <= 20; j++) {
                String ingredient = meal.optString("strIngredient" + j);
                if (ingredient != null && !ingredient.isBlank()) {
                    ingredients.add(ingredient);
                }
            }

            recipes.put(id, new Recipe(id, name, description, imageUrl, ingredients));
        }
    }

    public Collection<Recipe> getAllRecipes() {
        return recipes.values();
    }

    public Recipe getRecipe(String id) {
        return recipes.get(id);
    }
}

