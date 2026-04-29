package com.yourform.formbuilder.controller;

import com.yourform.formbuilder.model.User;
import com.yourform.formbuilder.repository.UserRepository;
import com.yourform.formbuilder.security.JwtUtil;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository repo;

    

    private final JwtUtil jwtUtil;
    public AuthController(
 UserRepository repo,
 JwtUtil jwtUtil){

 this.repo=repo;
 this.jwtUtil=jwtUtil;
}

    @PostMapping("/register")
    public String register(
            @RequestBody User user){

        repo.save(user);

        return "User registered";
    }
    @PostMapping("/login")
public String login(
 @RequestBody User user){

   User dbUser =
      repo.findByUsername(
       user.getUsername()
      ).orElseThrow(
       () -> new RuntimeException(
          "User not found"
       )
      );

   if(!dbUser.getPassword()
       .equals(
          user.getPassword())){

      throw new RuntimeException(
          "Invalid password");
   }

   return jwtUtil.generateToken(
      user.getUsername());
}
}