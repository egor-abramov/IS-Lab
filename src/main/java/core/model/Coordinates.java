package core.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;

@Entity
@Table(name = "coordinates")
public class Coordinates {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @DecimalMin(value = "-577", inclusive = false, message = "x must be greater than -577")
    @Column(nullable = false)
    private float x;

    @Column(nullable = false)
    private int y;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public float getX() { return x; }
    public void setX(float x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
}
