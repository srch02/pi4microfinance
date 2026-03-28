package pi.db.piversionbd.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Pi4MicroFinance API")
                        .version("1.0.0")
                        .description("API Documentation for Pi4MicroFinance Health Management System")
                        .contact(new Contact()
                                .name("Pi4MicroFinance Team")
                                .email("contact@pi4microfinance.tn")
                                .url("https://pi4microfinance.tn"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}

