package be.kuleuven.broker.controller;

import be.kuleuven.broker.repository.UserRepository;
import be.kuleuven.broker.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public Collection<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable String id) {
        return userRepository.findById(id);
    }

    @PostMapping
    public void registerUser(@RequestBody User user) {
        userRepository.register(user.getUsername(), user.getPassword());
    }

    @PostMapping("/login")
    public User login(@RequestBody User user) {
        User found = userRepository.findByUsername(user.getUsername());
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
