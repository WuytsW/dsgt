-- SOAP payloads
local getMealRequest = [[<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:gs="http://foodmenu.io/gt/webservice">
<soapenv:Header/>
<soapenv:Body>
<gs:getMealRequest>
<gs:name>Steak</gs:name>
</gs:getMealRequest>
</soapenv:Body>
</soapenv:Envelope>]]

local addOrderRequest = [[<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:gs="http://foodmenu.io/gt/webservice">
<soapenv:Header/>
<soapenv:Body>
<gs:addOrderRequest>
<gs:order>
<gs:address>123 Main St</gs:address>
<gs:meals>Steak</gs:meals>
<gs:meals>Portobello</gs:meals>
</gs:order>
</gs:addOrderRequest>
</soapenv:Body>
</soapenv:Envelope>]]

local getLargestMealRequest = [[<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:gs="http://foodmenu.io/gt/webservice">
<soapenv:Header/>
<soapenv:Body>
<gs:getLargestMealRequest/>
</soapenv:Body>
</soapenv:Envelope>]]

local getCheapestMealRequest = [[<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:gs="http://foodmenu.io/gt/webservice">
<soapenv:Header/>
<soapenv:Body>
<gs:getCheapestMealRequest/>
</soapenv:Body>
</soapenv:Envelope>]]

-- Define SOAP requests
local requests = {
    { method = "POST", path = "/ws", body = getMealRequest },
    { method = "POST", path = "/ws", body = addOrderRequest },
    { method = "POST", path = "/ws", body = getLargestMealRequest },
    { method = "POST", path = "/ws", body = getCheapestMealRequest }
}

-- Random request picker
request = function()
    local req = requests[math.random(1, #requests)]
    wrk.method = req.method
    wrk.body = req.body
    wrk.headers["Content-Type"] = "text/xml;charset=UTF-8"
    return wrk.format(req.method, req.path, wrk.headers, req.body)
end

-- Basic failure tracking
response = function(status, headers, body)
    if status < 200 or status >= 300 then
        print("FAIL")
    end
end

-- End summary
done = function(summary, latency, requests)
    io.write("\n--- SOAP Test Results ---\n")
    io.write("Total Requests: ", summary.requests, "\n")
    io.write("Average Latency: ", latency.mean / 1000, " ms\n")
    io.write("Max Latency: ", latency.max / 1000, " ms\n")
    io.write("Requests Per Second: ", summary.requests / (summary.duration / 1000000), "\n")
end

