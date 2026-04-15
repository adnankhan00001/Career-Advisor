package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
@Controller
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/signup")
    public String signup(@RequestBody User user) {
        userService.registerUser(user);
        return "User registered Successfully";
    }

    @PostMapping("/login")
    public String login(@RequestBody User user){
        return userService.loginUser(user.getEmail(), user.getPassword());
    }
}