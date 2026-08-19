package uk.police.k9.dogs.config;

import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

import java.time.Clock;

/** The clock the services stamp {@code deletedAt} with, injected so a test can pin the time. */
@Factory
public class ClockFactory {

    @Singleton
    Clock clock() {
        return Clock.systemUTC();
    }
}
