package at.spengergasse.sj2526pos1javaseed.Domain;

import at.spengergasse.sj2526pos1javaseed.Ids.UserId;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import at.spengergasse.sj2526pos1javaseed.richtypes.Email;

@Getter
@Setter
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public abstract class User {

    @EmbeddedId
    private UserId id;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "email", nullable = false, unique = true))
    })
    private Email email;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "address_street")),
        @AttributeOverride(name = "city", column = @Column(name = "address_city")),
        @AttributeOverride(name = "zipCode", column = @Column(name = "address_zip_code")),
        @AttributeOverride(name = "country", column = @Column(name = "address_country"))
    })
    private Address address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Permission permission;
}
