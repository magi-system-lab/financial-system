package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;

/**
 * Keeps the chosen user across HTTP sessions with a long-lived cookie.
 * iOS discards session cookies whenever it closes a Home Screen web app, so without this the
 * user would land on the email screen (and lose the page they were on) on almost every launch.
 * As an interceptor on /ui/** it restores {@code userEmail} into a fresh session from the cookie.
 */
public class RememberedUser implements HandlerInterceptor {

    static final String COOKIE = "fm_user";
    static final String SESSION_ATTR = "userEmail";
    private static final int MAX_AGE = (int) Duration.ofDays(365).toSeconds();

    public static void remember(HttpServletResponse response, String email) {
        response.addCookie(cookie(URLEncoder.encode(email, StandardCharsets.UTF_8), MAX_AGE));
    }

    public static void forget(HttpSession session, HttpServletResponse response) {
        session.removeAttribute(SESSION_ATTR);
        response.addCookie(cookie("", 0));
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        HttpSession existing = request.getSession(false);
        if (existing != null && existing.getAttribute(SESSION_ATTR) != null) return true;
        if (request.getCookies() == null) return true;
        Arrays.stream(request.getCookies())
                .filter(c -> COOKIE.equals(c.getName()) && !c.getValue().isBlank())
                .findFirst()
                .ifPresent(c -> request.getSession().setAttribute(SESSION_ATTR,
                        URLDecoder.decode(c.getValue(), StandardCharsets.UTF_8)));
        return true;
    }

    private static Cookie cookie(String value, int maxAge) {
        Cookie cookie = new Cookie(COOKIE, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "Lax");
        return cookie;
    }
}
