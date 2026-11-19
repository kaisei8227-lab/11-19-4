package com.example.eventreserve.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * どの画面でもヘッダーに「〇〇さん」「ログアウト」を出せるよう、
 * ログイン中の会員を loginUser という名前で画面に渡す。
 */
@ControllerAdvice
public class GlobalModelAdvice {

    @ModelAttribute("loginUser")
    public LoginUser loginUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return (session == null) ? null : (LoginUser) session.getAttribute(LoginUser.SESSION_KEY);
    }
}
