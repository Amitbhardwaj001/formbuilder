package com.yourform.formbuilder.security;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

 @Bean
 SecurityFilterChain filterChain(
   HttpSecurity http)
   throws Exception {

  http
 .csrf(csrf->csrf.disable())

 .authorizeHttpRequests(auth->auth
    .requestMatchers(
       "/auth/**",
       "/api/forms/**",
       "/api/responses/**"
     ).permitAll()

     .anyRequest()
     .authenticated()
 );

   return http.build();
 }
}