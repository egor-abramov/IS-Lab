package core.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "location")
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private long x;

    @NotNull(message = "location y is required")
    @Column(nullable = false)
    private Float y;

    @NotNull(message = "location name is required")
    @Column(nullable = false)
    private String name;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getX() { return x; }
    public void setX(long x) { this.x = x; }
    public Float getY() { return y; }
    public void setY(Float y) { this.y = y; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
