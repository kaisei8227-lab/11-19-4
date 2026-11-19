package com.example.eventreserve.controller;

import com.example.eventreserve.config.LoginUser;
import com.example.eventreserve.entity.Member;
import com.example.eventreserve.entity.Role;
import com.example.eventreserve.form.LoginForm;
import com.example.eventreserve.form.RegisterForm;
import com.example.eventreserve.service.BusinessException;
import com.example.eventreserve.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Optional;

@Controller
public class AuthController {

    private final MemberService memberService;

    public AuthController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/events";
    }

    // ---------- ログイン ----------

    @GetMapping("/login")
    public String loginForm(@ModelAttribute LoginForm loginForm) {
        return "login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute LoginForm loginForm, BindingResult result,
                        HttpServletRequest request, Model model) {
        if (result.hasErrors()) {
            return "login";
        }
        Optional<Member> member = memberService.authenticate(
                loginForm.getEmail(), loginForm.getPassword(), loginForm.getRole());
        if (member.isEmpty()) {
            // どれが違うかは教えない（他人のメールアドレスが登録済みか探られないように）
            model.addAttribute("loginError",
                    "メールアドレスまたはパスワードが違います。学生／企業の選択も確認してください");
            return "login";
        }
        startSession(request, member.get());
        return member.get().isCompany() ? "redirect:/admin/events" : "redirect:/events";
    }

    // ---------- 新規会員登録 ----------

    @GetMapping("/register")
    public String registerForm(@ModelAttribute RegisterForm registerForm) {
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterForm registerForm, BindingResult result,
                           HttpServletRequest request) {
        if (registerForm.getPassword() != null
                && !registerForm.getPassword().equals(registerForm.getPasswordConfirm())) {
            result.rejectValue("passwordConfirm", "mismatch", "パスワードが一致しません");
        }
        // 企業を選んだときは企業名が必須
        if (registerForm.getRole() == Role.COMPANY
                && (registerForm.getCompanyName() == null || registerForm.getCompanyName().isBlank())) {
            result.rejectValue("companyName", "required", "企業名を入力してください");
        }
        if (result.hasErrors()) {
            return "register";
        }
        try {
            // 登録と同時にメールアドレスとパスワード（ハッシュ）をDBに保管する
            Member member = memberService.register(registerForm.getEmail(), registerForm.getPassword(),
                    registerForm.getRole(), registerForm.getCompanyName());
            // 登録したらそのままログイン状態にする
            startSession(request, member);
            return member.isCompany() ? "redirect:/admin/events" : "redirect:/events";
        } catch (BusinessException e) {
            result.rejectValue("email", "duplicate", e.getMessage());
            return "register";
        }
    }

    // ---------- ログアウト ----------

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login?logout";
    }

    /** 古いセッションを捨てて新しく作る（セッション固定攻撃の対策） */
    private void startSession(HttpServletRequest request, Member member) {
        HttpSession old = request.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(LoginUser.SESSION_KEY, new LoginUser(member));
    }
}
