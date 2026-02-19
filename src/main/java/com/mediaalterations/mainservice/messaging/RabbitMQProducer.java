package com.mediaalterations.mainservice.messaging;

import com.mediaalterations.mainservice.dto.ProcessDto;
import com.mediaalterations.mainservice.entity.Process;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMQProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.process}")
    private String exchange;

    @Value("${rabbitmq.queue.process.created}")
    private String processCreatedQueue;

    public void publishProcessCreated(ProcessDto event){
        log.info("Process Created and publishing... id={}, command={}",event.id(),event.command());
        rabbitTemplate.convertAndSend(
                exchange,
                processCreatedQueue,
                event
        );
    }
}
