package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.configuration;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view.CardColors;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view.RememberedUser;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view.UiFormat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.time.Duration;
import java.util.Locale;

/**
 * Web UI wiring: language switch (?lang=en | ?lang=es_MX, remembered in a cookie; English by default)
 * restoring the remembered user on /ui pages, and the helper beans the templates use.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("fm_lang");
        resolver.setDefaultLocale(Locale.US);
        resolver.setCookieMaxAge(Duration.ofDays(365));
        return resolver;
    }

    /** Money / month formatting for templates: {@code ${@fmt.money(x)}}. */
    @Bean("fmt")
    public UiFormat uiFormat() {
        return new UiFormat();
    }

    /** Card colours for templates and controllers: {@code ${@cardColors.of(account)}}. */
    @Bean("cardColors")
    public CardColors cardColors() {
        return new CardColors();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        LocaleChangeInterceptor localeChange = new LocaleChangeInterceptor();
        localeChange.setParamName("lang");
        localeChange.setIgnoreInvalidLocale(true);
        registry.addInterceptor(localeChange);
        registry.addInterceptor(new RememberedUser()).addPathPatterns("/ui/**", "/ui");
    }
}
