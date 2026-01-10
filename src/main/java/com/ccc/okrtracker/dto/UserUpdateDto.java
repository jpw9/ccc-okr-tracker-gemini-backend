package com.ccc.okrtracker.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserUpdateDto {
    private String firstName;
    private String lastName;
    private String groupNo;
    private String email;
    private String login;
    private String avatar;
    private Long primaryProjectId;
    private Boolean isActive; // Assuming this can also be updated
    private List<Long> roleIds;
}