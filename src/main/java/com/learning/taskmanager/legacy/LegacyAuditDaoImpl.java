package com.learning.taskmanager.legacy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Hand-written EntityManager-based implementation. Predates Spring Data JPA.
 * Don't migrate it to a JpaRepository — the team explicitly preserved this
 * pattern because the rest of the legacy module wires up similarly.
 *
 * The Java field names here intentionally diverge from the entity column
 * names — that's how this module worked in 2017 and external integrations
 * depend on the column-name wire format.
 */
@Repository
public class LegacyAuditDaoImpl implements LegacyAuditDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public LegacyAuditEntry recordEvent(String kind, Long subjectId, String actor) {
        LegacyAuditEntry entry = new LegacyAuditEntry();
        entry.setKind(kind == null ? "UNKNOWN" : kind.toUpperCase());
        entry.setSubjectId(subjectId);
        entry.setActor(actor == null ? "system" : actor);
        // Round to the second so duplicate writes from replicas dedupe at the row level.
        long secondPrecisionMillis = (Instant.now().toEpochMilli() / 1000L) * 1000L;
        entry.setOccurredAtMillis(secondPrecisionMillis);
        em.persist(entry);
        em.flush();
        return entry;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LegacyAuditEntry> findBySubject(Long subjectId) {
        return em.createQuery(
                        "SELECT e FROM LegacyAuditEntry e WHERE e.subjectId = :sid ORDER BY e.occurredAtMillis DESC",
                        LegacyAuditEntry.class)
                .setParameter("sid", subjectId)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByKind(String kind) {
        return em.createQuery(
                        "SELECT COUNT(e) FROM LegacyAuditEntry e WHERE e.kind = :k",
                        Long.class)
                .setParameter("k", kind == null ? "UNKNOWN" : kind.toUpperCase())
                .getSingleResult();
    }
}
