package core.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "location")
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private long x;

    @NotNull(message = "y обязательное поле")
    @Column(nullable = false)
    private Float y;

    @NotNull(message = "name обязательное поле")
    @Column(nullable = false)
    private String name;
}
