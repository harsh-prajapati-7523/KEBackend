package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.UpdateWorkflowActionRequest;
import com.ke.ticketsystemke.entity.AccessKeyMetadata;
import com.ke.ticketsystemke.entity.WorkflowAction;
import com.ke.ticketsystemke.repository.AccessKeyMetadataRepository;
import com.ke.ticketsystemke.repository.WorkflowActionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowActionServiceTest {

    @Mock
    private WorkflowActionRepository workflowActionRepository;

    @Mock
    private AccessKeyMetadataRepository accessKeyMetadataRepository;

    private WorkflowActionService workflowActionService;

    @BeforeEach
    void setUp() {
        workflowActionService = new WorkflowActionService(
                workflowActionRepository,
                accessKeyMetadataRepository
        );
    }

    @Test
    void updateActionSynchronizesAccessMetadata() {
        WorkflowAction action = customAction();
        AccessKeyMetadata metadata = customMetadata();
        UpdateWorkflowActionRequest request = new UpdateWorkflowActionRequest();
        request.setDisplayName("Updated Label");
        request.setDescription("Updated description");
        request.setSortOrder(42);

        when(workflowActionRepository.findById(10L)).thenReturn(Optional.of(action));
        when(workflowActionRepository.save(action)).thenReturn(action);
        when(accessKeyMetadataRepository.findByAccessKey("CUSTOM_FLOW")).thenReturn(Optional.of(metadata));

        workflowActionService.updateAction(10L, request, "admin-1");

        ArgumentCaptor<AccessKeyMetadata> metadataCaptor = ArgumentCaptor.forClass(AccessKeyMetadata.class);
        verify(accessKeyMetadataRepository).save(metadataCaptor.capture());
        assertThat(metadataCaptor.getValue().getDisplayName()).isEqualTo("Updated Label");
        assertThat(metadataCaptor.getValue().getDescription()).isEqualTo("Updated description");
        assertThat(metadataCaptor.getValue().getSortOrder()).isEqualTo(42);
        assertThat(metadataCaptor.getValue().isActive()).isTrue();
    }

    @Test
    void updateActionStateDeactivatesAccessMetadata() {
        WorkflowAction action = customAction();
        AccessKeyMetadata metadata = customMetadata();

        when(workflowActionRepository.findById(10L)).thenReturn(Optional.of(action));
        when(workflowActionRepository.save(action)).thenReturn(action);
        when(accessKeyMetadataRepository.findByAccessKey("CUSTOM_FLOW")).thenReturn(Optional.of(metadata));

        workflowActionService.updateActionState(10L, false, "admin-1");

        ArgumentCaptor<AccessKeyMetadata> metadataCaptor = ArgumentCaptor.forClass(AccessKeyMetadata.class);
        verify(accessKeyMetadataRepository).save(metadataCaptor.capture());
        assertThat(metadataCaptor.getValue().isActive()).isFalse();
    }

    @Test
    void protectedActionCannotBeUpdated() {
        WorkflowAction action = new WorkflowAction();
        action.setActionKey("PICK_TICKET");
        action.setDisplayName("Pick Ticket");
        action.setButtonLabel("Pick Ticket");
        action.setSystemAction(true);
        action.setProtectedAction(true);

        when(workflowActionRepository.findById(10L)).thenReturn(Optional.of(action));

        assertThatThrownBy(() -> workflowActionService.updateAction(10L, new UpdateWorkflowActionRequest(), "admin-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("System and protected workflow actions are read-only");
    }

    private WorkflowAction customAction() {
        WorkflowAction action = new WorkflowAction();
        action.setActionKey("CUSTOM_FLOW");
        action.setDisplayName("Custom Flow");
        action.setButtonLabel("Custom Flow");
        action.setDescription("Custom description");
        action.setActive(true);
        action.setSystemAction(false);
        action.setProtectedAction(false);
        return action;
    }

    private AccessKeyMetadata customMetadata() {
        AccessKeyMetadata metadata = new AccessKeyMetadata();
        metadata.setAccessKey("CUSTOM_FLOW");
        metadata.setDisplayName("Custom Flow");
        metadata.setDescription("Custom description");
        metadata.setCategory("Workflow Actions");
        metadata.setActive(true);
        metadata.setSystemKey(false);
        metadata.setProtectedKey(false);
        return metadata;
    }
}
