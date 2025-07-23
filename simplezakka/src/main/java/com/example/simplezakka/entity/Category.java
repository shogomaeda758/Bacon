package com.example.simplezakka.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
<<<<<<< HEAD

=======
>>>>>>> ac1336c3bd81dcdfd17ae553f29dc9b6379f7d13
import java.time.LocalDateTime;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer categoryId;

    @Column(nullable = false)
    private String categoryName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

<<<<<<< HEAD
    /**
     * テストコード等で category.setName(...) を呼び出す場合の補助メソッド。
     */
    public void setName(String name) {
        this.categoryName = name;
=======
    public void setName(String string) {
        
        throw new UnsupportedOperationException("Unimplemented method 'setName'");
>>>>>>> ac1336c3bd81dcdfd17ae553f29dc9b6379f7d13
    }
}
