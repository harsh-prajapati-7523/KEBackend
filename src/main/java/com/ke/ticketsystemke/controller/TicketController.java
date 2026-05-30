package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.entity.Ticket;
import com.ke.ticketsystemke.service.TicketService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tickets")
@CrossOrigin(origins = "*")
public class TicketController {

    @Autowired
    private TicketService service;

    @PostMapping("/create")
    public Ticket createTicket(
            @RequestBody Ticket ticket) {
        return service.saveTicket(ticket);
    }
}