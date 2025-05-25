package be.kuleuven.broker.controllers;

import be.kuleuven.broker.domain.UserService;
import be.kuleuven.broker.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Collection<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable String id) {
        return userService.getUserById(id);
    }

    @PostMapping
    public void registerUser(@RequestBody User user) {
        userService.register(user.getUsername(), user.getPassword());
    }

    @PostMapping("/login")
    public User login(@RequestBody User user) {
        User found = userService.getUserByUsername(user.getUsername());
        if (found == null) {
            // Username not found → use 404 for better frontend handling
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        if (found.getPassword().equals(user.getPassword())) {
            return found;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }

}
