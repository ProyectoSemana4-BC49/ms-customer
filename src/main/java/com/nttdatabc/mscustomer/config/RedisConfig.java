package com.nttdatabc.mscustomer.config;

import com.nttdatabc.mscustomer.model.Customer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.*;


@Configuration
public class RedisConfig {

  @Bean("customerReactiveRedisTemplate")
  public ReactiveRedisTemplate<String, Customer> reactiveRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
    RedisSerializationContext<String, Customer> serializationContext = RedisSerializationContext
        .<String, Customer>newSerializationContext(new StringRedisSerializer())
        .key(new StringRedisSerializer())
        .value(new Jackson2JsonRedisSerializer<>(Customer.class))
        .hashKey(new Jackson2JsonRedisSerializer<>(Integer.class))
        .hashValue(new GenericJackson2JsonRedisSerializer())
        .build();
    return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
  }
}
