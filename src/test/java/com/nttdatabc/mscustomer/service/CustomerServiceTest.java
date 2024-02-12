package com.nttdatabc.mscustomer.service;

import com.nttdatabc.mscustomer.model.AuthorizedSigner;
import com.nttdatabc.mscustomer.model.Customer;
import com.nttdatabc.mscustomer.repository.CustomerRepository;
import com.nttdatabc.mscustomer.utils.exceptions.errors.ErrorResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

@SpringBootTest
public class CustomerServiceTest {
  @Mock
  CustomerRepository customerRepository;
  @InjectMocks
  CustomerServiceImpl customerService;

  @BeforeEach
  public void setup(){
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void getCustomerById_Found(){
    // Arrange
    final String CUSTOMER_ID = "12312";
    Customer customer = new Customer();
    when(customerRepository.findById(CUSTOMER_ID))
        .thenReturn(Mono.just(customer));

    // Act
    Mono<Customer> result = customerService.getCustomerByIdService(CUSTOMER_ID);

    // Assert
    StepVerifier.create(result)
        .expectNext(customer)
        .verifyComplete();

    // Verify
    verify(customerRepository).findById(CUSTOMER_ID);
  }
  @Test
  public void getCustomerById_Not_Found(){
    // Arrange
    final String CUSTOMER_ID = "12312";
    when(customerRepository.findById(CUSTOMER_ID))
        .thenReturn(Mono.empty());

    // Act
    Mono<Customer> result = customerService.getCustomerByIdService(CUSTOMER_ID);

    // Assert
    StepVerifier.create(result)
        .expectError(ErrorResponseException.class)
        .verify();

    verify(customerRepository).findById(CUSTOMER_ID);
  }

  @Test
  public void deleteCustomer_Found(){
    final String CUSTOMER_ID = "12312";
    // Arrange
    Customer customer = new Customer();
    when(customerRepository.findById(CUSTOMER_ID))
        .thenReturn(Mono.just(customer));
    when(customerRepository.delete(customer))
        .thenReturn(Mono.empty());

    // Act
    Mono<Void> result = customerService.deleteCustomerByIdService(CUSTOMER_ID);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    verify(customerRepository).findById(CUSTOMER_ID);

    verify(customerRepository).delete(customer);
  }
  @Test
  public void deleteCustomer_Not_Found(){
    // Arrange
    final String CUSTOMER_ID = "12312";
    when(customerRepository.findById(CUSTOMER_ID))
        .thenReturn(Mono.empty());

    // Act
    Mono<Void> result = customerService.deleteCustomerByIdService(CUSTOMER_ID);

    // Assert
    StepVerifier.create(result)
        .expectError(ErrorResponseException.class)
        .verify();

    verify(customerRepository).findById(CUSTOMER_ID);
    verify(customerRepository, never()).delete(any());
  }

  @Test
  void getAuthorizedSignersByCustomerIdService_ShouldReturnAuthorizedSigners_WhenCustomerExistsWithSigners() {
    // Arrange
    final String CUSTOMER_ID = "12312";
    List<AuthorizedSigner> authorizedSigners = new ArrayList<>();
    authorizedSigners.add(new AuthorizedSigner());
    authorizedSigners.add(new AuthorizedSigner());
    Customer customer = new Customer();
    customer.setAuthorizedSigners(authorizedSigners);
    when(customerRepository.findById(CUSTOMER_ID))
        .thenReturn(Mono.just(customer));

    // Act
    Flux<AuthorizedSigner> result = customerService.getAuthorizedSignersByCustomerIdService(CUSTOMER_ID);

    // Assert
    StepVerifier.create(result)
        .expectNextSequence(authorizedSigners)
        .verifyComplete();

    verify(customerRepository).findById(CUSTOMER_ID);
  }
  @Test
  void getAuthorizedSignersByCustomerIdService_ShouldReturnError_WhenCustomerDoesNotExist() {
    // Arrange
    final String CUSTOMER_ID = "12312";
    when(customerRepository.findById(CUSTOMER_ID))
        .thenReturn(Mono.empty());

    // Act
    Flux<AuthorizedSigner> result = customerService.getAuthorizedSignersByCustomerIdService(CUSTOMER_ID);

    // Assert
    StepVerifier.create(result)
        .expectError(ErrorResponseException.class);

    verify(customerRepository).findById(CUSTOMER_ID);
  }
  @Test
  void getAuthorizedSignersByCustomerIdService_ShouldReturnError_WhenCustomerExistsWithoutSigners() {
    // Arrange
    final String CUSTOMER_ID = "12312";
    Customer customer = new Customer();
    when(customerRepository.findById(CUSTOMER_ID))
        .thenReturn(Mono.just(customer));

    // Act
    Flux<AuthorizedSigner> result = customerService.getAuthorizedSignersByCustomerIdService(CUSTOMER_ID);

    // Assert
    StepVerifier.create(result)
        .expectError(ErrorResponseException.class)
        .verify();

    verify(customerRepository).findById(CUSTOMER_ID);
  }

  @Test
  void updateCustomerService_ShouldUpdateCustomer_WhenValidCustomerProvided() {
    // Arrange
    Customer existingCustomer = new Customer();
    existingCustomer.setId("342423");
    existingCustomer.setAddress("New Address");
    existingCustomer.setBirthday("New Birthday");
    existingCustomer.setEmail("New Email");
    existingCustomer.setFullname("New Fullname");
    existingCustomer.setPhone("New Phone");
    existingCustomer.setType("persona");
    existingCustomer.setIdentifier("4343");
    Customer updatedCustomer = new Customer();
    updatedCustomer.setId("324423");
    updatedCustomer.setType("persona");
    updatedCustomer.setAddress("New Address");
    updatedCustomer.setBirthday("New Birthday");
    updatedCustomer.setEmail("New Email");
    updatedCustomer.setFullname("New Fullname");
    updatedCustomer.setPhone("New Phone");
    updatedCustomer.setIdentifier("232132");

    when(customerRepository.findById(updatedCustomer.getId()))
        .thenReturn(Mono.just(existingCustomer));
    when(customerRepository.save(existingCustomer))
        .thenReturn(Mono.just(existingCustomer));
    when(customerRepository.findByIdentifier(updatedCustomer.getIdentifier())).thenReturn(Mono.empty());

    // Act
    Mono<Void> result = customerService.updateCustomerService(updatedCustomer);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    verify(customerRepository).findById(updatedCustomer.getId());

    verify(customerRepository).save(existingCustomer);

  }
  @Test
  void updateCustomerService_ShouldUpdateCustomer_Error() {
    // Arrange
    Customer existingCustomer = new Customer();
    existingCustomer.setId("342423");
    existingCustomer.setAddress("New Address");
    existingCustomer.setBirthday("New Birthday");
    existingCustomer.setEmail("New Email");
    existingCustomer.setFullname("New Fullname");
    existingCustomer.setPhone("New Phone");
    existingCustomer.setType("persona");
    existingCustomer.setIdentifier("4343");
    Customer updatedCustomer = new Customer();
    updatedCustomer.setId("324423");
    updatedCustomer.setType("persona");
    updatedCustomer.setAddress("New Address");
    updatedCustomer.setBirthday("New Birthday");
    updatedCustomer.setEmail("New Email");
    updatedCustomer.setFullname("New Fullname");
    updatedCustomer.setPhone("New Phone");
    updatedCustomer.setIdentifier("232132");

    when(customerRepository.findById(updatedCustomer.getId()))
        .thenReturn(Mono.just(existingCustomer));
    when(customerRepository.save(existingCustomer))
        .thenReturn(Mono.just(existingCustomer));
    when(customerRepository.findByIdentifier(updatedCustomer.getIdentifier())).thenReturn(Mono.just(new Customer()));

    // Act
    Mono<Void> result = customerService.updateCustomerService(updatedCustomer);

    // Assert
    StepVerifier.create(result)
        .expectError(ErrorResponseException.class);
  }

  @Test
  void createCustomerService_ShouldCreateCustomer_Succes() {
    // Arrange
    Customer customer = new Customer();
    customer.setId("342423");
    customer.setAddress("New Address");
    customer.setBirthday("New Birthday");
    customer.setEmail("New Email");
    customer.setFullname("New Fullname");
    customer.setPhone("New Phone");
    customer.setType("persona");
    customer.setIdentifier("4343");

    when(customerRepository.save(customer))
        .thenReturn(Mono.just(customer));
    when(customerRepository.findByIdentifier(customer.getIdentifier())).thenReturn(Mono.empty());

    // Act
    Mono<Void> result = customerService.createCustomerService(customer);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    verify(customerRepository).save(customer);
  }
  @Test
  void createCustomerService_ShouldCreateCustomer_Error() {
    // Arrange
    Customer customer = new Customer();
    customer.setId("342423");
    customer.setAddress("New Address");
    customer.setBirthday("New Birthday");
    customer.setEmail("New Email");
    customer.setFullname("New Fullname");
    customer.setPhone("New Phone");
    customer.setType("error");
    customer.setIdentifier("4343");

    when(customerRepository.save(customer))
        .thenReturn(Mono.just(customer));
    when(customerRepository.findByIdentifier(customer.getIdentifier())).thenReturn(Mono.empty());

    // Act
    Mono<Void> result = customerService.createCustomerService(customer);

    // Assert
    StepVerifier.create(result)
        .expectError(ErrorResponseException.class);

    verify(customerRepository,never()).save(customer);
  }
}
