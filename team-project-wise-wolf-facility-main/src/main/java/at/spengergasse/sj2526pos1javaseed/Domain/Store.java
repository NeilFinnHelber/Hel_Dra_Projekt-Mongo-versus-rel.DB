package at.spengergasse.sj2526pos1javaseed.Domain;

import at.spengergasse.sj2526pos1javaseed.Ids.StoreId;
import at.spengergasse.sj2526pos1javaseed.richtypes.Phone_Number_Richtype;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Store {

    @EmbeddedId
    private StoreId storeId;



    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "store_series",
            joinColumns = @JoinColumn(name = "store_id"),
            inverseJoinColumns = @JoinColumn(name = "series_id")
    )
    private Set<Series> series = new HashSet<>();


    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "store_films",
            joinColumns = @JoinColumn(name = "store_id"),
            inverseJoinColumns = @JoinColumn(name = "film_id")
    )
    private Set<Film> films = new HashSet<>();

    @Embedded
    private Phone_Number_Richtype phoneNumber;
}