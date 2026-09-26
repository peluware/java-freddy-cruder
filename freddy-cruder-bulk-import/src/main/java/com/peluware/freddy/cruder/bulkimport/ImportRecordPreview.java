package com.peluware.freddy.cruder.bulkimport;

import java.util.List;

/**
 * A record as it was read: to be created, skipped or rejected.
 *
 * @param <PREVIEW> the type of the data of the record
 */
public sealed interface ImportRecordPreview<PREVIEW> {

    /**
     * A record that will be created.
     *
     * @param position  the position of the record in its origin
     * @param preview   the data of the record
     * @param <PREVIEW> the type of the data of the record
     */
    record Created<PREVIEW>(int position, PREVIEW preview) implements ImportRecordPreview<PREVIEW> {
    }

    /**
     * A record that will be left alone.
     *
     * @param position  the position of the record in its origin
     * @param preview   the data of the record
     * @param reason    why it is left alone
     * @param <PREVIEW> the type of the data of the record
     */
    record Skipped<PREVIEW>(int position, PREVIEW preview, String reason) implements ImportRecordPreview<PREVIEW> {
    }

    /**
     * A record the source could not convert.
     *
     * @param position  the position of the record in its origin
     * @param preview   the data of the record
     * @param problems  what the source found wrong with it; never empty
     * @param <PREVIEW> the type of the data of the record
     */
    record Rejected<PREVIEW>(int position, PREVIEW preview, List<ImportProblem> problems) implements ImportRecordPreview<PREVIEW> {

        public Rejected {
            if (problems.isEmpty()) {
                throw new IllegalArgumentException("A rejected record needs at least one problem");
            }
            problems = List.copyOf(problems);
        }
    }

    /**
     * @return the position of the record in its origin
     */
    int position();

    /**
     * @return the data of the record
     */
    PREVIEW preview();

    /**
     * @return whether the source could not convert the record
     */
    default boolean rejected() {
        return this instanceof Rejected<PREVIEW>;
    }

    /**
     * @return whether the record is left alone
     */
    default boolean skipped() {
        return this instanceof Skipped<PREVIEW>;
    }
}
