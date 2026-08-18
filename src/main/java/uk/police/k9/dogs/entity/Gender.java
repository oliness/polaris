package uk.police.k9.dogs.entity;

/**
 * The sex of a dog. Unlike status and leaving reason this is a closed set, so it stays an enum;
 * the values are still published at {@code GET /api/dogs/genders} for clients to build from.
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
