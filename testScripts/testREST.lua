-- Load required files
local json_new_meal = [[
{
    "name": "Double Cheese",
    "kcal": 2000,
    "price": 10.0,
    "description": "Double Cheese Burger",
    "mealType": "MEAT"
}]]
local json_update_meal = [[
{
    "id": "cfd1601f-29a0-485d-8d21-7607ec0340c8",
    "name": "Fish and Chips and Mayo",
    "kcal": 1000,
    "price": 6.0,
    "description": "Fried fish with chips with mayo",
    "mealType": "FISH"
}]]
local json_order = [[
{
    "address": "Parkstraat 53, Leuven 3000, BE",
    "meals": ["4237681a-441f-47fc-a747-8e0169bacea1", "5268203c-de76-4921-a3e3-439db69c462a"]
}
]]

-- Define possible requests
local requests = {
    { method = "POST", path = "/rest/meals", body = json_new_meal },
    { method = "PUT", path = "/rest/meals/cfd1601f-29a0-485d-8d21-7607ec0340c8", body = json_update_meal },
    { method = "DELETE", path = "/rest/meals/cfd1601f-29a0-485d-8d21-7607ec0340c8", body = "" },
    { method = "POST", path = "/rest/order", body = json_order }
}

-- Function to select a random request
request = function()
    local req = requests[math.random(1, #requests)]
    wrk.method = req.method
    wrk.body = req.body
    wrk.headers["Content-Type"] = "application/json"
    return wrk.format(req.method, req.path, wrk.headers, req.body)
end

-- Track success/failures
response = function(status, headers, body)
    -- Only track failures to avoid shared variable issues
    if status < 200 or status >= 300 then
        print("FAIL")
    end
end

-- Print results at the end of the test
done = function(summary, latency, requests)
    io.write("\n--- Test Results ---\n")
    io.write("Total Requests: ", summary.requests, "\n")
    io.write("Average Latency: ", latency.mean / 1000, " ms\n")
    io.write("Max Latency: ", latency.max / 1000, " ms\n")
    io.write("Requests Per Second: ", summary.requests / (summary.duration / 1000000), "\n")
end

