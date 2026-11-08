package core.model;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "address")
public class Address implements OwnedEntity {
    @Column(name = "owner_id")
    private Long ownerId;

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
}
