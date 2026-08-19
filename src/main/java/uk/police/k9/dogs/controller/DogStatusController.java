package uk.police.k9.dogs.controller;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Produces;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.tags.Tag;
import uk.police.k9.dogs.service.DogStatusService;

/**
 * The statuses a dog can hold. The task lists the values <em>currently</em> possible, so they are
 * rows the force maintains rather than an enum that would need a release.
 */
@Controller(ApiPaths.STATUSES)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ExecuteOn(TaskExecutors.BLOCKING)
@Validated
@Tag(name = "Dog statuses")
public class DogStatusController extends ReferenceDataController {

    public DogStatusController(DogStatusService dogStatusService) {
        super(dogStatusService, ApiPaths.STATUSES);
    }
}
