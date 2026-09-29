package org.example.notifyservice.consumer;

import org.example.notifyservice.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RentalCreatedConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(RentalCreatedConsumer.class);

    private final EmailService emailService;

    public RentalCreatedConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(
            topics = "${notification.kafka.rental-created-topic:rental-created}",
            groupId = "${spring.kafka.consumer.group-id:notify-service}"
    )
    public void consume(String email) {
        if (!StringUtils.hasText(email)) {
            LOG.warn("Loi");
            return;
        }
        LOG.info("Thanh cong: {}", email);
        emailService.sendRentalCreatedEmail(email);
        LOG.info("Da gui email xac nhan: {}", email);
    }
}
