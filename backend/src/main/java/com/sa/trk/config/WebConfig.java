package com.sa.trk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.sa.trk.security.SecurityRequestInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final WebCorsProperties corsProperties;
    private final SecurityRequestInterceptor securityRequestInterceptor;

    public WebConfig(
            WebCorsProperties corsProperties,
            SecurityRequestInterceptor securityRequestInterceptor) {
        this.corsProperties = corsProperties;
        this.securityRequestInterceptor = securityRequestInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(corsProperties.getAllowedOriginPatterns().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(securityRequestInterceptor)
                .addPathPatterns("/api/**");
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
