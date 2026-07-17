package com.ke.ticketsystemke.dto;
import java.util.List;
import java.time.LocalDate;
public record WarrantyPendingActionResponse(String code,String label,int priority,boolean blocking,LocalDate dueDate,boolean overdue,long daysOverdue,List<String> reasons,String recommendedActionFormKey) {}
