package core.dto;

import core.model.ImportOperation;
import core.model.ImportStatus;

public record ImportOperationDto(Long id, ImportStatus status, String user, Integer addedCount) {
    public static ImportOperationDto from(ImportOperation operation) {
        return new ImportOperationDto(operation.getId(), operation.getStatus(),
                operation.getUser().getLogin(), operation.getAddedCount());
    }
}
