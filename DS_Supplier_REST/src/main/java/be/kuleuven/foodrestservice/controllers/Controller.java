package be.kuleuven.foodrestservice.controllers;

import be.kuleuven.foodrestservice.model.Address;
import be.kuleuven.foodrestservice.model.Ingredient;
import be.kuleuven.foodrestservice.model.OrderRequest;
import be.kuleuven.foodrestservice.model.User;
import be.kuleuven.foodrestservice.repository.IngredientRepository;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.*;
import com.azure.cosmos.util.CosmosPagedIterable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.*;


@RestController
public class Controller {

    private final Map<Integer, Integer> stockMap = new HashMap<>();


    @Value("${cosmos.endpoint-uri}")
    private String endpointUri;

    @Value("${cosmos.primary-key}")
    private String primaryKey;

    @Value("${cosmos.database-id}")
    private String databaseId;
    private static final String CONTAINER_ID = "ingredients"; // e.g., "Ingredients"

    CosmosClient client = null;
    private CosmosContainer container = null;


    public Controller() {
        // empty
    }

    @PostConstruct
    public void initializeCosmosDbClient(){
        try{
            System.out.println("Initializing Cosmos DB client...");
            // connect client to the db
            client = new CosmosClientBuilder()
                    .endpoint(endpointUri)
                    .key(primaryKey)
                    .buildClient();
            System.out.println("Cosmos DB client initialized.");

            // set the container
            container = client.getDatabase(databaseId).getContainer(CONTAINER_ID);
            System.out.println("Using database: " + databaseId + ", container: " + container.getId());
        }
        catch (Exception e){
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @GetMapping("/stock")
    public ResponseEntity<?> getAllStock() {
        if (this.container == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Cosmos DB container not initialized. Check application logs."));
        }

        List<Map<String, Object>> ingredientsDetailsList = new ArrayList<>();
        // query to get all the data (with stock) from the database supplier
        String sqlQuery = "SELECT c.ingredientId, c.ingredient, c.stock, c.supplier FROM c";
        CosmosQueryRequestOptions queryOptions = new CosmosQueryRequestOptions();

        try {
            CosmosPagedIterable<Map> items = container.queryItems(sqlQuery, queryOptions, Map.class);
            for (Map item : items) {
                ingredientsDetailsList.add(item);
            }
            return ResponseEntity.ok(ingredientsDetailsList);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve stock from database."));
        }
    }


    @GetMapping("/stock/{ingredientId}")
    public ResponseEntity<?> getStock(@PathVariable Integer ingredientId) {
        if (this.container == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Cosmos DB container not initialized. Check application logs."));
        }
        // ingredientID in cosmos db is a string, so convert it
        String ingredientIdString = String.valueOf(ingredientId);

        String sqlQuery = "SELECT VALUE c.stock FROM c WHERE c.ingredientId = @ingredientId";
        SqlParameter param = new SqlParameter("@ingredientId", ingredientIdString);
        SqlQuerySpec querySpec = new SqlQuerySpec(sqlQuery, Collections.singletonList(param));

        CosmosQueryRequestOptions queryOptions = new CosmosQueryRequestOptions();

        try {
            CosmosPagedIterable<String> items = container.queryItems(querySpec, queryOptions, String.class);

            String stockValueString = null;
            if (items.iterator().hasNext()) {
                stockValueString = items.iterator().next();
            }

            if (stockValueString != null) {
                try {
                    Integer stockValue = Integer.parseInt(stockValueString);
                    return ResponseEntity.ok(Map.of("stock", stockValue));
                } catch (NumberFormatException nfe) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "Stock data for ingredient " + ingredientIdString + " is corrupted (not a number)."));
                }
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Ingredient with ID " + ingredientIdString + " not found."));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve stock for ingredient " + ingredientIdString + " from database."));
        }
    }

    @PostMapping("/order")
    public ResponseEntity<?> placeOrder(@RequestBody OrderRequest request) {
        if (this.container == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Cosmos DB container not initialized. Check application logs."));
        }

        Integer requestedIngredientId = request.getIngredientId();
        if (requestedIngredientId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "ingredientId is required in the order request."));
        }
        String ingredientIdString = String.valueOf(requestedIngredientId);
        int amountToOrder = request.getAmount();

        if (amountToOrder <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Order amount must be positive."));
        }

        // query to get the full item
        String sqlQuery = "SELECT * FROM c WHERE c.ingredientId = @ingredientId";
        SqlParameter param = new SqlParameter("@ingredientId", ingredientIdString);
        SqlQuerySpec querySpec = new SqlQuerySpec(sqlQuery, Collections.singletonList(param));

        CosmosQueryRequestOptions queryOptions = new CosmosQueryRequestOptions();
        // set partition key for efficient query
        queryOptions.setPartitionKey(new PartitionKey(ingredientIdString));

        try {
            System.out.println("Processing order for ingredientId: " + ingredientIdString + ", amount: " + amountToOrder);
            CosmosPagedIterable<Map> items = container.queryItems(querySpec, queryOptions, Map.class);

            Map<String, Object> itemToUpdate;
            try {
                itemToUpdate = items.iterator().next(); // Get the first (and supposedly only) item
            } catch (NoSuchElementException nsee) {
                System.out.println("Ingredient with ID " + ingredientIdString + " not found in Cosmos DB for order.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Ingredient with ID " + ingredientIdString + " not found."));
            }

            String documentId = itemToUpdate.get("id").toString(); // Cosmos DB's unique document ID
            String stockString = itemToUpdate.get("stock").toString();
            int currentStock;

            try {
                currentStock = Integer.parseInt(stockString);
            } catch (NumberFormatException nfe) {
                System.err.println("Error parsing stock value '" + stockString + "' to integer for ingredientId " + ingredientIdString + ": " + nfe.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Stock data for ingredient " + ingredientIdString + " is corrupted."));
            }

            if (currentStock < amountToOrder) {
                System.out.println("Insufficient stock for ingredientId " + ingredientIdString + ". Requested: " + amountToOrder + ", Available: " + currentStock);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Not enough stock for ingredient " + ingredientIdString + ". Available: " + currentStock + ", Requested: " + amountToOrder));
            }

            // calculate new stock and update the item map
            int newStock = currentStock - amountToOrder;
            itemToUpdate.put("stock", String.valueOf(newStock));

            // replace the item in Cosmos DB
            CosmosItemResponse<Map> response = container.replaceItem(
                    itemToUpdate,
                    documentId,
                    new PartitionKey(ingredientIdString), //
                    new CosmosItemRequestOptions());

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                User user = request.getUser();
                Address address = user.getAddress();
                String addressString = address.getCountry() + ", " + address.getPostcode() + ", " + address.getStreet() + " " + address.getStreetNumber();
                System.out.printf("""
            ✅ Order Confirmed
            - Ingredient ID: %d
            - Amount: %d
            - Customer Email: %s
            - Shipping Address: %s
            - Remaining Stock: %d
            --------------------------------------------
            """, requestedIngredientId, amountToOrder, user.getEmail(), addressString, newStock);
                return ResponseEntity.ok(Map.of(
                        "message", "Order processed successfully",
                        "ingredientId", requestedIngredientId,
                        "remainingStock", newStock
                ));
            } else {
                System.err.println("Failed to update stock for ingredientId " + ingredientIdString + ". Status code: " + response.getStatusCode());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to update stock in database. Status: " + response.getStatusCode()));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process order for ingredient " + ingredientIdString + "."));
        }
    }


    @PostMapping("/revert")
    public ResponseEntity<?> revertOrder(@RequestBody OrderRequest request) {
        if (this.container == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Cosmos DB container not initialized. Check application logs."));
        }

        Integer requestedIngredientId = request.getIngredientId();
        if (requestedIngredientId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "ingredientId is required in the revert request."));
        }
        String ingredientIdString = String.valueOf(requestedIngredientId);
        int amountToRevert = request.getAmount();

        String sqlQuery = "SELECT * FROM c WHERE c.ingredientId = @ingredientId";
        SqlParameter param = new SqlParameter("@ingredientId", ingredientIdString);
        SqlQuerySpec querySpec = new SqlQuerySpec(sqlQuery, Collections.singletonList(param));

        CosmosQueryRequestOptions queryOptions = new CosmosQueryRequestOptions();
        queryOptions.setPartitionKey(new PartitionKey(ingredientIdString));

        try {
            System.out.println("Processing revert for ingredientId: " + ingredientIdString + ", amount: " + amountToRevert);
            CosmosPagedIterable<Map> items = container.queryItems(querySpec, queryOptions, Map.class);

            Map<String, Object> itemToUpdate;
            try {
                itemToUpdate = items.iterator().next(); // Get the first (and supposedly only) item
            } catch (NoSuchElementException nsee) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Ingredient with ID " + ingredientIdString + " not found."));
            }

            String documentId = itemToUpdate.get("id").toString(); // Cosmos DB's unique document ID
            Object stockObject = itemToUpdate.get("stock");

            if (stockObject == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Stock data for ingredient " + ingredientIdString + " is missing or corrupted."));
            }

            String stockString = stockObject.toString();
            int currentStock;

            try {
                currentStock = Integer.parseInt(stockString);
            } catch (NumberFormatException nfe) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Stock data for ingredient " + ingredientIdString + " is corrupted (not a number)."));
            }

            // Calculate new stock by adding the reverted amount
            int newStock = currentStock + amountToRevert;
            itemToUpdate.put("stock", String.valueOf(newStock));

            // Replace the item in Cosmos DB
            CosmosItemResponse<Map> response = container.replaceItem(
                    itemToUpdate,
                    documentId,
                    new PartitionKey(ingredientIdString),
                    new CosmosItemRequestOptions());

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                return ResponseEntity.ok(Map.of(
                        "message", "Revert processed successfully and stock updated",
                        "ingredientId", requestedIngredientId,
                        "remainingStock", newStock
                ));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to update stock in database during revert. Status: " + response.getStatusCode()));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process revert for ingredient " + ingredientIdString + "."));
        }
    }



}
