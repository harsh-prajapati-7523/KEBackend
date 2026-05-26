package com.ke.ticketsystemke.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerName;

    private String mobileNo;

    private String equipmentName;

    private String issue;

    private String address;

    private String priority;

    private String serviceDate;

    @Column(length = 1000)
    private String description;
}