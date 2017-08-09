package com.example.demospring.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.web.filter.AbstractRequestLoggingFilter;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.UnsupportedEncodingException;

@Configuration
public class Beans {

  @Autowired
  private Environment env;

  @Bean
  DataSource dataSource(){
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(env.getProperty("spring.datasource.url"));
    config.setUsername(env.getProperty("spring.datasource.username"));
    config.setPassword(env.getProperty("spring.datasource.password"));
    return new HikariDataSource(config);
  }

  @Bean
  Flyway flyway(){
    Flyway f = new Flyway();
    f.setDataSource(dataSource());
    f.setLocations(env.getProperty("flyway.locations"));
    return f;
  }

  @Bean
  RedisConnectionFactory redisConnectionFactory(
      @Value("${spring.redis.host}") String host,
      @Value("${spring.redis.port}") int port
      ){
    JedisConnectionFactory jedis =  new JedisConnectionFactory();
    jedis.setHostName(host);
    jedis.setPort(port);
    return jedis;
  }

  @Bean
  public RequestFilter logFilter() {
    RequestFilter filter =new RequestFilter();
    filter.setIncludePayload(true);
    filter.setMaxPayloadLength(1000);
    return filter;
  }
}

@Configuration
class WebMvcContext extends WebMvcConfigurerAdapter {
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new RequestInterceptor());
  }
}

class RequestFilter extends AbstractRequestLoggingFilter {
  @Override
  protected void beforeRequest(HttpServletRequest request, String message) {
    ContentCachingRequestWrapper wrapper = WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
  }

  @Override
  protected void afterRequest(HttpServletRequest request, String message) {
    System.out.println("body from filter -> " + message);
  }
}

class RequestInterceptor extends HandlerInterceptorAdapter {
  private final int PAYLOAD_SIZE = 1024;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    ContentCachingRequestWrapper wrapper = WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
    return true;
  }

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    StringBuilder msg = new StringBuilder();
    ContentCachingRequestWrapper wrapper = WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
    if (wrapper != null) {
      byte[] buf = wrapper.getContentAsByteArray();
      if (buf.length > 0) {
        int length = Math.min(buf.length, PAYLOAD_SIZE);
        String payload;
        try {
          payload = new String(buf, 0, length, wrapper.getCharacterEncoding());
        }
        catch (UnsupportedEncodingException e) {
          payload = "[unknown]";
        }
        msg.append(payload);
      }
    }
    System.out.printf("from interceptor -> %s [ %d ]\n", msg.toString(), response.getStatus());
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

  }
}