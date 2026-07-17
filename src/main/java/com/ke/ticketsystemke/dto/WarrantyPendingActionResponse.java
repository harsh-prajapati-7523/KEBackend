package com.ke.ticketsystemke.dto;
import java.util.List;
public record WarrantyPendingActionResponse(String code,String label,int priority,boolean blocking,List<String> reasons) {}
