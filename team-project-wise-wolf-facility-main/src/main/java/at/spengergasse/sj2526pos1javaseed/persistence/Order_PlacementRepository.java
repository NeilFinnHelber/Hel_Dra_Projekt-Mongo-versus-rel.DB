package at.spengergasse.sj2526pos1javaseed.persistence;

import at.spengergasse.sj2526pos1javaseed.Domain.Order_Placement;
import at.spengergasse.sj2526pos1javaseed.Ids.Order_PlacementId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface Order_PlacementRepository  extends JpaRepository<Order_Placement, Order_PlacementId> {
}
