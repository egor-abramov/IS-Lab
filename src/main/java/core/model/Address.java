package core.model;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "address")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "street обязательное поле")
    @Column(nullable = false)
    private String street;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "town_id")
    @Valid
    private Location town;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public Location getTown() { return town; }
    public void setTown(Location town) { this.town = town; }
}
