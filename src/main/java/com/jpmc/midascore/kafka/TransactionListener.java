package com.jpmc.midascore.kafka;

import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.repository.UserRepository;
import com.jpmc.midascore.repository.TransactionRepository;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.time.LocalDateTime;

@Service
public class TransactionListener {

    private static final Logger logger = LoggerFactory.getLogger(TransactionListener.class);

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final RestTemplate restTemplate;

    // Store received transactions for debugging/tests
    private final List<Transaction> receivedTransactions = new ArrayList<>();

    // Constructor injection
    public TransactionListener(UserRepository userRepository,
                               TransactionRepository transactionRepository,
                               RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.restTemplate = restTemplate;
    }

    @KafkaListener(topics = "${general.kafka-topic}", groupId = "midas-core-group")
    public void consume(ConsumerRecord<String, Transaction> record) {
        Transaction transaction = record.value();
        logger.info("✅ Received transaction: {}", transaction);

        // Look up sender & recipient
        Optional<UserRecord> senderOpt = userRepository.findById(transaction.getSenderId());
        Optional<UserRecord> recipientOpt = userRepository.findById(transaction.getRecipientId());

        if (senderOpt.isEmpty() || recipientOpt.isEmpty()) {
            logger.warn("⚠️ Sender or recipient not found for transaction {}", transaction);
            return; // skip this transaction
        }

        UserRecord sender = senderOpt.get();
        UserRecord recipient = recipientOpt.get();

        // Check balance
        if (sender.getBalance() < transaction.getAmount()) {
            logger.warn("❌ Insufficient funds for sender {} (balance: {}, tried: {})",
                    sender.getId(), sender.getBalance(), transaction.getAmount());
            return;
        }

        // Deduct from sender
        sender.setBalance(sender.getBalance() - transaction.getAmount());

        // Credit recipient with transaction amount
        recipient.setBalance(recipient.getBalance() + transaction.getAmount());

        // ✅ Call Incentive API
        String url = "http://localhost:8080/incentive";
        Incentive incentive = restTemplate.postForObject(url, transaction, Incentive.class);

        float incentiveAmount = (incentive != null) ? incentive.getAmount() : 0f;
        logger.info("🎁 Incentive received: {}", incentiveAmount);

        // Add incentive to recipient only
        recipient.setBalance(recipient.getBalance() + incentiveAmount);

        // Save updated users
        userRepository.save(sender);
        userRepository.save(recipient);

        // Create and save transaction record (with incentive)
        TransactionRecord recordEntity =
                new TransactionRecord(sender, recipient, transaction.getAmount(), incentiveAmount, LocalDateTime.now());
        transactionRepository.save(recordEntity);

        // Keep in memory (for debugging/tests)
        receivedTransactions.add(transaction);

        logger.info("💾 Transaction persisted: {}", recordEntity);
    }

    public List<Transaction> getReceivedTransactions() {
        return receivedTransactions;
    }
}
