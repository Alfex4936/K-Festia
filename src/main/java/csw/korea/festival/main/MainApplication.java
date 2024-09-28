package csw.korea.festival.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

import java.util.concurrent.Executors;

@SpringBootApplication
public class MainApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(MainApplication.class);
		application.setLazyInitialization(true);
		application.run(args);
	}

	@Bean
	public TaskExecutor taskExecutor() {
		return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
	}
}
