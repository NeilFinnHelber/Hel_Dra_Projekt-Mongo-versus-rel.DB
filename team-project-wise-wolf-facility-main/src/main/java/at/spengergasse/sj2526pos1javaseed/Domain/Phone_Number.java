package at.spengergasse.sj2526pos1javaseed.Domain;

import at.spengergasse.sj2526pos1javaseed.Ids.Phone_NumberId;
import at.spengergasse.sj2526pos1javaseed.richtypes.Phone_Number_Richtype;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "phone_numbers")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Phone_Number {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Phone_Number_Richtype phoneNumber;

    @Column(nullable = false)
    private int telephonePrefix;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Country country;
}