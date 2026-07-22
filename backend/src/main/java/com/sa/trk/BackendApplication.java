package com.sa.trk;

import java.util.Map;

import com.sa.trk.config.NexonProperties;
import com.sa.trk.config.OpenAiProperties;
import com.sa.trk.config.LocalEnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({NexonProperties.class, OpenAiProperties.class})
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(BackendApplication.class);
        Map<String, Object> localEnvProperties = LocalEnvLoader.load();
        if (!localEnvProperties.isEmpty()) {
            application.setDefaultProperties(localEnvProperties);
        }
        application.run(args);
    }
}
