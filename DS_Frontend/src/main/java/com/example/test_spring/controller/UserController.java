package com.example.test_spring.controller;

import com.example.test_spring.model.Address;
import com.example.test_spring.model.BasketItem;
import com.example.test_spring.model.User;
import com.example.test_spring.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import com.example.test_spring.service.BasketService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class UserController {

    private final UserService userService;
    private final BasketService basketService;

    public UserController(UserService userService, BasketService basketService) {
        this.userService = userService;
        this.basketService = basketService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    @GetMapping("/profile")
    public String profilePage(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("user");
        if (userId == null) {
            return "redirect:/login";
        }
        return "profile";
    }


    @PostMapping("/login")
    public String login(@RequestParam String identifier, @RequestParam String password, HttpSession session, Model model) {
        try {
            User user = userService.login(identifier, password);
            session.setAttribute("user", user.getId());
            session.setAttribute("username", user.getUsername());
            session.setAttribute("admin", user.getIsAdmin());

            // Check if there is a pending basket item saved before login
            BasketItem pendingItem = (BasketItem) session.getAttribute("postLoginBasketItem");
            if (pendingItem != null) {
                basketService.addToBasket(user.getId(), pendingItem);
                session.removeAttribute("postLoginBasketItem");
                return "redirect:/basket";
            }

            return "redirect:/";

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.NOT_FOUND) {
                model.addAttribute("error", "Invalid username or password");
                return "login";
            }
            model.addAttribute("error", "Login failed: " + e.getMessage());
            return "login";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "login";
        } catch (Exception e) {
            model.addAttribute("error", "An unexpected error occurred");
            return "login";
        }
    }

    @PostMapping("/signup")
    public String signup(@RequestParam String username,
                         @RequestParam String email,
                         @RequestParam String password,
                         @RequestParam String country,
                         @RequestParam String street,
                         @RequestParam String streetNumber,
                         @RequestParam String postcode,
                         HttpSession session,
                         Model model) {
        try {
            userService.register(username, email, password, new Address(country, street, streetNumber, postcode));

            // Log the user in and store in session
            User user = userService.login(username, password);
            session.setAttribute("user", user.getId());
            session.setAttribute("username", user.getUsername());

            return "redirect:/";
        } catch (ResponseStatusException e) {
            model.addAttribute("error", e.getReason());
            return "signup";
        } catch (Exception e) {
            model.addAttribute("error", "Unexpected error: " + e.getMessage());
            return "signup";
        }
    }


    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}

