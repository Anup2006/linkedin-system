package com.linkedin.user_service.service;

import com.linkedin.user_service.dto.UserResponse;
import com.linkedin.user_service.entity.Connection;
import com.linkedin.user_service.entity.ConnectionStatus;
import com.linkedin.user_service.entity.User;
import com.linkedin.user_service.repository.ConnectionRepo;
import com.linkedin.user_service.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final ConnectionRepo connectionRepo;
    private final UserRepo userRepo;
    private final S3Service s3Service;

    private final KafkaTemplate<String,Object> kafkaTemplate;

    private static final String CONNECTION_REQUESTED_TOPIC="connection.requested";
    private static final String CONNECTION_ACCEPTED_TOPIC="connection.accepted";
    private static final String USER_UPDATED_TOPIC="user.updated";


    public UserResponse updateProfile(String userId, UserResponse request) {
        User user = userRepo.findById(userId)
                .orElseThrow(()-> new RuntimeException("User not found"+userId));

        user.setHeadLine(request.getHeadLine());
        user.setAbout(request.getAbout());
        user.setLocation(request.getLocation());
        user.setSkills(request.getSkills());

        User saved= userRepo.save(user);

        //Publish user.updated event
        Map<String,Object> userUpdatedEvent =  new HashMap<>();
        userUpdatedEvent.put("userId",saved.getId());
        userUpdatedEvent.put("firstName",saved.getFirstName());
        userUpdatedEvent.put("lastName",saved.getLastName());
        userUpdatedEvent.put("headline",saved.getHeadLine());
        userUpdatedEvent.put("location",saved.getLocation());
        userUpdatedEvent.put("skills",saved.getSkills());

        kafkaTemplate.send(USER_UPDATED_TOPIC,saved.getId(),userUpdatedEvent);

        log.info("user.updated event published:{}",saved.getId());

        return mapToResponse(saved);
    }


    public String sendConnectionRequest(String receiverId, String requesterId) {
        if(connectionRepo.existsByRequesterIdAndReceiverId(receiverId,requesterId)){
            throw new RuntimeException("Connection request already sent");
        }

        Connection connection = new Connection();
        connection.setRequesterId(requesterId);
        connection.setReceiverId(receiverId);
        connection.setStatus(ConnectionStatus.PENDING);

        connectionRepo.save(connection);

        //Publish connection.requested event
        Map<String,Object> connectionRequestedEvent = new HashMap<>();
        connectionRequestedEvent.put("requesterId",requesterId);
        connectionRequestedEvent.put("receiverId",receiverId);

        kafkaTemplate.send(CONNECTION_REQUESTED_TOPIC,requesterId,connectionRequestedEvent);

        log.info("Connection request sent:{} -> {}",requesterId,receiverId);

        return "Connection request sent";

    }

    public String acceptConnectionRequest(String connectionId) {
        Connection connection = connectionRepo.findById(connectionId)
                .orElseThrow(()-> new RuntimeException("Connection not found"+connectionId));

        connection.setStatus(ConnectionStatus.CONNECTED);
        connectionRepo.save(connection);

        //Publish connection.accepted event
        Map<String,Object> connectionAcceptedEvent = new HashMap<>();
        connectionAcceptedEvent.put("requesterId",connection.getRequesterId());
        connectionAcceptedEvent.put("receiverId",connection.getReceiverId());

        kafkaTemplate.send(CONNECTION_ACCEPTED_TOPIC,connection.getRequesterId(),connectionAcceptedEvent);

        log.info("Connection accepted: {}",connectionId);

        return "Connection accepted";
    }

    public List<UserResponse> getConnections(String userId) {
        List<Connection> connections= connectionRepo.findByRequesterIdAndStatus(userId,ConnectionStatus.CONNECTED);

        return connections.stream()
                .map(c->getUserProfile(c.getReceiverId()))
                .collect(Collectors.toList());
    }

    public UserResponse getUserProfile(String userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(()-> new RuntimeException("User not found"+userId));

        return mapToResponse(user);

    }

    private UserResponse mapToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setHeadLine(user.getHeadLine());
        response.setAbout(user.getAbout());
        response.setLocation(user.getLocation());
        response.setProfilePhotoUrl(user.getProfilePhotoUrl());
        response.setCoverPhotoUrl(user.getCoverPhotoUrl());
        response.setRole(user.getRole());
        response.setSkills(user.getSkills());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    public UserResponse uploadProfilePhoto(String userId, MultipartFile file) {
        User user = userRepo.findById(userId)
                .orElseThrow(()-> new RuntimeException("User not found"+userId));


        String photoUrl=s3Service.uploadFile(
                file,"profiles/" + userId + "/avatar"
        );


        user.setProfilePhotoUrl(photoUrl);
        User saved= userRepo.save(user);

        log.info("Profile photo uploaded for user:{}",userId);


        return  mapToResponse(saved);

    }
}
