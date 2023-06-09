package com.shepherdmoney.interviewproject.model;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.*;
import net.minidev.json.annotate.JsonIgnore;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class BalanceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    private Instant date;

    private double balance;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name="card_id", nullable = false)
    @ToString.Exclude
    private CreditCard creditCard;
}
