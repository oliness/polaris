package uk.police.k9.dogs.entity;

/**
 * The sex of a dog. A closed set, unlike status and leaving reason, so it stays an enum; the
 * values are published at {@code GET /api/dogs/genders}.
 */
public enum Gender {

    MALE("Male"),
    FEMALE("Female");

    private final String label;

    Gender(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
