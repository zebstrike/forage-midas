package com.jpmc.midascore.services;

import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.foundation.Incentive;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class IncentiveService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String INCENTIVE_API_URL = "http://localhost:8080/incentive";

    public Incentive getIncentive(Transaction transaction) {
        return restTemplate.postForObject(INCENTIVE_API_URL, transaction, Incentive.class);
    }
}
