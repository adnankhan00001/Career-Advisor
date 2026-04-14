package com.careeradvisor.backend.model;

import jakarta.persistence.*;
import lombok.Setter;

@Entity
@Table(name = "users")

public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @Setter
    @Column(unique = true)
    private String email;
    @Setter
    private String password;

    public User(){}

    public Long getId(){
        return id;
    }
    public String getName(){
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

}
