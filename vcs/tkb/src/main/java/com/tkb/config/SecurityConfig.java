//package com.tkb.config;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Bean;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.web.SecurityFilterChain;
//
//@Configuration
//public class SecurityConfig {
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http.authorizeHttpRequests(authorize -> authorize
//                        .requestMatchers(
//                                "/login",
//                                "/doc.html",
//                                "/v3/api-docs/**",
//                                "/swagger-ui/**",
//                                "/webjars/**" , //  webjars 也放行
//                                "/swagger-ui.html",
//                                "/favicon.ico"
//                        ).permitAll()
//                        .anyRequest().authenticated()
//                )
//                .csrf(csrf -> csrf.disable()) // 禁用 CSRF
//                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable())); // 禁用 X-Frame-Options
//
//        return http.build();
//    }
//}
