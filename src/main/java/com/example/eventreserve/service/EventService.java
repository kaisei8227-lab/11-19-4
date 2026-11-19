package com.example.eventreserve.service;

import com.example.eventreserve.entity.Event;
import com.example.eventreserve.entity.Member;
import com.example.eventreserve.entity.Reservation;
import com.example.eventreserve.form.EventForm;
import com.example.eventreserve.repository.EventRepository;
import com.example.eventreserve.repository.MemberRepository;
import com.example.eventreserve.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;

    public EventService(EventRepository eventRepository,
                        ReservationRepository reservationRepository,
                        MemberRepository memberRepository) {
        this.eventRepository = eventRepository;
        this.reservationRepository = reservationRepository;
        this.memberRepository = memberRepository;
    }

    /**
     * 掲示板（イベント一覧）用。
     * 予約期限を過ぎたイベントは表示しない（開くたびに今の時刻で絞り込む）。
     */
    @Transactional(readOnly = true)
    public List<EventView> listForMember(Long memberId) {
        return eventRepository.findByDeadlineAfterOrderByStartAtAsc(LocalDateTime.now()).stream()
                .map(e -> toView(e, memberId))
                .toList();
    }

    /** 企業の管理画面用: 自社のイベントだけ */
    @Transactional(readOnly = true)
    public List<EventView> listForCompany(Long companyMemberId) {
        return eventRepository.findByCreatedByIdOrderByStartAtAsc(companyMemberId).stream()
                .map(e -> toView(e, companyMemberId))
                .toList();
    }

    @Transactional(readOnly = true)
    public EventView getView(Long eventId, Long memberId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException("イベントが見つかりません"));
        return toView(event, memberId);
    }

    /** 企業がイベントを作成して掲示板に追加する */
    @Transactional
    public Event publish(EventForm form, Long companyMemberId) {
        Member company = memberRepository.findById(companyMemberId)
                .orElseThrow(() -> new BusinessException("企業アカウントが見つかりません"));
        String description = (form.getDescription() == null || form.getDescription().isBlank())
                ? null : form.getDescription().trim();
        Event event = new Event(
                form.getTitle().trim(),
                company.getCompanyName(),   // 企業名はアカウントのものを使う
                form.getCapacity(),
                description,
                form.getLocation().trim(),
                form.getStartAt(),
                form.getDeadline(),
                company);
        return eventRepository.save(event);
    }

    /** 企業がイベントを取り下げる（予約もいっしょに消える） */
    @Transactional
    public void delete(Long eventId, Long companyMemberId) {
        Event event = findOwnEvent(eventId, companyMemberId);
        reservationRepository.deleteAll(reservationRepository.findByEventIdOrderByCreatedAtAsc(eventId));
        eventRepository.delete(event);
    }

    /** 企業が自社イベントの予約者一覧を見る */
    @Transactional(readOnly = true)
    public List<Reservation> reservationsOf(Long eventId, Long companyMemberId) {
        findOwnEvent(eventId, companyMemberId);
        return reservationRepository.findByEventIdOrderByCreatedAtAsc(eventId);
    }

    @Transactional(readOnly = true)
    public Event findOwnEvent(Long eventId, Long companyMemberId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException("イベントが見つかりません"));
        if (!event.getCreatedBy().getId().equals(companyMemberId)) {
            throw new BusinessException("このイベントを操作する権限がありません");
        }
        return event;
    }

    private EventView toView(Event e, Long memberId) {
        long count = reservationRepository.countByEventId(e.getId());
        boolean mine = memberId != null && reservationRepository.existsByEventIdAndMemberId(e.getId(), memberId);
        return new EventView(e, count, mine);
    }
}
