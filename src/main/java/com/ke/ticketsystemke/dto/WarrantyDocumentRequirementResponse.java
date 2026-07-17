package com.ke.ticketsystemke.dto;import com.ke.ticketsystemke.entity.WarrantyAttachmentCategory;
public record WarrantyDocumentRequirementResponse(WarrantyAttachmentCategory category,boolean required,boolean present,boolean overridden,boolean blocking,String label){}
