package be.kuleuven.broker.repository;

import be.kuleuven.broker.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, Integer> {
}
