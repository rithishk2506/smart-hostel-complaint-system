package com.hostelcomplaint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.jms.annotation.EnableJms;

@EnableJms
@SpringBootApplication
@EnableScheduling

public class SmartHostelComplaintSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                SmartHostelComplaintSystemApplication.class,
                args
        );
    }
}