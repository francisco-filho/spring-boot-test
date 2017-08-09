package com.example.demospring;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@SpringBootApplication
public class DemoSpringApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoSpringApplication.class, args);
	}

	@Bean
	CommandLineRunner migrations(Flyway flyway, StringRedisTemplate template) {
		ValueOperations<String, String> values = template.opsForValue();
		return args -> {
			flyway.setBaselineOnMigrate(true);
			flyway.migrate();
			values.set("bar", "newfoo", 10, TimeUnit.SECONDS);
			System.out.println("value of foo -> " + values.get("bar"));
			System.out.println("value of foo -> " + values.get("foo"));
		};
	}

	@PostMapping("/doit")
	public Produto doit(@RequestBody Produto body){
		System.out.println("body from method -> " + body);
		return body;
	}
}

class Produto {
	private int id;
	private String name;

	public Produto(){}

	public Produto(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Produto produto = (Produto) o;

		if (id != produto.id) return false;
		return name.equals(produto.name);
	}

	@Override
	public int hashCode() {
		int result = id;
		result = 31 * result + (name != null ? name.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "Produto{" +
				"id=" + id +
				", name='" + name + '\'' +
				'}';
	}
}


