package com.daeyang.SmartFactoryWeb.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        try {
            HttpServletResponse res = (HttpServletResponse) response;

            res.setHeader("Content-Security-Policy",
                    "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline'; " +
                    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdnjs.cloudflare.com; " +
                    "font-src https://fonts.gstatic.com https://cdnjs.cloudflare.com; " +
                    "img-src 'self' data:; " +
                    "object-src 'none'; " +
                    "frame-ancestors 'none';");

            res.setHeader("X-Frame-Options", "DENY");
            res.setHeader("X-Content-Type-Options", "nosniff");
            res.setHeader("Referrer-Policy", "same-origin");

            chain.doFilter(request, response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
