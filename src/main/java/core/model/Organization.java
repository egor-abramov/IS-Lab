package core.model;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "organization")
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Positive(message = "id должно быть больше 0")
    private Long id;

    @NotBlank(message = "name не может быть пустым")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "coordinates обязательное поле")
    @ManyToOne(optional = false, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "coordinates_id", nullable = false)
    @Valid
    private Coordinates coordinates;

    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "official_address_id")
    @Valid
    private Address officialAddress;

    @Positive(message = "annualTurnover должно быть больше 0")
    @Column(name = "annual_turnover")
    private Float annualTurnover;

    @Positive(message = "employeesCount должно быть больше 0")
    @Column(name = "employees_count", nullable = false)
    private int employeesCount;

    @Positive(message = "rating должно быть больше 0")
    private Long rating;

    @Column(name = "full_name")
    private String fullName;

    @Enumerated(EnumType.STRING)
    private OrganizationType type;

    @NotNull(message = "postalAddress обязательное поле")
    @ManyToOne(optional = false, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "postal_address_id", nullable = false)
    @Valid
    private Address postalAddress;

    @PrePersist
    void onCreate() {
        creationDate = LocalDateTime.now();
    }
}
