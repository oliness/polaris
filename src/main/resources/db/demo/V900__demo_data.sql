--
-- Sample data for local exploration. This migration is only on the Flyway path when the
-- 'dev' environment is active (see application-dev.properties), so tests and production
-- start from an empty register.
--
INSERT INTO supplier (name, contact_name, contact_email, contact_phone, address, created_at, updated_at)
VALUES ('Ravenscroft Working Dogs', 'Marie Ravenscroft', 'kennels@ravenscroft.example', '01592 555 210',
        'Ravenscroft Farm, Kinross, KY13 9XX', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('Northgate Kennels', 'Declan Barr', 'enquiries@northgate.example', '0191 555 8842',
        '17 Northgate Road, Newcastle upon Tyne, NE4 6RT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('Ashcombe Malinois', 'Priya Raman', 'priya@ashcombe.example', '01458 555 019',
        'Ashcombe Lane, Glastonbury, BA6 8LT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO dog (name, breed, supplier_id, badge_id, gender, birth_date, date_acquired, status_id,
                 leaving_date, leaving_reason_id, kennelling_characteristic, created_at, updated_at)
SELECT d.name,
       d.breed,
       (SELECT id FROM supplier WHERE name = d.supplier_name),
       d.badge_id,
       d.gender,
       d.birth_date,
       d.date_acquired,
       (SELECT id FROM dog_status WHERE code = d.status_code),
       d.leaving_date,
       (SELECT id FROM leaving_reason WHERE code = d.leaving_reason_code),
       d.kennelling_characteristic,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM (VALUES ('Baxter', 'German Shepherd', 'Ravenscroft Working Dogs', 'K9-1041', 'MALE', DATE '2020-03-14',
              DATE '2021-01-06', 'IN_SERVICE', NULL, NULL,
              'Settles quickly. Must not be kennelled next to entire males.'),
             ('Nala', 'Belgian Malinois', 'Ashcombe Malinois', 'K9-1042', 'FEMALE', DATE '2021-06-02',
              DATE '2022-02-14', 'IN_SERVICE', NULL, NULL,
              'High drive - remove all toys from the kennel overnight.'),
             ('Rufus', 'Springer Spaniel', 'Northgate Kennels', 'K9-1043', 'MALE', DATE '2019-11-21',
              DATE '2020-09-30', 'RETIRED', NULL, NULL,
              'Elderly. Needs orthopaedic bedding and a low step.'),
             ('Sable', 'Belgian Malinois', 'Ashcombe Malinois', 'K9-1044', 'FEMALE', DATE '2023-04-18',
              DATE '2024-01-22', 'IN_TRAINING', NULL, NULL,
              'Noise sensitive. Kennel away from the vehicle bay.'),
             ('Tank', 'German Shepherd', 'Ravenscroft Working Dogs', 'K9-1045', 'MALE', DATE '2018-02-09',
              DATE '2019-05-11', 'LEFT', DATE '2025-03-31', 'RETIRED_REHOUSED',
              'Re-housed with former handler.'),
             ('Willow', 'Cocker Spaniel', 'Northgate Kennels', 'K9-1046', 'FEMALE', DATE '2022-08-30',
              DATE '2023-06-05', 'IN_SERVICE', NULL, NULL,
              'Food guarding - feed separately.'),
             ('Ziggy', 'Dutch Herder', 'Ashcombe Malinois', NULL, 'MALE', DATE '2024-05-02',
              DATE '2025-02-17', 'IN_TRAINING', NULL, NULL,
              'Chews bedding. Rubber matting only.')) AS d (name, breed, supplier_name, badge_id, gender,
                                                            birth_date, date_acquired, status_code,
                                                            leaving_date, leaving_reason_code,
                                                            kennelling_characteristic);

-- One soft-deleted record, so that the effect of the default list filter is visible.
INSERT INTO dog (name, breed, supplier_id, badge_id, gender, birth_date, date_acquired, status_id,
                 leaving_date, leaving_reason_id, kennelling_characteristic, created_at, updated_at, deleted_at)
VALUES ('Bruno', 'Rottweiler',
        (SELECT id FROM supplier WHERE name = 'Northgate Kennels'), 'K9-1039', 'MALE',
        DATE '2017-07-07', DATE '2018-04-02',
        (SELECT id FROM dog_status WHERE code = 'LEFT'), DATE '2024-10-14',
        (SELECT id FROM leaving_reason WHERE code = 'DIED'),
        'Record retained for audit only.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
