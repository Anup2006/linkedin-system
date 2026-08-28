package com.linkedin.user_service.service;

import com.linkedin.user_service.dto.AuthResponse;
import com.linkedin.user_service.dto.LoginRequest;
import com.linkedin.user_service.dto.RegisterRequest;
import com.linkedin.user_service.entity.User;
import com.linkedin.user_service.entity.UserRole;
import com.linkedin.user_service.repository.UserRepo;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final UserRepo userRepo;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    private final KafkaTemplate<String,Object> kafkaTemplate;

    private static final String USER_CREATED_TOPIC="user.created";

    public AuthResponse register(RegisterRequest request){
        log.info("Registering user:{}",request.getEmail());

        if(userRepo.existsByEmail(request.getEmail())){
            throw new RuntimeException("Email already registered:"+request.getEmail());
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setHeadLine(request.getHeadLine());
        user.setLocation(request.getLocation());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.NORMAL_USER);

        User saved = userRepo.save(user);
        log.info("User registered: {}",saved.getId());


        //publish user.created event
        //search service consume this and indexes user

        Map<String,Object> userCreatedEvent =  new HashMap<>();
        userCreatedEvent.put("userId",saved.getId());
        userCreatedEvent.put("firstName",saved.getFirstName());
        userCreatedEvent.put("lastName",saved.getLastName());
        userCreatedEvent.put("email",saved.getEmail());
        userCreatedEvent.put("headline",saved.getHeadLine());
        userCreatedEvent.put("location",saved.getLocation());


        kafkaTemplate.send(USER_CREATED_TOPIC,saved.getId(),userCreatedEvent);

        log.info("user.created event published: {}",saved.getId());

        String token = generateToken(saved.getId(),saved.getEmail());

        return buildAuthResponse(saved,token);
    }



    private String generateToken(String userId, String email) {

        return Jwts.builder()
                .claim("userId",userId)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(
                        System.currentTimeMillis()+jwtExpiration
                ))
                .signWith(getSigninKey(), SignatureAlgorithm.HS256)
                .compact();

    }

    private Key getSigninKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        AuthResponse response = new AuthResponse();
        response.setAccessToken(token);
        response.setRefreshToken(generateRefreshToken(user.getId()));
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());

        return response;

    }

    private String generateRefreshToken(String userId) {
        return Jwts.builder()
                .claim("userId",userId)
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(
                        System.currentTimeMillis()+refreshExpiration
                ))
                .signWith(getSigninKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public AuthResponse login(@Valid LoginRequest request) {
        log.info("Login attempt:{}",request.getEmail());

        User user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(()-> new RuntimeException("User not found:"+request.getEmail()));


        //Bcrypt verify - compare raw pass with stored hash
        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())){
            throw new RuntimeException("Invalid Credentials");
        }

        log.info("Login successful: {}",user.getId());

        //Generate JWT token
        String token = generateToken(user.getId(),user.getEmail());

        return buildAuthResponse(user,token);

    }
}















