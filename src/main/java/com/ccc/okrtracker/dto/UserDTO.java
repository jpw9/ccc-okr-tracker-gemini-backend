package com.ccc.okrtracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * DTO for User updates, allowing role assignment by ID.
 * This prevents the issue where the backend overwrites roles with an empty set
 * when receiving updates without the full Role objects.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String groupNo;
    private String email;
    private String login;
    private String avatar;
    private Long primaryProjectId;
    private Set<Long> roleIds; // Role IDs for assignment
}
