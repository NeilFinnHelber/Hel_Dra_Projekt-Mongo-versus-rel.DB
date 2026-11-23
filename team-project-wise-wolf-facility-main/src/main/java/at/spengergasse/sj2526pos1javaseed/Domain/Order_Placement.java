package at.spengergasse.sj2526pos1javaseed.Domain;

import at.spengergasse.sj2526pos1javaseed.Ids.Order_PlacementId;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "order_placement")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Order_Placement {

    @EmbeddedId
    private Order_PlacementId id;

    @Column(nullable = false)
    private LocalDate orderDate;

    @Column(nullable = false)
    private LocalDate orderReturnDate;

    private LocalDate actualReturnDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;


    public void setCustomer(Customer customer) {
        if (this.customer != null) {
            this.customer.getOrderPlacements().remove(this);
        }
        this.customer = customer;
        if (customer != null) {
            customer.getOrderPlacements().add(this);
        }
    }

    public void setOrder(Order order) {
        if (this.order != null) {
            this.order.setOrderPlacement(null);
        }
        this.order = order;
        if (order != null) {
            order.setOrderPlacement(this);
        }
    }
}

