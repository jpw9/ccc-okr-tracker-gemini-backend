package com.ccc.okrtracker.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "app_users") // 'user' is a reserved keyword in Postgres
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class User extends BaseEntity {
    private String firstName;
    private String lastName;
    private String groupNo;
    private String email;

    @Column(unique = true)
    private String login;

    private String avatar; // Initials usually

    private Long primaryProjectId; // Simple linking for now

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
}