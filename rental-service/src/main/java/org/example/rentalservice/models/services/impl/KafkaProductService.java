package org.example.rentalservice.models.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProductService {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendRentalCreated(String email) {
        kafkaTemplate.send("rental-created", email);
    }
}
