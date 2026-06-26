package com.ddarahakit.course.domain.roadmap.model;

import com.ddarahakit.course.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Entity
public class Roadmap extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 200)
    private String image;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "roadmap", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoadmapCourse> roadmapCourses = new ArrayList<>();

    public void update(String name, String image, String description) {
        this.name = name;
        this.image = image;
        this.description = description;
    }
}
