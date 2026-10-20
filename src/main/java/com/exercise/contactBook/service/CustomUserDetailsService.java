package com.exercise.contactBook.service;

import com.exercise.contactBook.enums.UserStatus;
import com.exercise.contactBook.model.User;
import com.exercise.contactBook.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository){
        this.userRepository = userRepository;
    }


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email).orElseThrow(()-> new UsernameNotFoundException("User Not found with email" + email));

        List<GrantedAuthority> authorities = new ArrayList<>();
        if(user.getRole() != null){
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
        }
        if(user.getOrgRole() != null){
            user.getOrgRole().getPermissions()
                    .forEach( p -> authorities.add(new SimpleGrantedAuthority(p.name())));
        }
      return org.springframework.security.core.userdetails.User.builder()
              .username(user.getEmail())
              .password(user.getPassword()== null ?"" : user.getPassword())
              .authorities(authorities)
              .disabled(user.getStatus() == UserStatus.PENDING)
              .build();
    }
}
