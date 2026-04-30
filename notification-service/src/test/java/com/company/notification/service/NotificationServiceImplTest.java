package com.company.notification.service;

import com.company.notification.config.security.AuthenticatedUser;
import com.company.notification.dto.NotificationRequest;
import com.company.notification.dto.NotificationResponse;
import com.company.notification.dto.UnreadCountResponse;
import com.company.notification.repository.NotificationRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceImplTest {

    @Autowired private NotificationService service;
    @Autowired private NotificationRepository repository;
    @Autowired private CircuitBreakerRegistry cbRegistry;

    @MockitoSpyBean
    private NotificationRepository repositorySpy;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        cbRegistry.circuitBreaker("notificationDb").reset();
        cbRegistry.circuitBreaker("notificationCreate").reset();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        reset(repositorySpy);
    }

    private void authAs(String role, Long deptId) {
        var user = new AuthenticatedUser(1L, role, deptId);
        var auth = new UsernamePasswordAuthenticationToken(
                user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void create_then_count_then_list_then_markRead() {
        // Producer publishes
        service.create(NotificationRequest.builder()
                .eventType("INVOICE_SUBMITTED").message("Inv 1")
                .toRole("PROJECT_MANAGER").toDepartmentId(7L)
                .fromService("vendor-service").fromRole("VENDOR")
                .build());

        // PM in dept 7 sees 1 unread
        authAs("PROJECT_MANAGER", 7L);
        UnreadCountResponse count = service.unreadCountForCurrentUser();
        assertThat(count.getCount()).isEqualTo(1L);
        assertThat(count.isDegraded()).isFalse();

        // PM in dept 99 sees 0 — RBAC works
        authAs("PROJECT_MANAGER", 99L);
        assertThat(service.unreadCountForCurrentUser().getCount()).isZero();

        // Vendor sees 0 — RBAC works
        authAs("VENDOR", 7L);
        assertThat(service.unreadCountForCurrentUser().getCount()).isZero();

        // Back to PM dept 7 — list and mark
        authAs("PROJECT_MANAGER", 7L);
        var page = service.listForCurrentUser(null, null, PageRequest.of(0, 20));
        assertThat(page.getContent()).hasSize(1);

        Long id = page.getContent().get(0).getId();
        NotificationResponse marked = service.markAsRead(id);
        assertThat(marked.isRead()).isTrue();
        assertThat(service.unreadCountForCurrentUser().getCount()).isZero();
    }

    @Test
    void unreadCount_fallback_returns_degraded_when_db_fails() {
        authAs("PROJECT_MANAGER", 7L);

        doThrow(new DataAccessResourceFailureException("DB down"))
                .when(repositorySpy)
                .countUnreadForRecipient(anyString(), any());

        UnreadCountResponse resp = service.unreadCountForCurrentUser();

        // Strong fallback — bell shouldn't crash the UI
        assertThat(resp.getCount()).isZero();
        assertThat(resp.isDegraded()).isTrue();
    }

    @Test
    void list_fallback_returns_empty_page_when_db_fails() {
        authAs("PROJECT_MANAGER", 7L);

        doThrow(new DataAccessResourceFailureException("DB down"))
                .when(repositorySpy)
                .findAllForRecipientFiltered(anyString(), any(), any(), any(), any());

        var page = service.listForCurrentUser(null, null, PageRequest.of(0, 20));
        assertThat(page.getContent()).isEqualTo(List.of());
        assertThat(page.getTotalElements()).isZero();
    }
}
