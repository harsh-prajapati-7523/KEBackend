package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.entity.AccessKeyMetadata;
import com.ke.ticketsystemke.entity.Employee;
import com.ke.ticketsystemke.entity.Role;
import com.ke.ticketsystemke.entity.TicketCategoryConfig;
import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.entity.WorkflowAction;
import com.ke.ticketsystemke.entity.WorkflowMode;
import com.ke.ticketsystemke.entity.WorkflowStatus;
import com.ke.ticketsystemke.entity.WorkflowTransition;
import com.ke.ticketsystemke.repository.AccessKeyMetadataRepository;
import com.ke.ticketsystemke.repository.RoleAccessRuleRepository;
import com.ke.ticketsystemke.repository.RoleRepository;
import com.ke.ticketsystemke.repository.WorkflowActionRepository;
import com.ke.ticketsystemke.repository.WorkflowStatusRepository;
import com.ke.ticketsystemke.repository.WorkflowTransitionCategoryRuleRepository;
import com.ke.ticketsystemke.repository.WorkflowTransitionRepository;
import com.ke.ticketsystemke.repository.WorkflowTransitionRoleRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultWorkflowProvisioningServiceTest {

    @Mock
    private WorkflowStatusRepository workflowStatusRepository;

    @Mock
    private WorkflowActionRepository workflowActionRepository;

    @Mock
    private WorkflowTransitionRepository workflowTransitionRepository;

    @Mock
    private WorkflowTransitionCategoryRuleRepository categoryRuleRepository;

    @Mock
    private WorkflowTransitionRoleRuleRepository roleRuleRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RoleAccessRuleRepository roleAccessRuleRepository;

    @Mock
    private AccessKeyMetadataRepository accessKeyMetadataRepository;

    @Mock
    private WorkflowValidationService workflowValidationService;

    private DefaultWorkflowProvisioningService service;

    @BeforeEach
    void setUp() {
        service = new DefaultWorkflowProvisioningService(
                workflowStatusRepository,
                workflowActionRepository,
                workflowTransitionRepository,
                categoryRuleRepository,
                roleRuleRepository,
                roleRepository,
                roleAccessRuleRepository,
                accessKeyMetadataRepository,
                workflowValidationService
        );
    }

    @Test
    void provisionAndActivateCreatesGenericUnprotectedDefaultWorkflow() {
        TicketCategoryConfig category = category();
        Employee actor = new Employee();
        Role superAdmin = role();
        AtomicLong statusIds = new AtomicLong(10);
        AtomicLong transitionIds = new AtomicLong(20);

        when(workflowStatusRepository.findByStatusKey(anyString())).thenReturn(Optional.empty());
        when(workflowStatusRepository.save(any(WorkflowStatus.class))).thenAnswer(invocation -> {
            WorkflowStatus status = invocation.getArgument(0);
            if (status.getId() == null) {
                ReflectionTestUtils.setField(status, "id", statusIds.getAndIncrement());
            }
            return status;
        });
        when(workflowActionRepository.findByActionKey(anyString())).thenReturn(Optional.empty());
        when(workflowActionRepository.save(any(WorkflowAction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accessKeyMetadataRepository.findByAccessKey(anyString())).thenReturn(Optional.empty());
        when(accessKeyMetadataRepository.save(any(AccessKeyMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(workflowTransitionRepository.findByActionKeyAndFromStatusRecord_IdAndToStatusRecord_Id(anyString(), anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(workflowTransitionRepository.save(any(WorkflowTransition.class))).thenAnswer(invocation -> {
            WorkflowTransition transition = invocation.getArgument(0);
            if (transition.getId() == null) {
                ReflectionTestUtils.setField(transition, "id", transitionIds.getAndIncrement());
            }
            return transition;
        });
        when(roleRepository.findAllByOrderByRoleKeyAsc()).thenReturn(List.of(superAdmin));
        when(categoryRuleRepository.findByWorkflowTransition_IdAndCategory_Id(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(roleRuleRepository.findByWorkflowTransition_IdAndRole_Id(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(roleAccessRuleRepository.findByRoleIdAndAccessKey(anyLong(), anyString())).thenReturn(Optional.empty());
        doNothing().when(workflowValidationService).requireValidCategoryWorkflow(3L);

        service.provisionAndActivate(category, actor);

        ArgumentCaptor<WorkflowStatus> statusCaptor = ArgumentCaptor.forClass(WorkflowStatus.class);
        ArgumentCaptor<WorkflowAction> actionCaptor = ArgumentCaptor.forClass(WorkflowAction.class);
        ArgumentCaptor<WorkflowTransition> transitionCaptor = ArgumentCaptor.forClass(WorkflowTransition.class);
        verify(workflowStatusRepository, org.mockito.Mockito.times(3)).save(statusCaptor.capture());
        verify(workflowActionRepository, org.mockito.Mockito.times(2)).save(actionCaptor.capture());
        verify(workflowTransitionRepository, org.mockito.Mockito.times(2)).save(transitionCaptor.capture());

        assertThat(statusCaptor.getAllValues())
                .extracting(WorkflowStatus::getStatusKey)
                .containsExactly("DEFAULT_NEW", "DEFAULT_IN_PROGRESS", "DEFAULT_DONE");
        assertThat(statusCaptor.getAllValues())
                .allMatch(status -> !status.isSystemStatus())
                .allMatch(status -> !status.isProtectedStatus());
        assertThat(statusCaptor.getAllValues())
                .extracting(WorkflowStatus::getBehaviorBucket)
                .containsExactly(TicketStatus.NEW, TicketStatus.IN_PROGRESS, TicketStatus.COMPLETED);

        assertThat(actionCaptor.getAllValues())
                .extracting(WorkflowAction::getActionKey)
                .containsExactly("DEFAULT_START", "DEFAULT_FINISH");
        assertThat(actionCaptor.getAllValues())
                .allMatch(action -> !action.isSystemAction())
                .allMatch(action -> !action.isProtectedAction());

        assertThat(transitionCaptor.getAllValues())
                .extracting(WorkflowTransition::getActionKey)
                .containsExactly("DEFAULT_START", "DEFAULT_FINISH");
        assertThat(transitionCaptor.getAllValues())
                .allMatch(transition -> !transition.isSystemTransition())
                .allMatch(transition -> !transition.isProtectedTransition());

        assertThat(category.getWorkflowMode()).isEqualTo(WorkflowMode.DB_CONFIGURED);
        assertThat(category.isDbWorkflowEnabled()).isTrue();
        assertThat(category.isFixedActionsEnabled()).isFalse();
    }

    private TicketCategoryConfig category() {
        TicketCategoryConfig category = new TicketCategoryConfig();
        ReflectionTestUtils.setField(category, "id", 3L);
        category.setCategoryKey("TEST_CATEGORY");
        category.setDisplayName("Test Category");
        category.setActive(true);
        return category;
    }

    private Role role() {
        Role role = new Role();
        ReflectionTestUtils.setField(role, "id", 1L);
        role.setRoleKey("SUPER_ADMIN");
        role.setDisplayName("Super Admin");
        role.setActive(true);
        return role;
    }
}
