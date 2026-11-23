package at.spengergasse.sj2526pos1javaseed.Ids;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class StoreId implements Serializable {

    private Long id;

    public StoreId() {}

    public StoreId(Long id) {
        this.id = id;
    }

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if(!(o instanceof StoreId)) return false;
        StoreId that = (StoreId) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

