package com.example.test_spring.service;

import com.example.test_spring.model.Address;
import com.example.test_spring.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;


@Service
public class UserService {
    private final RestTemplate restTemplate = new RestTemplate();
    private String url;

    public UserService(@Value("${broker.url}") String brokerUrl) {
        url = brokerUrl + "/users";
    }


    public User login(String identifier, String password) {
        try {
            User loginAttempt = new User(null, identifier, password); // identifier can be username or email
            return restTemplate.postForObject(url + "/login", loginAttempt, User.class);
        }catch (HttpClientErrorException e){
            throw new HttpClientErrorException(e.getStatusCode());
        }catch (RestClientException e) {
            throw new RuntimeException("Unable to reach broker. Please try again later.");
        }
    }



    public void register(String username, String email, String password, Address address) {
        try {
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(password);
            user.setAddress(address);
            user.setIsAdmin(false);

            restTemplate.postForObject(url + "/register", user, Void.class);
        } catch (RestClientException e) {
            throw new RuntimeException("Unable to reach broker. Please try again later.");
        }
    }
}





