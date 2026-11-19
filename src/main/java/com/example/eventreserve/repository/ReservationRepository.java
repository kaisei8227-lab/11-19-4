package com.example.eventreserve.repository;

import com.example.eventreserve.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    long countByEventId(Long eventId);

    boolean existsByEventIdAndMemberId(Long eventId, Long memberId);

    List<Reservation> findByEventIdOrderByCreatedAtAsc(Long eventId);
}
