package at.spengergasse.sj2526pos1javaseed.Ids;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class Phone_NumberId implements Serializable {
    private Long id;

    public Phone_NumberId() {}

    public Phone_NumberId(Long id) {
        this.id = id;
    }

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if(!(o instanceof Phone_NumberId)) return false;
        Phone_NumberId that = (Phone_NumberId) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
