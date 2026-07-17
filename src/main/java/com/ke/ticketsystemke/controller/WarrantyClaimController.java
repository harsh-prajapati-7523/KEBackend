package com.ke.ticketsystemke.controller;
import com.ke.ticketsystemke.dto.*; import com.ke.ticketsystemke.service.WarrantyClaimService; import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/volt/tickets/{ticketId}/warranty")
public class WarrantyClaimController {
 private final WarrantyClaimService service; public WarrantyClaimController(WarrantyClaimService service){this.service=service;}
 @GetMapping public ResponseEntity<WarrantyClaimResponse> get(@PathVariable Long ticketId,Authentication auth){WarrantyClaimResponse response=service.get(ticketId,auth.getName());return response==null?ResponseEntity.noContent().build():ResponseEntity.ok(response);}
 @PostMapping public ResponseEntity<WarrantyClaimResponse> create(@PathVariable Long ticketId,@Valid @RequestBody(required=false) CreateWarrantyClaimRequest request,Authentication auth){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(ticketId,request==null?new CreateWarrantyClaimRequest():request,auth.getName()));}
 @PatchMapping("/{claimId}") public WarrantyClaimResponse update(@PathVariable Long ticketId,@PathVariable Long claimId,@Valid @RequestBody UpdateWarrantyClaimRequest request,Authentication auth){return service.update(ticketId,claimId,request,auth.getName());}
 @GetMapping("/{claimId}/events") public WarrantyClaimEventPageResponse events(@PathVariable Long ticketId,@PathVariable Long claimId,@RequestParam(required=false) Integer page,@RequestParam(required=false) Integer size,Authentication auth){return service.history(ticketId,claimId,page,size,auth.getName());}
}
