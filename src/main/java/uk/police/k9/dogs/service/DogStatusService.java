package uk.police.k9.dogs.service;

import jakarta.inject.Singleton;
import uk.police.k9.dogs.entity.DogStatus;
import uk.police.k9.dogs.mapper.ReferenceDataMapper;
import uk.police.k9.dogs.repository.DogStatusRepository;

import java.time.Clock;

/** Maintains the statuses a dog can hold, and whatever the force adds next. */
@Singleton
public class DogStatusService extends ReferenceDataService<DogStatus> {

    public DogStatusService(DogStatusRepository repository, ReferenceDataMapper mapper, Clock clock) {
        super(repository, mapper, DogStatus::new, "Dog status", clock);
    }
}
