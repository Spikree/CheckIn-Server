package com.learnspring.checkinserver.repository;

import com.learnspring.checkinserver.model.ParentStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParentStudentRepository extends JpaRepository<ParentStudent, Long> {

    // Check if ANY link exists (Pending or Accepted) to prevent duplicates
    boolean existsByRequesterIdAndReceiverId(Long reqId, Long recId);
    boolean existsByReceiverIdAndRequesterId(Long recId, Long reqId); // Check reverse direction too!

    // Find requests sent TO me that are PENDING
    List<ParentStudent> findByReceiverIdAndStatus(Long receiverId, ParentStudent.Status status);

    // Find the specific request to Accept/Reject
    Optional<ParentStudent> findByIdAndReceiverId(Long id, Long receiverId);

    // 1. Find requests I SENT
    List<ParentStudent> findByRequesterIdAndStatus(Long requesterId, ParentStudent.Status status);

    // 2. Find a specific request I SENT (Secure Delete)
    Optional<ParentStudent> findByIdAndRequesterId(Long id, Long requesterId);

    // Check if two users are linked (Accepted) regardless of who sent the invite
    @Query("SELECT COUNT(ps) > 0 FROM ParentStudent ps WHERE ps.status = 'ACCEPTED' AND " +
            "((ps.requester.id = :user1Id AND ps.receiver.id = :user2Id) OR " +
            "(ps.requester.id = :user2Id AND ps.receiver.id = :user1Id))")
    boolean areUsersLinked(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    // Find ALL accepted links where I am involved (either as requester or receiver)
    @Query("SELECT ps FROM ParentStudent ps WHERE ps.status = 'ACCEPTED' AND " +
            "(ps.requester.id = :userId OR ps.receiver.id = :userId)")
    List<ParentStudent> findAllAcceptedLinks(@Param("userId") Long userId);
}