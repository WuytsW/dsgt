package com.example.test_spring.controller;

import com.example.test_spring.model.Address;
import com.example.test_spring.model.BasketItem;
import com.example.test_spring.model.GuestOrder;
import com.example.test_spring.model.User;
import com.example.test_spring.service.BasketService;
import com.example.test_spring.service.RecipeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
public class OrderController {

    private final RecipeService recipeService;
    private final String brokerUrl;
    ObjectMapper objectMapper = new ObjectMapper();
    RestTemplate restTemplate = new RestTemplate();

    public OrderController(RecipeService recipeService, @Value("${broker.url}") String brokerUrl) {
        this.recipeService = recipeService;
        this.brokerUrl = brokerUrl;
    }

    @PostMapping("/check")
    public String checkAvailability(HttpSession session, RedirectAttributes redirectAttributes) throws JsonProcessingException {
        try {
            if(session.getAttribute("user") == null){
                String url = brokerUrl + "/check-guest";
                List<BasketItem> basket = getOrCreateBasket(session);
                restTemplate.postForObject(url, basket, String.class);
            }else {
                String url = brokerUrl + "/check/" + session.getAttribute("user");
                restTemplate.postForObject(url, null, String.class);
            }
        }catch (HttpClientErrorException.Conflict e) {
            if(session.getAttribute("user") == null){
                return GuestConflict(e, redirectAttributes, session);
            }else {
                return Conflict(e, redirectAttributes);
            }
        }catch(HttpServerErrorException.BadGateway e) {
            return BadGateway(redirectAttributes);
        }catch(Exception e) {
           e.printStackTrace();
        }

        return "redirect:/payment";
    }

    @PostMapping("/order")
    public String Order(Model model, HttpSession session, RedirectAttributes redirectAttributes) throws JsonProcessingException {
        try {
            String response;
            if(session.getAttribute("user") == null){
                String url = brokerUrl + "/order-guest";

                List<BasketItem> basket = getOrCreateBasket(session);
                User guestuser = (User) session.getAttribute("guestUser");
                GuestOrder guestOrder = new GuestOrder(basket, guestuser);
                response = restTemplate.postForObject(url, guestOrder, String.class);
            }else {
                String url = brokerUrl + "/order/" + session.getAttribute("user");
                response = restTemplate.postForObject(url, null, String.class);
            }
            model.addAttribute("mealsJson", response);
        }catch (HttpClientErrorException.Conflict e) {
            if(session.getAttribute("user") == null){
                return GuestConflict(e, redirectAttributes, session);
            }else {
                return Conflict(e, redirectAttributes);
            }
        }catch(HttpServerErrorException.BadGateway e) {
            return BadGateway(redirectAttributes);
        }catch (Exception e) {
            model.addAttribute("mealsJson", "Error: " + e.getMessage());
        }

        if(session.getAttribute("user") == null){
            session.removeAttribute("basket");
            session.removeAttribute("guestUser");
        }

        return "order-result";
    }

    private String Conflict(HttpClientErrorException.Conflict e, RedirectAttributes redirectAttributes) throws JsonProcessingException{
        JsonNode root = objectMapper.readTree(e.getResponseBodyAsString());

        List<String> removedRecipes = new ArrayList<>();
        for (JsonNode idNode : root.path("blockedRecipesIds")) {
            int id = Integer.parseInt(idNode.asText());
            String name = recipeService.getRecipeById(id).getRecipe();
            removedRecipes.add(name);
        }

        redirectAttributes.addFlashAttribute("msg", "Some recipes could not be ordered and have been removed from your basket.");
        redirectAttributes.addFlashAttribute("removedRecipes", removedRecipes);
        return "redirect:/basket";
    }

    private String BadGateway(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("msg", "Some suppliers didn't respond");
        return "redirect:/basket";
    }

    private String GuestConflict(HttpClientErrorException.Conflict e, RedirectAttributes redirectAttributes, HttpSession session) throws JsonProcessingException {

        JsonNode root = objectMapper.readTree(e.getResponseBodyAsString());

        List<String> removedRecipes = new ArrayList<>();
        JsonNode blocked = root.path("blockedRecipesIds");

        @SuppressWarnings("unchecked")
        List<BasketItem> basket = (List<BasketItem>) session.getAttribute("basket");
        if (basket == null) basket = new ArrayList<>();

        Set<Integer> blockedIds = new HashSet<>();

        for (JsonNode idNode : blocked) {
            int id = Integer.parseInt(idNode.asText());
            blockedIds.add((id));
            String name = recipeService.getRecipeById(id).getRecipe(); // or getName()
            removedRecipes.add(name);
        }

        // Remove blocked recipes from basket
        basket.removeIf(item -> blockedIds.contains(item.getRecipe().getId()));
        session.setAttribute("basket", basket); // update session

        // Set flash messages
        redirectAttributes.addFlashAttribute("msg", "Some recipes could not be ordered and have been removed from your basket.");
        redirectAttributes.addFlashAttribute("removedRecipes", removedRecipes);

        return "redirect:/basket";
    }


    @GetMapping("/payment")
    public String showPaymentPage(Model model) {
        return "payment";
    }

    @PostMapping("/pay")
    public String pay(HttpSession session,
                      @RequestParam(required = false) String email,
                      @RequestParam(required = false) String country,
                      @RequestParam(required = false) String street,
                      @RequestParam(required = false) String streetNumber,
                      @RequestParam(required = false) String postcode,
                      Model model, RedirectAttributes redirectAttributes) {
        try {
            String url;

            if (session.getAttribute("user") == null) {
                url = brokerUrl + "/pay/0";

                User guestUser = new User(0, "guest", "0000");
                guestUser.setEmail(email);
                Address address = new Address(country, street, streetNumber, postcode);
                guestUser.setAddress(address);
                session.setAttribute("guestUser", guestUser);
            }else {
                url = brokerUrl + "/pay/" + session.getAttribute("user");
            }
            restTemplate.postForEntity(url, null, Void.class);

            redirectAttributes.addFlashAttribute("paid", true);
            return "redirect:/payment";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Payment failed.");
            return "redirect:/payment";
        }
    }


    @SuppressWarnings("unchecked")
    private List<BasketItem> getOrCreateBasket(HttpSession session) {
        List<BasketItem> basket = (List<BasketItem>) session.getAttribute("basket");
        if (basket == null) {
            basket = new ArrayList<>();
            session.setAttribute("basket", basket);
        }
        return basket;
    }
}
