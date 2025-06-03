package be.kuleuven.broker.repository;

import be.kuleuven.broker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    User findByUsername(String username);
    boolean existsByUsername(String username);
    void deleteByUsername(String username);

    boolean existsByEmail(String email);
}


