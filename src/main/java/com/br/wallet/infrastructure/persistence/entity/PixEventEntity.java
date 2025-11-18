package com.br.wallet.infrastructure.persistence.entity;

import com.br.wallet.domain.enums.PixEventType;
import com.br.wallet.domain.model.PixEvent;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "pix_events",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_pix_events_event_id", columnNames = "event_id")
        }
)
@Getter
@Setter
public class PixEventEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private String eventId;

    @Column(name = "end_to_end_id", nullable = false, updatable = false)
    private String endToEndId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private PixEventType eventType;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public PixEventEntity() {
    }

    public PixEventEntity(UUID id, String eventId, String endToEndId, PixEventType eventType, Instant occurredAt, Instant processedAt) {
        this.id = id;
        this.eventId = eventId;
        this.endToEndId = endToEndId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.processedAt = processedAt;
    }

    public static PixEventEntity fromDomain(PixEvent event) {
        return new PixEventEntity(
                event.id(),
                event.eventId(),
                event.endToEndId(),
                event.eventType(),
                event.occurredAt(),
                event.processedAt()
        );
    }

    public PixEvent toDomain() {
        return new PixEvent(
                this.id,
                this.eventId,
                this.endToEndId,
                this.eventType,
                this.occurredAt,
                this.processedAt
        );
    }
}
