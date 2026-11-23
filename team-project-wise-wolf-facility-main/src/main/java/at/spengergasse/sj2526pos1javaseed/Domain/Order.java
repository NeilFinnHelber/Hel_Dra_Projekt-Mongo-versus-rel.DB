package at.spengergasse.sj2526pos1javaseed.Domain;

import at.spengergasse.sj2526pos1javaseed.Ids.OrderId;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Order {

    @EmbeddedId
    private OrderId id;

    @Column(nullable = false)
    private LocalDateTime orderDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;


    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, optional = true)
    private Order_Placement orderPlacement;



    public void setOrderPlacement(Order_Placement orderPlacement) {
        this.orderPlacement = orderPlacement;
        if (orderPlacement != null && orderPlacement.getOrder() != this) {
            orderPlacement.setOrder(this);
        }
    }
}