package com.example.eventreserve.controller;

import com.example.eventreserve.config.LoginUser;
import com.example.eventreserve.entity.Reservation;
import com.example.eventreserve.form.ReservationForm;
import com.example.eventreserve.service.BusinessException;
import com.example.eventreserve.service.EventService;
import com.example.eventreserve.service.EventView;
import com.example.eventreserve.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EventController {

    private final EventService eventService;
    private final ReservationService reservationService;

    public EventController(EventService eventService, ReservationService reservationService) {
        this.eventService = eventService;
        this.reservationService = reservationService;
    }

    /** イベント一覧（掲示板） */
    @GetMapping("/events")
    public String list(@SessionAttribute(LoginUser.SESSION_KEY) LoginUser user, Model model) {
        model.addAttribute("events", eventService.listForMember(user.getId()));
        return "events/list";
    }

    /** 予約ボタンを押したときの、個人情報入力ページ */
    @GetMapping("/events/{id}/reserve")
    public String reserveForm(@PathVariable Long id,
                              @SessionAttribute(LoginUser.SESSION_KEY) LoginUser user,
                              @ModelAttribute ReservationForm reservationForm,
                              Model model, RedirectAttributes redirect) {
        if (user.isCompany()) {
            redirect.addFlashAttribute("error", "企業アカウントでは予約できません");
            return "redirect:/events";
        }
        try {
            EventView view = eventService.getView(id, user.getId());
            if (!view.isReservable()) {
                redirect.addFlashAttribute("error", reasonNotReservable(view));
                return "redirect:/events";
            }
            model.addAttribute("view", view);
            return "events/reserve";
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/events";
        }
    }

    /** 予約完了ボタン */
    @PostMapping("/events/{id}/reserve")
    public String reserve(@PathVariable Long id,
                          @SessionAttribute(LoginUser.SESSION_KEY) LoginUser user,
                          @Valid @ModelAttribute ReservationForm reservationForm, BindingResult result,
                          Model model, RedirectAttributes redirect) {
        if (user.isCompany()) {
            redirect.addFlashAttribute("error", "企業アカウントでは予約できません");
            return "redirect:/events";
        }
        try {
            if (result.hasErrors()) {
                model.addAttribute("view", eventService.getView(id, user.getId()));
                return "events/reserve";
            }
            Reservation r = reservationService.reserve(id, user.getId(), reservationForm);
            // 完了ページへリダイレクト（再読み込みで二重予約にならないように）
            return "redirect:/reservations/" + r.getId() + "/complete";
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/events";
        }
    }

    /** 予約完了ページ */
    @GetMapping("/reservations/{id}/complete")
    public String complete(@PathVariable Long id,
                           @SessionAttribute(LoginUser.SESSION_KEY) LoginUser user,
                           Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("reservation", reservationService.getOwn(id, user.getId()));
            return "events/complete";
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/events";
        }
    }

    private String reasonNotReservable(EventView view) {
        if (view.isReservedByMe()) return "このイベントはすでに予約済みです";
        if (view.getEvent().isClosed()) return "予約期限を過ぎたため予約できません";
        return "満席のため予約できません";
    }
}
