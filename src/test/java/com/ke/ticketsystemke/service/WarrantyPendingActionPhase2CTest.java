package com.ke.ticketsystemke.service;
import com.ke.ticketsystemke.entity.*;import org.junit.jupiter.api.Test;import java.time.*;import static org.assertj.core.api.Assertions.*;
class WarrantyPendingActionPhase2CTest {
 private final LocalDate today=LocalDate.of(2026,7,20);private final WarrantyPendingActionCalculator calculator=new WarrantyPendingActionCalculator(Clock.fixed(today.atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant(),ZoneId.of("Asia/Kolkata")));
 @Test void repairedCaseMovesToDelivery(){WarrantyClaim c=claim();c.setResult(WarrantyClaimResult.MANUFACTURER_REPAIR_DONE);c.setState(WarrantyClaimState.MANUFACTURER_REPAIR_COMPLETED);assertThat(calculator.calculate(c).code()).isEqualTo("MOVE_TO_READY_DELIVERY");}
 @Test void rejectedAwaitingDecisionRequestsDecision(){WarrantyClaim c=claim();c.setResult(WarrantyClaimResult.WARRANTY_REJECTED);c.setRejectionDecision(WarrantyRejectionDecision.AWAIT_CUSTOMER_DECISION);assertThat(calculator.calculate(c).code()).isEqualTo("RECORD_CUSTOMER_DECISION");}
 @Test void replacementApprovalOffersRecordFormWhenDue(){WarrantyClaim c=claim();c.setState(WarrantyClaimState.REPLACEMENT_APPROVED_AWAITING_PRODUCT);c.setExpectedReplacementDate(today);var result=calculator.calculate(c);assertThat(result.code()).isEqualTo("RECORD_REPLACEMENT");assertThat(result.recommendedActionFormKey()).isEqualTo("record-replacement");}
 private WarrantyClaim claim(){WarrantyClaim c=new WarrantyClaim();c.setActive(true);c.setResult(WarrantyClaimResult.PENDING);c.setState(WarrantyClaimState.VISIT_COMPLETED_AWAITING_RESULT);return c;}
}
