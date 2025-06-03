package be.kuleuven.broker.repository;

import be.kuleuven.broker.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Integer> {
}
