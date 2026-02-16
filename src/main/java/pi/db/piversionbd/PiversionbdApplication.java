package pi.db.piversionbd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PiversionbdApplication {

    public static void main(String[] args) {

        SpringApplication.run(PiversionbdApplication.class, args);
        System.out.println("\nApplication démarrée avec succès !");
        System.out.println("Accédez à Swagger UI : http://localhost:8080/swagger-ui.html\n");
    }

}
