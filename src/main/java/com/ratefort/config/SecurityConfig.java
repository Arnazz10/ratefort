package com.ratefort.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.authentication.WebFilterChainServerAuthenticationSuccessHandler;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
                                                            AuthenticationWebFilter apiKeyAuthFilter) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .exceptionHandling(exceptionHandlingSpec -> exceptionHandlingSpec
                        .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/api/**").authenticated()
                        .anyExchange().permitAll())
                .addFilterAt(apiKeyAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    public AuthenticationWebFilter apiKeyAuthFilter(ApiSecurityProperties apiSecurityProperties) {
        ReactiveAuthenticationManager authenticationManager = authentication -> {
            String principal = String.valueOf(authentication.getPrincipal());
            if (apiSecurityProperties.getValidApiKeys().contains(principal)) {
                Authentication authenticated = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        principal, principal, AuthorityUtils.NO_AUTHORITIES);
                return Mono.just(authenticated);
            }
            return Mono.error(new BadCredentialsException("Invalid API key"));
        };

        ServerAuthenticationConverter converter = exchange -> Mono.justOrEmpty(
                exchange.getRequest().getHeaders().getFirst("X-API-KEY"))
                .map(apiKey -> new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(apiKey, apiKey));

        AuthenticationWebFilter webFilter = new AuthenticationWebFilter(authenticationManager);
        webFilter.setServerAuthenticationConverter(converter);
        webFilter.setRequiresAuthenticationMatcher(
                org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers("/api/**"));
        webFilter.setAuthenticationSuccessHandler(new WebFilterChainServerAuthenticationSuccessHandler());
        webFilter.setAuthenticationFailureHandler(
                (webFilterExchange, exception) -> Mono.fromRunnable(
                        () -> webFilterExchange.getExchange().getResponse().setStatusCode(HttpStatus.UNAUTHORIZED)));
        return webFilter;
    }
}
