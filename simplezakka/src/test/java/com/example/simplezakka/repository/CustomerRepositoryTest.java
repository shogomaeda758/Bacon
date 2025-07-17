package com.example.simplezakka.repository;

import com.example.simplezakka.entity.Customer;
import com.example.simplezakka.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class CustomerRepositoryTest {

    @Autowired
    CustomerRepository customerRepository;

    @Test
    void findByEmail_Exists_ReturnsCustomer() {
        Customer c = new Customer();
        c.setLastName("森");
        c.setFirstName("進一");
        c.setEmail("shinichi@moriforest.com");
        c.setPassword("password123");
        c.setAddress("山形県");
        c.setPhoneNumber("090-3333-2222");
        customerRepository.save(c);
        Optional<Customer> found = customerRepository.findByEmail("shinichi@moriforest.com");
        assertTrue(found.isPresent());
        assertEquals("森", found.get().getLastName());
        assertEquals("進一", found.get().getFirstName());
    }

    @Test
    void findByEmail_NotFound_ReturnsEmpty() {
        Optional<Customer> found = customerRepository.findByEmail("none@nowhere.com");
        assertTrue(found.isEmpty());
    }

    @Test
    void existsByEmail_Exists_ReturnsTrue() {
        Customer c = new Customer();
        c.setLastName("吉田");
        c.setFirstName("京子");
        c.setEmail("kyoko@yoshida.net");
        c.setPassword("xx");
        c.setAddress("新潟県長岡市");
        c.setPhoneNumber("090-8787-0000");
        customerRepository.save(c);
        assertTrue(customerRepository.existsByEmail("kyoko@yoshida.net"));
    }

    @Test
    void existsByEmail_NotExists_ReturnsFalse() {
        assertFalse(customerRepository.existsByEmail("unknown@net.com"));
    }

    @Test
    void findByNameContainingOrPhoneNumberContaining_Match_ReturnsList() {
        Customer c = new Customer();
        c.setLastName("田中");
        c.setFirstName("花子");
        c.setEmail("hanako@tanaka.com");
        c.setPassword("aa");
        c.setAddress("熊本県");
        c.setPhoneNumber("080-9999-8888");
        customerRepository.save(c);
        List<Customer> result = customerRepository.findByLastNameContainingOrFirstNameContainingOrPhoneNumberContaining("田中", "花子", "9999");
        assertFalse(result.isEmpty());
        assertEquals("花子", result.get(0).getFirstName());
    }
}

