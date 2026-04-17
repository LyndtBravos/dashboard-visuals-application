package com.mediahost.dashboard.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "user")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "Name", length = 100)
    private String name;

    @Column(name = "UserID", length = 4, unique = true)
    private String userId;

    @Column(name = "Password", length = 20)
    private String password;

    @Column(name = "DT")
    private LocalDateTime dt;

    @Column(name = "level")
    private Integer level;

    @Column(name = "email", length = 50)
    private String email;

    @Column(name = "Statusid", nullable = false)
    private Integer statusId = 1;

    @Column(name = "Shift")
    private Integer shift = 0;

    @Column(name = "RegionId")
    private Integer regionId;
}

//package com.mediahost.dashboard.model.entity;
//
//import jakarta.persistence.*;
//import jakarta.validation.constraints.*;
//import lombok.Data;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "user")
//@Data
//public class User {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Integer id;
//
//    @Size(max = 100, message = "Name cannot exceed 100 characters")
//    @Column(name = "Name")
//    private String name;
//
//    @NotBlank(message = "UserID is required")
//    @Size(min = 3, max = 3, message = "UserID must be exactly 3 characters")
//    @Pattern(regexp = "^[A-Za-z0-9]{3}$", message = "UserID must be alphanumeric and exactly 3 characters")
//    @Column(name = "UserID", unique = true, length = 3)
//    private String userId;
//
//    @NotBlank(message = "Password is required")
//    @Size(min = 3, max = 20, message = "Password must be between 3 and 20 characters")
//    @Column(name = "Password", length = 20)
//    private String password;
//
//    @Past(message = "Date must be in the past")
//    @Column(name = "DT")
//    private LocalDateTime dt;
//
//    @Min(value = 0, message = "Level must be at least 0")
//    @Max(value = 999, message = "Level cannot exceed 999")
//    @Column(name = "level")
//    private Integer level;
//
//    @Email(message = "Email should be valid")
//    @Size(max = 50, message = "Email cannot exceed 50 characters")
//    @Column(name = "email", length = 50)
//    private String email;
//
//    @NotNull(message = "Statusid is required")
//    @Min(value = 1, message = "Statusid must be at least 1")
//    @Column(name = "Statusid", nullable = false)
//    private Integer statusId = 1;
//
//    @Min(value = 0, message = "Shift must be at least 0")
//    @Max(value = 3, message = "Shift cannot exceed 3")
//    @Column(name = "Shift")
//    private Integer shift = 0;
//
//    @Column(name = "RegionId")
//    private Integer regionId;
//}