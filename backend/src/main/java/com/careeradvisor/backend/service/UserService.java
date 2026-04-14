package com.careeradvisor.backend.service;

import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public User registerUser(User user) {

        // check duplicate email
        if (userRepository.findByEmail(user.getEmail()) != null) {
            throw new RuntimeException("Email already exists");
        }

        // 🔐 hash password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }
    public String loginUser(String email, String password){
        User user = userRepository.findByEmail(email);
        if(user==null){
            return "USer not Found";
        }
        if(passwordEncoder.matches(password, user.getPassword())){
            return "Login Successful";
        }else{
            return "Invalid Password";
        }
    }
}