package com.ke.ticketsystemke.service;
import com.ke.ticketsystemke.entity.WarrantyClaim; import org.junit.jupiter.api.Test; import static org.junit.jupiter.api.Assertions.*;
class WarrantyPendingActionCalculatorTest {
 private final WarrantyPendingActionCalculator calculator=new WarrantyPendingActionCalculator();
 @Test void ownerIsHighestPriority(){assertEquals("ASSIGN_WARRANTY_OWNER",calculator.calculate(new WarrantyClaim()).code());}
 @Test void requiresPracticalProductDetailsAfterOwner(){WarrantyClaim c=new WarrantyClaim();c.setWarrantyOwnerEmployeeId("EMP1");assertEquals("COMPLETE_WARRANTY_DETAILS",calculator.calculate(c).code());assertTrue(calculator.calculate(c).reasons().contains("PRODUCT_IDENTIFICATION_MISSING"));}
 @Test void complaintAndFollowUpProgression(){WarrantyClaim c=new WarrantyClaim();c.setWarrantyOwnerEmployeeId("EMP1");c.setManufacturerName("Maker");c.setModelNumber("M1");assertEquals("REGISTER_MANUFACTURER_COMPLAINT",calculator.calculate(c).code());c.setManufacturerComplaintNumber("C1");c.setComplaintRegisteredDate(java.time.LocalDate.now());assertEquals("ADD_FOLLOW_UP_DATE",calculator.calculate(c).code());c.setNextFollowUpDate(java.time.LocalDate.now());assertEquals("NO_IMMEDIATE_ACTION",calculator.calculate(c).code());}
}
