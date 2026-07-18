package com.ke.ticketsystemke.service;
import com.ke.ticketsystemke.entity.WarrantyClaim; import org.junit.jupiter.api.Test; import static org.junit.jupiter.api.Assertions.*;
class WarrantyPendingActionCalculatorTest {
 private final WarrantyPendingActionCalculator calculator=new WarrantyPendingActionCalculator();
 @Test void requiresPracticalProductDetailsFirst(){WarrantyClaim c=new WarrantyClaim();c.setActive(true);assertEquals("COMPLETE_WARRANTY_DETAILS",calculator.calculate(c).code());assertTrue(calculator.calculate(c).reasons().contains("PRODUCT_IDENTIFICATION_MISSING"));}
 @Test void complaintAndVisitProgression(){WarrantyClaim c=new WarrantyClaim();c.setActive(true);c.setManufacturerName("Maker");c.setModelNumber("M1");assertEquals("REGISTER_MANUFACTURER_COMPLAINT",calculator.calculate(c).code());c.setManufacturerComplaintNumber("C1");c.setComplaintRegisteredDate(java.time.LocalDate.now());assertEquals("ADD_EXPECTED_VISIT_DATE",calculator.calculate(c).code());}
}
