package com.curaai.curaai.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "nutrition_logs")
public class NutritionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date = LocalDate.now();

    private String foodName;

    private String mealType;

    @Column(nullable = false)
    private int calories;

    private Integer protein;
    private Integer carbs;
    private Integer fat;
    private Integer fiber;
    private Integer sugar;
    private Integer healthScore;

    @Column(length = 1000)
    private String insight;

    public NutritionLog() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }

    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }

    public int getCalories() { return calories; }
    public void setCalories(int calories) { this.calories = calories; }

    public Integer getProtein() { return protein; }
    public void setProtein(Integer protein) { this.protein = protein; }

    public Integer getCarbs() { return carbs; }
    public void setCarbs(Integer carbs) { this.carbs = carbs; }

    public Integer getFat() { return fat; }
    public void setFat(Integer fat) { this.fat = fat; }

    public Integer getFiber() { return fiber; }
    public void setFiber(Integer fiber) { this.fiber = fiber; }

    public Integer getSugar() { return sugar; }
    public void setSugar(Integer sugar) { this.sugar = sugar; }

    public Integer getHealthScore() { return healthScore; }
    public void setHealthScore(Integer healthScore) { this.healthScore = healthScore; }

    public String getInsight() { return insight; }
    public void setInsight(String insight) { this.insight = insight; }
}
