package com.example.restservice;

import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.CookieValue;

import org.springframework.jdbc.core.JdbcTemplate;

@RestController
public class GreetingController {

	private static final String template = "Hello, %s!";
	private final AtomicLong counter = new AtomicLong();

  @Autowired
  JdbcTemplate jdbcTemplate;

  @Autowired
  SmthElse smthElse;

  @GetMapping("/test1")
  public String test1(@RequestParam(value = "name", defaultValue = "World") String name, HttpServletResponse response) {
    // ruleid: jdbctemplate-sqli
    jdbcTemplate.execute("INSERT INTO customers(first_name, last_name) VALUES ("+ name +", foobar)");
    return "ok";
  }

  @GetMapping("/test2")
  public String test2(@RequestBody String name, HttpServletResponse response) {
    // ruleid: jdbctemplate-sqli
    jdbcTemplate.batchUpdate(String.format("INSERT INTO customers(first_name, last_name) VALUES (%s,foobar)", name));
    return "ok";
  }

  @GetMapping("/test3/{name}")
  public String test3(@PathVariable String name) {
    // ruleid: jdbctemplate-sqli
    jdbcTemplate.query("SELECT id, first_name, last_name FROM customers WHERE first_name = " + name);
    return "ok";
  }

  @GetMapping("/ok-test1")
  public String okTest1(@RequestParam(value = "name", defaultValue = "World") String name, HttpServletResponse response) {
    // ok: jdbctemplate-sqli
    smthElse.execute("INSERT INTO customers(first_name, last_name) VALUES ("+ name +", foobar)");
    return "ok";
  }

  @GetMapping("/ok-test2")
  public String okTest2(@RequestBody String name, HttpServletResponse response) {
    // ok: jdbctemplate-sqli
    jdbcTemplate.batchUpdate("INSERT INTO customers(first_name, last_name) VALUES (?,foobar)", name);
    return "ok";
  }

  public String okTest3(String notUserInput) {
    // ok: jdbctemplate-sqli
    jdbcTemplate.query("SELECT id, first_name, last_name FROM customers WHERE first_name = " + notUserInput);
    return "ok";
  }

  @GetMapping("/ok-test4")
  public String okTest4(@RequestBody Boolean name, HttpServletResponse response) {
    // ok: jdbctemplate-sqli
    jdbcTemplate.batchUpdate(String.format("INSERT INTO customers(first_name, last_name) VALUES (%s,foobar)", name));
    return "ok";
  }

  @GetMapping("/ok-test5")
  public String okTest5(@RequestBody String name, HttpServletResponse response) {
    // ok: jdbctemplate-sqli
    jdbcTemplate.batchUpdate(String.format("INSERT INTO customers(first_name, last_name) VALUES (%s,foobar)", (name != null)));
    return "ok";
  }
}
