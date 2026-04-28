package com.learning.taskmanager.legacy;

import java.util.List;

/**
 * Pre-2018 DAO interface. Predates Spring Data JPA in this codebase.
 *
 * NOTE: callers MUST use {@link #recordEvent(String, Long, String)} to write —
 * never construct a LegacyAuditEntry yourself, because the implementation is
 * the only place that knows how to fill in occurredAtMillis correctly (it
 * rounds to the second to deduplicate across replicas — see impl).
 */
public interface LegacyAuditDao {

    /**
     * Record a new audit event. Returns the persisted entry, including its
     * generated auditId.
     */
    LegacyAuditEntry recordEvent(String kind, Long subjectId, String actor);

    /**
     * Find all entries for a given subject, newest first.
     */
    List<LegacyAuditEntry> findBySubject(Long subjectId);

    /**
     * Count of entries with the given kind.
     */
    long countByKind(String kind);
}
