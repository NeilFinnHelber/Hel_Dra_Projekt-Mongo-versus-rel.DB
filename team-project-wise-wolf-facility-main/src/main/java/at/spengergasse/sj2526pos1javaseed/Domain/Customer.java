package at.spengergasse.sj2526pos1javaseed.Domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class Customer extends User {

    private String firstName;
    private String lastName;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BillingInformation> billingInformation = new ArrayList<>();

    @OneToMany(mappedBy = "customer")
    @Builder.Default
    private List<Order_Placement> orderPlacements = new ArrayList<>();
}
