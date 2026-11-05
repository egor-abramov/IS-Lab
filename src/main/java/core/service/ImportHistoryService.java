package core.service;

import core.dto.ImportOperationDto;
import core.dto.ItemsDto;
import core.model.ImportOperation;
import core.model.ImportStatus;
import core.repository.ImportOperationRepository;
import core.security.CurrentUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ImportHistoryService {
    @Inject
    ImportOperationRepository repository;
    @Inject
    CurrentUser currentUser;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public long start() {
        ImportOperation operation = new ImportOperation();
        operation.setUser(currentUser.get());
        operation.setStatus(ImportStatus.IN_PROGRESS);
        repository.create(operation);
        return operation.getId();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void finish(long id, ImportStatus status, Integer count) {
        repository.finish(id, status, count);
    }

    @Transactional
    public ItemsDto<ImportOperationDto> history() {
        return new ItemsDto<>(repository.history().stream().map(ImportOperationDto::from).toList());
    }
}
