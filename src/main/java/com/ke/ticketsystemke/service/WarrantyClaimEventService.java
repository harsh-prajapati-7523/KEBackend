package com.ke.ticketsystemke.service;
import com.ke.ticketsystemke.entity.*; import com.ke.ticketsystemke.repository.*; import org.springframework.http.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.*; import org.springframework.web.server.ResponseStatusException; import java.util.Map;
@Service
public class WarrantyClaimEventService {
 private final WarrantyClaimEventRepository events; private final EmployeeRepository employees;
 public WarrantyClaimEventService(WarrantyClaimEventRepository e,EmployeeRepository employees){this.events=e;this.employees=employees;}
 @Transactional(propagation=Propagation.MANDATORY)
 public void record(WarrantyClaim claim,WarrantyClaimEventType type,String actor,String summary,Map<String,?> changed){
  WarrantyClaimEvent event=new WarrantyClaimEvent(); event.setWarrantyClaim(claim); event.setTicketId(claim.getTicket().getId()); event.setEventType(type); event.setActorEmployeeId(actor);
  event.setActorNameSnapshot(employees.findByEmployeeIdIgnoreCase(actor==null?"":actor.trim()).map(Employee::getName).orElse(null)); event.setSummary(summary);
  if(changed!=null&&!changed.isEmpty()) event.setChangedFieldsJson(toSafeJson(changed));
  events.save(event);
 }
 private String toSafeJson(Map<String,?> values){StringBuilder json=new StringBuilder("{");boolean first=true;for(var entry:values.entrySet()){if(!first)json.append(',');first=false;json.append(quote(entry.getKey())).append(':');Object value=entry.getValue();if(value instanceof Map<?,?> nested){json.append('{');boolean nestedFirst=true;for(var item:nested.entrySet()){if(!nestedFirst)json.append(',');nestedFirst=false;json.append(quote(String.valueOf(item.getKey()))).append(':').append(quote(String.valueOf(item.getValue())));}json.append('}');}else json.append(quote(String.valueOf(value)));}return json.append('}').toString();}
 private String quote(String value){return "\""+value.replace("\\","\\\\").replace("\"","\\\"").replace("\n"," ").replace("\r"," ")+"\"";}
}
