package uk.police.k9.dogs.controller;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Produces;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.tags.Tag;
import uk.police.k9.dogs.service.LeavingReasonService;

/**
 * The reasons a dog can leave the force. As with the statuses, the task lists the reasons that are
 * <em>currently</em> possible, so they are maintained as data rather than code.
 */
@Controller(ApiPaths.LEAVING_REASONS)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ExecuteOn(TaskExecutors.BLOCKING)
@Validated
@Tag(name = "Leaving reasons")
public class LeavingReasonController extends ReferenceDataController {

    public LeavingReasonController(LeavingReasonService leavingReasonService) {
        super(leavingReasonService, ApiPaths.LEAVING_REASONS);
    }
}
