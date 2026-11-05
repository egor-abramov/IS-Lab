package core.model;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Coordinates getCoordinates() { return coordinates; }
    public void setCoordinates(Coordinates coordinates) { this.coordinates = coordinates; }
    public LocalDateTime getCreationDate() { return creationDate; }
    public void setCreationDate(LocalDateTime creationDate) { this.creationDate = creationDate; }
    public Address getOfficialAddress() { return officialAddress; }
    public void setOfficialAddress(Address officialAddress) { this.officialAddress = officialAddress; }
    public Float getAnnualTurnover() { return annualTurnover; }
    public void setAnnualTurnover(Float annualTurnover) { this.annualTurnover = annualTurnover; }
    public int getEmployeesCount() { return employeesCount; }
    public void setEmployeesCount(int employeesCount) { this.employeesCount = employeesCount; }
    public Long getRating() { return rating; }
    public void setRating(Long rating) { this.rating = rating; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public OrganizationType getType() { return type; }
    public void setType(OrganizationType type) { this.type = type; }
    public Address getPostalAddress() { return postalAddress; }
    public void setPostalAddress(Address postalAddress) { this.postalAddress = postalAddress; }
}
