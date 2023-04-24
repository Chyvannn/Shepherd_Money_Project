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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
        }
        return ResponseEntity.ok(creditCardViews);
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
                return ResponseEntity.ok(creditCards.get(0).getUser().getId());
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

            BalanceHistory sameDayHistory = new BalanceHistory();
            sameDayHistory.setDate(transTime);
            sameDayHistory.setCreditCard(creditCard);
            BalanceHistory nextDayHistory = new BalanceHistory();
            nextDayHistory.setDate(transTime.plus(1, ChronoUnit.DAYS));
            nextDayHistory.setCreditCard(creditCard);

            boolean found = false;
            double curBalance = histories.get(histories.size() - 1).getBalance();
            for (int i = histories.size() - 2; i >= 0; i--) {
                BalanceHistory curHistory = histories.get(i);
                BalanceHistory prevHistory = histories.get(i + 1);
                // if the insert pos is not found
                if (!found) {
                    if (curHistory.getDate().isAfter(transTime)) {
                        // if the transaction happened before the previous history
                        if (prevHistory.getDate().isAfter(transTime)) {
                            sameDayHistory.setBalance(curBalance);
                            histories.add(i + 2, sameDayHistory);
                            prevHistory.setBalance(prevHistory.getBalance() + transAmount);
                            // if the prevHistory is more than 1 day after transaction, one more history add
                            if (prevHistory.getDate().isAfter(nextDayHistory.getDate())) {
                                nextDayHistory.setBalance(curBalance + transAmount);
                                histories.add(i + 2, nextDayHistory);
                            } // otherwise, prevHistory is nextDayHistory
                        } // otherwise the transaction happened on prevHistory date, no change to prevHistory
                        sameDayHistory.setBalance(sameDayHistory.getBalance() + transAmount);
                        found = true;
                    } // trans happened on curHistory will be handled in next iteration
                } else {
                    // if already found, only update balance is needed
                    curHistory.setBalance(curHistory.getBalance() + transAmount);
                }
                curBalance = curHistory.getBalance();
            }
            // if still not found after iteration, trans happened after all history
            if (!found) {
                sameDayHistory.setBalance(curBalance);
                histories.add(0, sameDayHistory);
                nextDayHistory.setBalance(curBalance + transAmount);
                histories.add(0, nextDayHistory);
                curBalance = curBalance + transAmount;
            }
            // check if the top history is the current data
            Instant currentTime = Instant.now().truncatedTo(ChronoUnit.DAYS);
            LocalDate firstDate = LocalDateTime.ofInstant(histories.get(0).getDate(), ZoneId.systemDefault()).toLocalDate();
            LocalDate currentDate = LocalDateTime.ofInstant(currentTime, ZoneId.systemDefault()).toLocalDate();
            if (!firstDate.equals(currentDate)) {
                 BalanceHistory topHistory = new BalanceHistory();
                 topHistory.setCreditCard(creditCard);
                 topHistory.setDate(currentTime);
                 topHistory.setBalance(curBalance);
                 histories.add(0, topHistory);
            }
        }
        return ResponseEntity.ok().build();
    }
    
}
