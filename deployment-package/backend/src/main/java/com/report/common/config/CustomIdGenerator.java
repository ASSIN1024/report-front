package com.report.common.config;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class CustomIdGenerator implements IdentifierGenerator {

    private final AtomicLong lastTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public Number nextId(Object entity) {
        long currentTime = System.currentTimeMillis();
        long lastTimestamp = lastTime.get();
        
        if (currentTime < lastTimestamp) {
            log.warn("Clock moved backwards, using last timestamp + sequence");
            currentTime = lastTimestamp;
        }
        
        if (currentTime == lastTimestamp) {
            long seq = sequence.incrementAndGet();
            if (seq > 999) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                currentTime = System.currentTimeMillis();
                sequence.set(0);
            }
        } else {
            lastTime.set(currentTime);
            sequence.set(0);
        }
        
        return currentTime * 10000 + sequence.get();
    }
}
