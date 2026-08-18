package uk.police.k9.dogs.controller;

/**
 * The routes the API exposes. Every one sits under {@link #ROOT}, as the task requires, so keeping
 * the prefix in one constant makes that checkable rather than spread across five controllers.
 */
public final class ApiPaths {

    public static final String ROOT = "/api/dogs";

    public static final String DOGS = ROOT + "/dogs";

    public static final String SUPPLIERS = ROOT + "/suppliers";

    public static final String STATUSES = ROOT + "/statuses";

    public static final String LEAVING_REASONS = ROOT + "/leaving-reasons";

    public static final String GENDERS = ROOT + "/genders";

    private ApiPaths() {
    }
}
