package com.jpmc.midascore.services;

import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.repository.UserRepository;
import com.jpmc.midascore.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public UserService(UserRepository userRepository, TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    // ✅ New method: Get all users
    public Iterable<UserRecord> getAllUsers() {
        return userRepository.findAll();
    }

    // Find user by ID (throws exception if not found)
    public UserRecord getUserById(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("❌ User not found with id: " + id));
    }

    // Transfer money between users
    public void transferMoney(long senderId, long recipientId, float amount) {
        UserRecord sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("❌ Sender not found with id: " + senderId));

        UserRecord recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new RuntimeException("❌ Recipient not found with id: " + recipientId));

        if (sender.getBalance() < amount) {
            throw new RuntimeException("❌ Insufficient balance for sender: " + senderId);
        }

        // Update balances
        sender.setBalance(sender.getBalance() - amount);
        recipient.setBalance(recipient.getBalance() + amount);

        // Save updated balances
        userRepository.save(sender);
        userRepository.save(recipient);

        // Record transaction
        TransactionRecord record = new TransactionRecord(sender, recipient, amount);
        transactionRepository.save(record);
    }
}
