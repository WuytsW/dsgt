package be.kuleuven.broker.domain;

import be.kuleuven.broker.model.User;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void register(String username, String password) {
        String id = UUID.randomUUID().toString();
        userRepository.save(new User(id, username, password));
    }

    public User getUserById(String id) {
        return userRepository.findById(id);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Collection<User> getAllUsers() {
        return userRepository.findAll();
    }

    public boolean validateLogin(String username, String password) {
        User user = getUserByUsername(username);
        return user != null && user.getPassword().equals(password);
    }


}

