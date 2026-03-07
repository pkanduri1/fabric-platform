package com.fabric.batch.service;

import com.fabric.batch.entity.FieldTemplateEntity;
import com.fabric.batch.repository.TransformationConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransformationConfigService {

    private final TransformationConfigRepository repository;

    @Transactional(readOnly = true)
    public List<FieldTemplateEntity> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public FieldTemplateEntity findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Transformation config not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<FieldTemplateEntity> findBySourceSystem(String fileType) {
        return repository.findBySourceSystem(fileType);
    }

    @Transactional
    public FieldTemplateEntity create(FieldTemplateEntity entity) {
        return repository.save(entity);
    }

    @Transactional
    public FieldTemplateEntity update(String id, FieldTemplateEntity entity) {
        repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Transformation config not found: " + id));
        entity.setId(id);
        return repository.update(entity);
    }

    @Transactional
    public void softDelete(String id) {
        repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Transformation config not found: " + id));
        repository.softDelete(id);
    }
}
