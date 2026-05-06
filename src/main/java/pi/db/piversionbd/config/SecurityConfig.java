package pi.db.piversionbd.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.Customizer;

import java.util.List;
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtTokenProvider jwtTokenProvider;

    public SecurityConfig(UserDetailsService userDetailsService, JwtTokenProvider jwtTokenProvider) {
        this.userDetailsService = userDetailsService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // No WWW-Authenticate: Basic — avoids the browser username/password bar; clients use JWT only.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/members/auth/**",
                                "/api/hedera/status",
                                "/api/claims/test-hedera/**",
                                "/recaptcha-test.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        // Public registration/member-creation entrypoints
                        .requestMatchers(HttpMethod.POST, "/api/members").permitAll()
                        // Public pré-inscription funnel (no JWT): submit, OCR, Q&A, uploads
                        .requestMatchers(HttpMethod.POST, "/api/pre-registration").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/pre-registration/form").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/pre-registration/ocr/cin").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/pre-registration/medical-history/qa").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/pre-registration/*/cin/upload").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/pre-registration/medical-history/*/upload").permitAll()
                        // Allow full pre-registration workflow to everyone
                        .requestMatchers("/api/pre-registration/**").permitAll()
                        // Public group catalog (browse before login); mutations stay MEMBER/ADMIN below
                        .requestMatchers(HttpMethod.GET, "/api/groups").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/groups/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/groups/*/pool").permitAll()
                        // Public endpoints needed for the rest of the member onboarding funnel before login
                        .requestMatchers(HttpMethod.GET, "/api/members/*/group-suggestions").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/groups/*/members").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payments/memberships/*").permitAll()
                        // Admin-only APIs
                        .requestMatchers(
                                "/api/admin/**",
                                "/api/system-alerts/**",
                                "/api/member-churn-forecasts/**",
                                "/api/platform-kpi-snapshots/**",
                                "/api/retention-interventions/**",
                                "/api/chatbot/**",
                                "/api/rewards/catalog/**",
                                "/api/claim-scorings/**",
                                "/api/adherence-tracking/**",
                                "/api/member-rewards/**"
                        ).hasRole("ADMIN")
                        // Member app endpoints (admins keep access too)
                        .requestMatchers(
                                "/api/members/**",
                                "/api/memberships/**",
                                "/api/payments/**",
                                "/api/claims/**",
                                "/api/group-change-requests/**",
                                "/api/groups",
                                "/api/groups/**",
                                "/api/groups/*/chat/**",
                                // Health module (read/write allowed for authenticated members/admins)
                                "/api/doctors/**",
                                "/api/consultations/**",
                                "/api/produits/**"
                        ).hasAnyRole("MEMBER", "ADMIN")
                        .anyRequest().authenticated()
                )
                // JWT Bearer auth must run before BasicAuthenticationFilter,
                // otherwise Swagger will be forced into username/password prompts.
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), BasicAuthenticationFilter.class)
                .authenticationProvider(authenticationProvider());
        return http.build();

    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
