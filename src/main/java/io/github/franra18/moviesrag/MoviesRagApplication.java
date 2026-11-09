package io.github.franra18.moviesrag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MoviesRagApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoviesRagApplication.class, args);
	}

}
