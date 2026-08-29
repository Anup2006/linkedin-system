package com.linkedin.user_service.repository;

import com.linkedin.user_service.entity.Connection;
import com.linkedin.user_service.entity.ConnectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConnectionRepo extends JpaRepository<Connection,String> {
    boolean existsByRequesterIdAndReceiverId(String receiverId, String requesterId);

    List<Connection> findByRequesterIdAndStatus(String userId, ConnectionStatus status);

    List<Connection> findByReceiverIdAndStatus(String userId, ConnectionStatus status);

}
