package com.buildsmart.finance.repository;

import com.buildsmart.finance.entity.Budget;
import com.buildsmart.finance.entity.enums.BudgetCategory;
import com.buildsmart.finance.entity.enums.BudgetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, String> {

    /**
     * Find budget by project ID and category
     */
    @Query("SELECT b FROM Budget b WHERE b.projectId = :projectId AND b.budgetCategory = :category " +
            "AND b.isDeleted = false")
    Optional<Budget> findByProjectIdAndCategory(
            @Param("projectId") String projectId,
            @Param("category") BudgetCategory category);

    /**
     * Find all budgets for a project (with pagination and search)
     */
    @Query("SELECT b FROM Budget b WHERE b.projectId = :projectId " +
            "AND b.isDeleted = false")
    Page<Budget> findByProjectId(@Param("projectId") String projectId, Pageable pageable);

    /**
     * Find all budget revisions for a parent budget (deprecated - no longer supported)
     */
    @Query("SELECT b FROM Budget b WHERE b.budgetId = :parentBudgetId AND b.isDeleted = false")
    Page<Budget> findRevisions(@Param("parentBudgetId") String parentBudgetId, Pageable pageable);

    /**
     * Find all approved budgets for a project
     */
    @Query("SELECT b FROM Budget b WHERE b.projectId = :projectId AND b.status = 'APPROVED' " +
            "AND b.isDeleted = false")
    List<Budget> findApprovedBudgetsByProjectId(@Param("projectId") String projectId);

    /**
     * Find budgets by status
     */
    @Query("SELECT b FROM Budget b WHERE b.status = :status " +
            "AND b.isDeleted = false ORDER BY b.createdAt DESC")
    Page<Budget> findByStatus(@Param("status") BudgetStatus status, Pageable pageable);

    /**
     * Check if budget exists for project and category
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END " +
            "FROM Budget b WHERE b.projectId = :projectId AND b.budgetCategory = :category " +
            "AND b.isDeleted = false")
    boolean existsByProjectIdAndCategory(
            @Param("projectId") String projectId,
            @Param("category") BudgetCategory category);

    /**
     * Find budgets created by user
     */
    @Query("SELECT b FROM Budget b WHERE b.createdBy = :createdBy " +
            "AND b.isDeleted = false")
    Page<Budget> findByCreatedBy(@Param("createdBy") String createdBy, Pageable pageable);
}
