package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.Employee;
import com.ke.ticketsystemke.entity.EmployeeRole;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmployeeIdIgnoreCase(String employeeId);

    @Query("select employee from Employee employee where lower(employee.employeeId) in :employeeIds")
    List<Employee> findByEmployeeIdLowercaseIn(@Param("employeeIds") Set<String> employeeIds);

    List<Employee> findAllByActiveTrueOrderByNameAscEmployeeIdAsc();

    boolean existsByEmployeeIdIgnoreCase(String employeeId);

    boolean existsByRole(EmployeeRole role);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Employee> findAllByRole(EmployeeRole role);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Employee> findAllByRoleRecord_RoleKey(String roleKey);
}
