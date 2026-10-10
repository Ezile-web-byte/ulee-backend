package com.ulee.ulee_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on @Scheduled jobs (used by AccountSuspensionService.processDue). Harmless if already enabled elsewhere. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}