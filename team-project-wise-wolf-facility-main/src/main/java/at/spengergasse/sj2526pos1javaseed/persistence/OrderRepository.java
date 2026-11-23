package at.spengergasse.sj2526pos1javaseed.persistence;

import at.spengergasse.sj2526pos1javaseed.Domain.Order;
import at.spengergasse.sj2526pos1javaseed.Ids.OrderId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, OrderId> {
}
