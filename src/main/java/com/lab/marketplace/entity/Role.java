package com.lab.marketplace.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String name;

    @Column(length = 100)
    private String description;

    @ManyToMany(mappedBy = "roles")
    @Builder.Default
    private Set<User> users = new HashSet<>();

    // Role constants
    public static final String ADMIN = "ADMIN";
    public static final String SELLER = "SELLER";
    public static final String BUYER = "BUYER";

    public Role(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
