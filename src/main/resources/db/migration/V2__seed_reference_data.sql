-- The values the force currently recognises, held in tables so they can be maintained through
-- /api/dogs/statuses and /api/dogs/leaving-reasons without a schema change.
INSERT INTO dog_status (code, label, description, display_order, created_at, updated_at)
VALUES ('IN_TRAINING', 'In Training', 'Undergoing initial or conversion training.', 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('IN_SERVICE', 'In Service', 'Operationally deployed with a handler.', 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('RETIRED', 'Retired', 'No longer operational but still on the register.', 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('LEFT', 'Left', 'No longer with the force.', 40, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO leaving_reason (code, label, description, display_order, created_at, updated_at)
VALUES ('TRANSFERRED', 'Transferred', 'Transferred to another force or agency.', 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('RETIRED_PUT_DOWN', 'Retired (Put Down)', 'Retired and euthanised on veterinary advice.', 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('KIA', 'KIA', 'Killed in action.', 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('REJECTED', 'Rejected', 'Did not meet the standard required for service.', 40, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('RETIRED_REHOUSED', 'Retired (Re-housed)', 'Retired and re-housed, usually with the handler.', 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('DIED', 'Died', 'Died of natural causes or illness.', 60, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
