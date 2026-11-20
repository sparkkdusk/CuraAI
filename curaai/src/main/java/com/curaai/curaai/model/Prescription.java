package com.curaai.curaai.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A single prescription upload/analysis, owned by a user.
 * Holds the raw OCR text plus the structured medications extracted from it.
 */
@Entity
@Table(name = "prescriptions")
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    /** Optional user-facing label, e.g. "Dr. Rahman - July checkup", to tell history entries apart. */
    private String label;

    // NOTE: intentionally NOT @Lob - see ChatMessage.content for why. @Lob on
    // a String maps to a Postgres large object (OID), which requires a real
    // transaction to stream and throws "Large Objects may not be used in
    // auto-commit mode" on a plain read. columnDefinition = "TEXT" alone is
    // an unlimited-length Postgres text column with no such requirement.
    @Column(columnDefinition = "TEXT")
    private String rawText;

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PrescriptionMedication> medications = new ArrayList<>();

    public Prescription() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }

    public List<PrescriptionMedication> getMedications() { return medications; }
    public void setMedications(List<PrescriptionMedication> medications) { this.medications = medications; }
}