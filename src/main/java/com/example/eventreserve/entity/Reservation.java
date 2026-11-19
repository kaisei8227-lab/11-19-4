package com.example.eventreserve.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * 予約。予約ページで入力した個人情報もここに保存する。
 * 同じ会員が同じイベントを2回予約できないよう、DBでも一意制約をかける。
 */
@Entity
@Table(name = "reservations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "member_id"}))
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false, length = 50)
    private String fullName;

    @Column(nullable = false, length = 50)
    private String fullNameKana;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 100)
    private String school;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Reservation() {
    }

    public Reservation(Event event, Member member, String fullName, String fullNameKana,
                       String phone, String school, String note) {
        this.event = event;
        this.member = member;
        this.fullName = fullName;
        this.fullNameKana = fullNameKana;
        this.phone = phone;
        this.school = school;
        this.note = note;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Event getEvent() { return event; }
    public Member getMember() { return member; }
    public String getFullName() { return fullName; }
    public String getFullNameKana() { return fullNameKana; }
    public String getPhone() { return phone; }
    public String getSchool() { return school; }
    public String getNote() { return note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
