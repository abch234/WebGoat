package com.example.restservice;

import java.util.concurrent.atomic.AtomicLong;
import java.util.ArrayList;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.jdo.Extent;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;

@RestController
public class GreetingController {

  private static final PersistenceManagerFactory pmfInstance = JDOHelper.getPersistenceManagerFactory("transactions-optional");

  public static PersistenceManager getPM() {
    return pmfInstance.getPersistenceManager();
  }

  @GetMapping("/test1")
  public String test1(@RequestParam(value = "name", defaultValue = "World") String name, HttpServletResponse response) {
    PersistenceManager pm = getPM();
    // ruleid: jdo-sqli
    pm.newQuery(UserEntity.class,new ArrayList(),"id == " + name);
    return "ok";
  }

  @GetMapping("/test2")
  public String test2(@RequestBody String name, HttpServletResponse response) {
    PersistenceManager pm = getPM();
    // ruleid: jdo-sqli
    pm.newQuery("select * from Users where name = " + name);
    return "ok";
  }

  @GetMapping("/test3/{name}")
  public String test3(@PathVariable String name) {
    PersistenceManager pm = getPM();
    Query q = pm.newQuery(UserEntity.class);
    // ruleid: jdo-sqli
    q.setGrouping(name);
    return "ok";
  }

  @GetMapping("/test4/")
  public String test4(@RequestHeader("my-name") String name) {
    PersistenceManager pm = getPM();
    Query q = pm.newQuery(UserEntity.class);
    // ruleid: jdo-sqli
    q.setFilter("id == " + name);
    return "ok";
  }

  @GetMapping("/ok-test1")
  public String okTest1(@RequestBody String name, HttpServletResponse response) {
    PersistenceManager pm = getPM();
    String val = "Foobar";
    // ok: jdo-sqli
    pm.newQuery("select * from Users where name = " + val);
    return "ok";
  }

  @GetMapping("/ok-test2/{name}")
  public String okTest2(@PathVariable String name) {
    PersistenceManager pm = getPM();
    Query q = pm.newQuery(UserEntity.class);
    // ok: jdo-sqli
    q.differentMethod(name);
    return "ok";
  }

  public String okTest3(String name) {
    PersistenceManager pm = getPM();
    Query q = pm.newQuery(UserEntity.class);
    // ok: jdo-sqli
    q.setFilter("id == " + name);
    return "ok";
  }

  @GetMapping("/ok4/")
  public String okTest4(@RequestHeader("my-name") Integer name) {
    PersistenceManager pm = getPM();
    Query q = pm.newQuery(UserEntity.class);
    // ok: jdo-sqli
    q.setFilter("id == " + name);
    return "ok";
  }

  @GetMapping("/ok5/")
  public String okTest5(@RequestHeader("my-name") String name) {
    PersistenceManager pm = getPM();
    Query q = pm.newQuery(UserEntity.class);
    // ok: jdo-sqli
    q.setFilter("id == " + (name.concat("yeet") != "yeet3"));
    return "ok";
  }
}
