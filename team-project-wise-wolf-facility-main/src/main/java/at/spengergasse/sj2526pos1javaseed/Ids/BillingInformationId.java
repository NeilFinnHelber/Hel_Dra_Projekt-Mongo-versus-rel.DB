package at.spengergasse.sj2526pos1javaseed.Ids;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class BillingInformationId implements Serializable {
    @Column(name = "billing_information_id")
    private Long id;

    public BillingInformationId() {}

    public BillingInformationId(long id) {
        this.id = id;
    }

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if(!(o instanceof BillingInformationId)) return false;
        BillingInformationId that = (BillingInformationId) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
