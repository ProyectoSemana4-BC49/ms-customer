package com.nttdatabc.mscustomer.service;

import static com.nttdatabc.mscustomer.utils.Constantes.EX_ERROR_PERSON_AUTH_SIGNER;
import static com.nttdatabc.mscustomer.utils.Constantes.EX_NOT_FOUND_RECURSO;
import static com.nttdatabc.mscustomer.utils.CustomerValidator.validateAuthorizedSignerEmpty;
import static com.nttdatabc.mscustomer.utils.CustomerValidator.validateAuthorizedSignerNoNulls;
import static com.nttdatabc.mscustomer.utils.CustomerValidator.validateAuthorizedSignerOnlyEmpresa;
import static com.nttdatabc.mscustomer.utils.CustomerValidator.validateCustomerEmpty;
import static com.nttdatabc.mscustomer.utils.CustomerValidator.validateCustomerNoNulls;
import static com.nttdatabc.mscustomer.utils.CustomerValidator.validateUserNotRegistred;
import static com.nttdatabc.mscustomer.utils.CustomerValidator.verifyTypePerson;
import static com.nttdatabc.mscustomer.utils.Utilitarios.generateUuid;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nttdatabc.mscustomer.model.AuthorizedSigner;
import com.nttdatabc.mscustomer.model.Customer;
import com.nttdatabc.mscustomer.repository.CustomerRepository;
import com.nttdatabc.mscustomer.utils.exceptions.errors.ErrorResponseException;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


/**
 * Clase del CustomerServiceImpl.
 */
@Service
public class CustomerServiceImpl implements CustomerService {
  @Autowired
  private CustomerRepository customerRepository;

  @Autowired
  private KafkaTemplate kafkaTemplate;
  @Autowired
  @Qualifier("customerReactiveRedisTemplate")
  private ReactiveRedisTemplate<String, Customer> redisTemplate;


  @Override
  public Flux<Customer> getAllCustomersService() {
    String cacheKey = "customers";
    Duration cacheDuration = Duration.ofSeconds(50);
    return redisTemplate.opsForList().range(cacheKey, 0, -1)
        .switchIfEmpty(
            customerRepository.findAll()
                .flatMap(customers -> redisTemplate.opsForList().leftPushAll(cacheKey, customers)
                    .thenMany(Flux.just(customers))))
        .cache(cacheDuration)
        .doOnSubscribe(subscription -> redisTemplate.expire(cacheKey, cacheDuration).subscribe());
  }

  @Override
  public Mono<Void> createCustomerService(Customer customer) {
    return validateCustomerNoNulls(customer)
        .then(validateCustomerEmpty(customer))
        .then(verifyTypePerson(customer))
        .then(validateUserNotRegistred(customer.getIdentifier(), customerRepository))
        .then(validateAuthorizedSignerOnlyEmpresa(customer))
        .then(Mono.fromSupplier(() -> {
          customer.setId(generateUuid());
          return customer;
        }))
        .flatMap(customerRepository::save)
        .then();
  }

  @Override
  public Mono<Customer> getCustomerByIdService(String customerId) {
    return customerRepository.findById(customerId)
        .switchIfEmpty(Mono.error(new ErrorResponseException(EX_NOT_FOUND_RECURSO,
            HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND)));
  }

  @Override
  public Mono<Void> updateCustomerService(Customer customer) {
    return validateCustomerNoNulls(customer)
        .then(validateCustomerEmpty(customer))
        .then(verifyTypePerson(customer))
        .then(validateUserNotRegistred(customer.getIdentifier(), customerRepository))
        .then(validateAuthorizedSignerOnlyEmpresa(customer))
        .then(Mono.just(customer))
        .flatMap(customerFlujo -> getCustomerByIdService(customerFlujo.getId()))
        .map(customerUpdate -> {
          customerUpdate.setAddress(customer.getAddress());
          customerUpdate.setBirthday(customer.getBirthday());
          customerUpdate.setEmail(customer.getEmail());
          customerUpdate.setFullname(customer.getFullname());
          customerUpdate.setPhone(customer.getPhone());
          customerUpdate.setAuthorizedSigners(customer.getAuthorizedSigners());
          return customerUpdate;
        })
        .flatMap(customerRepository::save)
        .then();
  }

  @Override
  public Mono<Void> deleteCustomerByIdService(String customerId) {
    return getCustomerByIdService(customerId)
        .flatMap(customerRepository::delete)
        .then();
  }

  @Override
  public Flux<AuthorizedSigner> getAuthorizedSignersByCustomerIdService(String customerId) {
    return getCustomerByIdService(customerId)
        .flux()
        .flatMap(customer -> {
          if (customer.getAuthorizedSigners() == null) {
            return Flux.error(new ErrorResponseException(EX_ERROR_PERSON_AUTH_SIGNER,
                HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT));
          } else {
            return Flux.fromIterable(customer.getAuthorizedSigners());
          }
        });
  }

  @Override
  public Mono<Void> createAuthorizedSignersByCustomerId(String customerId, AuthorizedSigner authorizedSigner) {
    return validateAuthorizedSignerNoNulls(authorizedSigner)
        .then(validateAuthorizedSignerEmpty(authorizedSigner))
        .then(getCustomerByIdService(customerId))
        .map(customer -> {
          List<AuthorizedSigner> existingSigners = customer.getAuthorizedSigners();
          if (Objects.isNull(existingSigners)) {
            existingSigners = new ArrayList<>();
          }
          existingSigners.add(authorizedSigner);
          customer.setAuthorizedSigners(existingSigners);
          return customer;
        })
        .flatMap(customerRepository::save)
        .then();
  }


  @KafkaListener(topics = {"verify-customer-exist"}, groupId = "my-group-id")
  public void listenerVerifyCustomer(String message) {
    Gson gson = new Gson();
    Map<String, String> map = gson.fromJson(message, new TypeToken<Map<String, String>>() {
    }.getType());
    String customerId = map.get("customerId");
    Mono<Customer> getCustomer = getCustomerByIdService(customerId);

    getCustomer.subscribe(customer -> {
      String jsonDataMono = gson.toJson(customer);
      kafkaTemplate.send("response-verify-customer-exist", jsonDataMono);
    },
        throwable -> {
          Map<String, String> responseError = new HashMap<>();
          responseError.put("error", throwable.getMessage());
          String responseString = gson.toJson(responseError);
          kafkaTemplate.send("response-verify-customer-exist", responseString);
        });

  }

  @KafkaListener(topics = {"verify-customer-exist-credit"}, groupId = "my-group-id")
  public void listenerVerifyCustomerCredit(String message) {
    Gson gson = new Gson();
    Map<String, String> map = gson.fromJson(message, new TypeToken<Map<String, String>>() {
    }.getType());
    String customerId = map.get("customerId");
    Mono<Customer> getCustomer = getCustomerByIdService(customerId);
    getCustomer.subscribe(
        customer -> {
          String jsonDataMono = gson.toJson(customer);
          kafkaTemplate.send("response-verify-customer-exist-credit", jsonDataMono);
        },
        throwable -> {
          Map<String, String> responseError = new HashMap<>();
          responseError.put("error", throwable.getMessage());
          String responseString = gson.toJson(responseError);
          kafkaTemplate.send("response-verify-customer-exist-credit", responseString);
        }
    );

  }

}
