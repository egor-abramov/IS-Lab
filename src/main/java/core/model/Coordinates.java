package core.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "coordinates")
public class Coordinates {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @DecimalMin(value = "-577", inclusive = false, message = "x должно быть больше -577")
    @Column(nullable = false)
    private float x;

    @Column(nullable = false)
    private int y;
}
