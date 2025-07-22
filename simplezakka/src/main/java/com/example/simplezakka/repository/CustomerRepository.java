package com.example.simplezakka.repository;

import com.example.simplezakka.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    
    
    Optional<Customer> findByEmail(String email);
    
    
    boolean existsByEmail(String email);
    
    
    List<Customer> findByLastNameContainingOrFirstNameContaining(String lastName, String firstName);
    
    List<Customer> findByLastNameContainingOrFirstNameContainingOrPhoneNumberContaining(
            String lastName, String firstName, String phoneNumber);
    
    
    @Query("SELECT c FROM Customer c WHERE c.customerId = :customerId AND c.email = :email")
    Optional<Customer> findByCustomerIdAndEmail(@Param("customerId") Integer customerId, @Param("email") String email);

}