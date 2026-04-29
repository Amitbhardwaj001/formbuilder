package com.yourform.formbuilder.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import com.yourform.formbuilder.security.JwtUtil;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

 private final Key key =
  Keys.secretKeyFor(
   SignatureAlgorithm.HS256
  );

 public String generateToken(
      String username){

   return Jwts.builder()
      .setSubject(username)
      .setIssuedAt(
        new Date())
      .setExpiration(
        new Date(
         System.currentTimeMillis()
         +86400000
        ))
      .signWith(key)
      .compact();
 }
}