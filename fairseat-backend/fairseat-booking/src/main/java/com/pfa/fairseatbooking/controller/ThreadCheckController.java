package com.pfa.fairseatbooking.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/v1/booking/threads")
@Slf4j
public class ThreadCheckController {

    @GetMapping("/inspect")
    public ResponseEntity<Map<String, String>> inspectCurrentThread() {
        Thread currentThread = Thread.currentThread();

        String threadName = currentThread.toString();
        boolean isVirtual = currentThread.isVirtual();

        log.info("Inspected thread execution context: Name [{}], IsVirtual [{}]", threadName, isVirtual);

        return ResponseEntity.ok(Map.of(
                "threadDescription", threadName,
                "isVirtualThread", String.valueOf(isVirtual)
        ));
    }
}