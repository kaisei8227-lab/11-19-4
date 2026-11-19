package com.example.eventreserve.service;

import com.example.eventreserve.entity.Event;
import com.example.eventreserve.entity.Member;
import com.example.eventreserve.entity.Reservation;
import com.example.eventreserve.form.ReservationForm;
import com.example.eventreserve.repository.EventRepository;
import com.example.eventreserve.repository.MemberRepository;
import com.example.eventreserve.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {

    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;

    public ReservationService(EventRepository eventRepository,
                              ReservationRepository reservationRepository,
                              MemberRepository memberRepository) {
        this.eventRepository = eventRepository;
        this.reservationRepository = reservationRepository;
        this.memberRepository = memberRepository;
    }

    /**
     * 予約を確定する。
     * イベントの行をロックしてから数えるので、同時に押されても定員を超えない。
     */
    @Transactional
    public Reservation reserve(Long eventId, Long memberId, ReservationForm form) {
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new BusinessException("イベントが見つかりません"));

        if (event.isClosed()) {
            throw new BusinessException("予約期限を過ぎたため予約できません");
        }
        if (reservationRepository.existsByEventIdAndMemberId(eventId, memberId)) {
            throw new BusinessException("このイベントはすでに予約済みです");
        }
        long count = reservationRepository.countByEventId(eventId);
        if (count >= event.getCapacity()) {
            throw new BusinessException("満席のため予約できませんでした");
        }

        Member member = memberRepository.getReferenceById(memberId);
        Reservation reservation = new Reservation(
                event,
                member,
                form.getFullName().trim(),
                form.getFullNameKana().trim(),
                form.getPhone().trim(),
                blankToNull(form.getSchool()),
                blankToNull(form.getNote()));
        return reservationRepository.save(reservation);
    }

    /** 予約完了ページ用。本人の予約でなければ見せない */
    @Transactional(readOnly = true)
    public Reservation getOwn(Long reservationId, Long memberId) {
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException("予約が見つかりません"));
        if (!r.getMember().getId().equals(memberId)) {
            throw new BusinessException("予約が見つかりません");
        }
        return r;
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
