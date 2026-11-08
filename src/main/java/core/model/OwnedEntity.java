package core.model;

public interface OwnedEntity {
    Long getId();
    Long getOwnerId();
    void setOwnerId(Long ownerId);
}
