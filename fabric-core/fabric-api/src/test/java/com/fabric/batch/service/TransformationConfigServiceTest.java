package com.fabric.batch.service;

import com.fabric.batch.entity.FieldTemplateEntity;
import com.fabric.batch.repository.TransformationConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransformationConfigServiceTest {

    @Mock TransformationConfigRepository repository;
    @InjectMocks TransformationConfigService service;

    @Test
    void findAll_returnsList() {
        when(repository.findAll()).thenReturn(List.of(new FieldTemplateEntity()));
        assertThat(service.findAll()).hasSize(1);
    }

    @Test
    void findById_found_returnsEntity() {
        FieldTemplateEntity e = new FieldTemplateEntity();
        e.setId("T-001");
        when(repository.findById("T-001")).thenReturn(Optional.of(e));
        assertThat(service.findById("T-001").getId()).isEqualTo("T-001");
    }

    @Test
    void findById_notFound_throwsException() {
        when(repository.findById("GONE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById("GONE"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("GONE");
    }

    @Test
    void create_savesAndReturns() {
        FieldTemplateEntity e = new FieldTemplateEntity();
        e.setId("T-NEW");
        when(repository.save(e)).thenReturn(e);
        assertThat(service.create(e).getId()).isEqualTo("T-NEW");
    }

    @Test
    void update_existingId_updatesAndReturns() {
        FieldTemplateEntity e = new FieldTemplateEntity();
        e.setId("T-001");
        when(repository.findById("T-001")).thenReturn(Optional.of(e));
        when(repository.update(e)).thenReturn(e);
        assertThat(service.update("T-001", e).getId()).isEqualTo("T-001");
    }

    @Test
    void softDelete_existingId_callsRepo() {
        FieldTemplateEntity e = new FieldTemplateEntity();
        e.setId("T-001");
        when(repository.findById("T-001")).thenReturn(Optional.of(e));
        service.softDelete("T-001");
        verify(repository).softDelete("T-001");
    }
}
