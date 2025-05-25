package be.kuleuven.broker.domain;

import be.kuleuven.broker.model.User;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Repository
public class UserRepository {
    private final Map<String, User> usersById = new HashMap<>();

    public void save(User user) {
        usersById.put(user.getId(), user);
    }

    public User findById(String id) {
        return usersById.get(id);
    }

    public User findByUsername(String username) {
        return usersById.values().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }

    public Collection<User> findAll() {
        return usersById.values();
    }
}

