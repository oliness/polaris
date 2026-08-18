package uk.police.k9.dogs.config;

import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

import java.time.Clock;

/**
 * Publishes the clock the services use to stamp {@code deletedAt}. Taking it as a dependency lets
 * a test pin the time and assert on the exact value written.
 */
@Factory
public class ClockFactory {

    @Singleton
    Clock clock() {
        return Clock.systemUTC();
    }
}
