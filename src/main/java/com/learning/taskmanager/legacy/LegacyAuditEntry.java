package com.learning.taskmanager.legacy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Pre-2018 entity. Predates the rest of this repo's conventions:
 *   - column names are snake_case (modern entities here use defaults)
 *   - the `kind` field is an upper-case string, not an enum
 *   - timestamps use millis since epoch in a `BIGINT`, not Instant
 *   - field names diverge from getter names in places (look closely)
 *
 * Don't try to "modernize" it during this exercise — code in other modules
 * still depends on the wire format. Just integrate WITH it as-is.
 */
@Entity
@Table(name = "legacy_audit_log")
public class LegacyAuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long auditId;

    @Column(name = "audit_kind", nullable = false, length = 32)
    private String kind;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "actor", length = 64)
    private String actor;

    @Column(name = "occurred_at_millis", nullable = false)
    private Long occurredAtMillis;

    public LegacyAuditEntry() {
    }

    public Long getAuditId() {
        return auditId;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public Long getOccurredAtMillis() {
        return occurredAtMillis;
    }

    public void setOccurredAtMillis(Long occurredAtMillis) {
        this.occurredAtMillis = occurredAtMillis;
    }

    public Instant getOccurredAtInstant() {
        return occurredAtMillis == null ? null : Instant.ofEpochMilli(occurredAtMillis);
    }
}
