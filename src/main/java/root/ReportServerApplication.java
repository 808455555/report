package root;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication(exclude={MongoAutoConfiguration.class,MongoDataAutoConfiguration.class})
@PropertySource(value={"file:config/application.properties"})
public class ReportServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReportServerApplication.class, args);
	}
}
