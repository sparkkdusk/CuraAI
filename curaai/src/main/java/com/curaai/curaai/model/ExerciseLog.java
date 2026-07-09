package com.curaai.curaai.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "exercise_logs")
public class ExerciseLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date = LocalDate.now();

    @Column(nullable = false)
    private String activityType;

    @Column(nullable = false)
    private int durationMinutes;

    private Integer caloriesBurned;

    private String notes;

    public ExerciseLog() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public Integer getCaloriesBurned() { return caloriesBurned; }
    public void setCaloriesBurned(Integer caloriesBurned) { this.caloriesBurned = caloriesBurned; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}