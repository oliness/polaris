package uk.police.k9.dogs.service;

import jakarta.inject.Singleton;
import uk.police.k9.dogs.entity.LeavingReason;
import uk.police.k9.dogs.mapper.ReferenceDataMapper;
import uk.police.k9.dogs.repository.LeavingReasonRepository;

import java.time.Clock;

/** Maintains the reasons a dog can leave the force, and whatever the force adds next. */
@Singleton
public class LeavingReasonService extends ReferenceDataService<LeavingReason> {

    public LeavingReasonService(LeavingReasonRepository repository, ReferenceDataMapper mapper, Clock clock) {
        super(repository, mapper, LeavingReason::new, "Leaving reason", clock);
    }
}
