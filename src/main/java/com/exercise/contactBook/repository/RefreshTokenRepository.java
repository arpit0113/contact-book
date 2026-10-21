package com.exercise.contactBook.repository;

import com.exercise.contactBook.model.RefreshToken;
import com.exercise.contactBook.model.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken , Long> {

   Optional<RefreshToken> findByTokenHash(String tokenHash);

   @Transactional
   @Modifying
   @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.family = :family")
   void revokeFamily(@Param("family") String family);

   @Transactional
   @Modifying
   @Query("DELETE FROM RefreshToken r WHERE r.user = :user")
   void deleteByUser(@Param("user") User user);

   @Transactional
   @Modifying
   @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.id = :id AND r.revoked = false")
   int revokeIfActive(@Param("id") Long id);

   @Transactional
   @Modifying
   @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now")
   int deleteExpired(@Param("now") Instant now);
}
