package at.spengergasse.sj2526pos1javaseed.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(org.springframework.security.config.Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/webjars/**",
                    "/favicon.ico",
                    "/doc/**",
                    "/docs/**"
                ).permitAll()
                // benchmark api für frontend/k6, kein login nötig
                .requestMatchers("/items", "/items/**", "/tests", "/tests/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/", "/films", "/series", "/customers", "/orders", "/stores", "/docs", "/docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1.0.0/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1.0.0/**").hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers(HttpMethod.PATCH, "/api/v1.0.0/**").hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers(HttpMethod.PUT, "/api/v1.0.0/**").hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers(HttpMethod.DELETE, "/api/v1.0.0/**").hasAnyRole("ADMIN", "EMPLOYEE")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.withDefaultPasswordEncoder()
                .username("admin")
                .password("admin")
                .roles("ADMIN", "EMPLOYEE")
                .build();

        UserDetails employee = User.withDefaultPasswordEncoder()
                .username("employee")
                .password("employee")
                .roles("EMPLOYEE")
                .build();

        UserDetails customer = User.withDefaultPasswordEncoder()
                .username("customer")
                .password("customer")
                .roles("CUSTOMER")
                .build();

        return new InMemoryUserDetailsManager(admin, employee, customer);
    }
}
