package uk.police.k9.dogs.controller;

/** Documentation shared by the paging parameters, which every list endpoint describes alike. */
final class ApiParameters {

    static final String PAGE = "Zero-based index of the page to return";

    static final String SIZE = "Results per page. A larger request is capped at 100 rather than refused.";

    static final String SORT = "Property and direction to order by. Repeat the parameter to sort on "
            + "more than one property.";

    static final String ETAG = "The record's version, to send back as If-Match on the next update";

    static final String IF_MATCH = "The entity tag last read for this record, e.g. \"3\". When it "
            + "no longer matches, the update is refused with 412 rather than overwriting the "
            + "change that was made in between. Omit it, or send *, to update unconditionally.";

    private ApiParameters() {
    }
}
