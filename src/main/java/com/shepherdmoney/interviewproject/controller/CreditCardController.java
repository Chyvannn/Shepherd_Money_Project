package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.BalanceHistory;
import com.shepherdmoney.interviewproject.model.CreditCard;
import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.repository.CreditCardRepository;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.vo.request.AddCreditCardToUserPayload;
import com.shepherdmoney.interviewproject.vo.request.UpdateBalancePayload;
import com.shepherdmoney.interviewproject.vo.response.CreditCardView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
public class CreditCardController {

    // TODO: wire in CreditCard repository here (~1 line)
    private final CreditCardRepository creditCardRepository;
    private final UserRepository userRepository;

    public CreditCardController(CreditCardRepository creditCardRepository, UserRepository userRepository) {
        this.creditCardRepository = creditCardRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/credit-card")
    public ResponseEntity<Integer> addCreditCardToUser(@RequestBody AddCreditCardToUserPayload payload) {
        // TODO: Create a credit card entity, and then associate that credit card with user with given userId
        //       Return 200 OK with the credit card id if the user exists and credit card is successfully associated with the user
        //       Return other appropriate response code for other exception cases
        //       Do not worry about validating the card number, assume card number could be any arbitrary format and length
        String cardNumber = payload.getCardNumber();
        String cardIssuanceBank = payload.getCardIssuanceBank();
        int userId = payload.getUserId();
        // Information is null
        if (cardNumber == null || cardIssuanceBank == null || cardNumber.isEmpty() || cardIssuanceBank.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        Optional<User> userOptional = userRepository.findById(userId);
        // No such user found
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        CreditCard creditCard = new CreditCard();
        creditCard.setUser(userOptional.get());
        creditCard.setIssuanceBank(cardIssuanceBank);
        creditCard.setNumber(cardNumber);
        return ResponseEntity.ok(creditCardRepository.save(creditCard).getId());
    }

    @GetMapping("/credit-card:all")
    public ResponseEntity<List<CreditCardView>> getAllCardOfUser(@RequestParam int userId) {
        // TODO: return a list of all credit card associated with the given userId, using CreditCardView class
        //       if the user has no credit card, return empty list, never return null
        Optional<User> userOptional = userRepository.findById(userId);
        List<CreditCardView> creditCardViews = new ArrayList<>();
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            for (CreditCard card : user.getCreditCards()) {
                creditCardViews.add(CreditCardView.builder()
                        .issuanceBank(card.getIssuanceBank())
                        .number(card.getNumber())
                        .build());
            }
            return ResponseEntity.ok(creditCardViews);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/credit-card:user-id")
    public ResponseEntity<Integer> getUserIdForCreditCard(@RequestParam String creditCardNumber) {
        // TODO: Given a credit card number, efficiently find whether there is a user associated with the credit card
        //       If so, return the user id in a 200 OK response. If no such user exists, return 400 Bad Request
        if (creditCardNumber == null || creditCardNumber.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } else {
            List<CreditCard> creditCards = creditCardRepository.findByNumber(creditCardNumber);
            if (!creditCards.isEmpty()) {
                return ResponseEntity.ok(creditCards.get(0).getId());
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        }
    }

    @PostMapping("/credit-card:update-balance")
    public ResponseEntity<Integer> postCreditCardTransaction(@RequestBody UpdateBalancePayload[] payload) {
        //TODO: Given a list of transactions, update credit cards' balance history.
        //      For example: if today is 4/12, a credit card's balanceHistory is [{date: 4/12, balance: 110}, {date: 4/10, balance: 100}],
        //      Given a transaction of {date: 4/10, amount: 10}, the new balanceHistory is
        //      [{date: 4/12, balance: 120}, {date: 4/11, balance: 110}, {date: 4/10, balance: 100}]
        //      Return 200 OK if update is done and successful, 400 Bad Request if the given card number
        //      is not associated with a card.

        // If empty or null payload
        if (payload == null || payload.length == 0) {
            return ResponseEntity.badRequest().build();
        }
        // Get current date
        Instant currentTime = Instant.now().truncatedTo(ChronoUnit.DAYS);

        // Update for each transaction
        for (UpdateBalancePayload trans : payload) {
            String creditCardNumber = trans.getCreditCardNumber();
            Instant transTime = trans.getTransactionTime();
            double transAmount = trans.getTransactionAmount();

            List<CreditCard> creditCards = creditCardRepository.findByNumber(creditCardNumber);
            // no credit card found
            if (creditCards.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            CreditCard creditCard = creditCards.get(0);
            List<BalanceHistory> histories = creditCard.getBalanceHistoryList();

            BalanceHistory earliestHistory = histories.get(histories.size() - 1);
            BalanceHistory latestHistory = histories.get(0);

            if (earliestHistory.getDate().isAfter(transTime.plus(1, ChronoUnit.DAYS))) {
                BalanceHistory tempHistory = new BalanceHistory();
                tempHistory.setDate(transTime);
                tempHistory.setBalance(earliestHistory.getBalance());
                histories.add(histories.size(), tempHistory);

                tempHistory = new BalanceHistory();
                tempHistory.setDate(transTime.plus(1, ChronoUnit.DAYS));
                tempHistory.setBalance(earliestHistory.getBalance() + transAmount);
                histories.add(histories.size() - 1, tempHistory);
            }
            if (earliestHistory.getDate().equals(transTime.plus(1, ChronoUnit.DAYS))) {
                BalanceHistory tempHistory = histories.get(histories.size() - 2);
                tempHistory.setBalance(tempHistory.getBalance() + transAmount);
            }
            for (int i = histories.size() - 1; i > 0; i--) {
                 BalanceHistory earlierHistory = histories.get(i);
                 BalanceHistory laterHistory = histories.get(i - 1);
                 if (transTime.isAfter(laterHistory.getDate())) {
                     continue;
                 }
                 if (transTime.equals(earlierHistory.getDate().minus(1, ChronoUnit.DAYS))) {
                     earlierHistory.setBalance(earlierHistory.getBalance() + transAmount);
                 }
                 if (earlierHistory.getDate().isBefore(transTime) && laterHistory.getDate().isAfter(transTime)) {
                     BalanceHistory newHistory = new BalanceHistory();
                     newHistory.setDate(transTime);
                     newHistory.setBalance(earliestHistory.getBalance());
                     histories.add(i, newHistory);
                     if (!transTime.plus(1, ChronoUnit.DAYS).equals(laterHistory.getDate())) {
                         BalanceHistory tempHistory = new BalanceHistory();
                         tempHistory.setDate(transTime.plus(1, ChronoUnit.DAYS));
                         tempHistory.setBalance(earlierHistory.getBalance() + transAmount);
                         histories.add(i, tempHistory);
                     }
                 }
                 if (earlierHistory.getDate().isAfter(transTime)) {
                     earlierHistory.setBalance(earlierHistory.getBalance() + transAmount);
                 }
            }
            if (latestHistory.getDate().isAfter(transTime.plus(1, ChronoUnit.DAYS))) {
                latestHistory.setBalance(latestHistory.getBalance() + transAmount);
            } else if (latestHistory.getDate().equals(transTime.plus(1, ChronoUnit.DAYS))) {
                latestHistory.setBalance(latestHistory.getBalance() + transAmount);
            } else if (latestHistory.getDate().equals(transTime)) {
                BalanceHistory tempHistory = new BalanceHistory();
                tempHistory.setDate(transTime.plus(1, ChronoUnit.DAYS));
                tempHistory.setBalance(latestHistory.getBalance() + transAmount);
                histories.add(0, tempHistory);
            } else {
                BalanceHistory tempHistory = new BalanceHistory();
                tempHistory.setDate(transTime);
                tempHistory.setBalance(latestHistory.getBalance());
                histories.add(0, tempHistory);

                tempHistory = new BalanceHistory();
                tempHistory.setDate(transTime.plus(1, ChronoUnit.DAYS));
                tempHistory.setBalance(latestHistory.getBalance() + transAmount);
                histories.add(0, tempHistory);
            }
        }
        return ResponseEntity.ok().build();
    }
    
}
