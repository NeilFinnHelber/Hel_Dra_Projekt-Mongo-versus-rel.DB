package at.spengergasse.sj2526pos1javaseed.Ids;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class Order_PlacementId implements Serializable {
    private Long id;

    public Order_PlacementId() {}

    public Order_PlacementId(Long id) {
        this.id = id;
    }

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if(!(o instanceof Order_PlacementId)) return false;
        Order_PlacementId that = (Order_PlacementId) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
