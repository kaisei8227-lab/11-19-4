package com.example.eventreserve.controller;

import com.example.eventreserve.config.LoginUser;
import com.example.eventreserve.form.EventForm;
import com.example.eventreserve.service.BusinessException;
import com.example.eventreserve.service.EventService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

/** 企業の管理画面（企業アカウントだけが開ける。LoginInterceptor でチェック） */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final EventService eventService;

    public AdminController(EventService eventService) {
        this.eventService = eventService;
    }

    /** 配信フォーム＋自社イベント一覧 */
    @GetMapping("/events")
    public String events(@SessionAttribute(LoginUser.SESSION_KEY) LoginUser user,
                         @ModelAttribute EventForm eventForm, Model model) {
        model.addAttribute("events", eventService.listForCompany(user.getId()));
        return "admin/events";
    }

    /** イベントを作成して掲示板に追加する */
    @PostMapping("/events")
    public String publish(@SessionAttribute(LoginUser.SESSION_KEY) LoginUser user,
                          @Valid @ModelAttribute EventForm eventForm, BindingResult result,
                          Model model, RedirectAttributes redirect) {
        checkDates(eventForm, result);
        if (result.hasErrors()) {
            model.addAttribute("events", eventService.listForCompany(user.getId()));
            return "admin/events";
        }
        eventService.publish(eventForm, user.getId());
        redirect.addFlashAttribute("message", "イベントを掲示板に追加しました");
        return "redirect:/admin/events";
    }

    /** 日時の前後関係のチェック */
    private void checkDates(EventForm form, BindingResult result) {
        LocalDateTime now = LocalDateTime.now();
        if (form.getStartAt() != null && form.getStartAt().isBefore(now)) {
            result.rejectValue("startAt", "past", "イベント日時は今より後にしてください");
        }
        if (form.getDeadline() != null && form.getDeadline().isBefore(now)) {
            result.rejectValue("deadline", "past", "予約期限は今より後にしてください");
        }
        if (form.getStartAt() != null && form.getDeadline() != null
                && form.getDeadline().isAfter(form.getStartAt())) {
            result.rejectValue("deadline", "afterStart", "予約期限はイベント日時より前にしてください");
        }
    }

    /** イベントを取り下げる */
    @PostMapping("/events/{id}/delete")
    public String delete(@PathVariable Long id,
                         @SessionAttribute(LoginUser.SESSION_KEY) LoginUser user,
                         RedirectAttributes redirect) {
        try {
            eventService.delete(id, user.getId());
            redirect.addFlashAttribute("message", "イベントを削除しました");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/events";
    }

    /** 予約者の一覧 */
    @GetMapping("/events/{id}/reservations")
    public String reservations(@PathVariable Long id,
                               @SessionAttribute(LoginUser.SESSION_KEY) LoginUser user,
                               Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("event", eventService.findOwnEvent(id, user.getId()));
            model.addAttribute("reservations", eventService.reservationsOf(id, user.getId()));
            return "admin/reservations";
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/events";
        }
    }
}
