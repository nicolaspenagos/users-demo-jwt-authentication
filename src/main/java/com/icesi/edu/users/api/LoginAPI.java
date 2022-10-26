package com.icesi.edu.users.api;

import com.icesi.edu.users.dto.LoginDTO;
import com.icesi.edu.users.dto.TokenDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/login")
public interface LoginAPI {

    @PostMapping
    TokenDTO login(@RequestBody LoginDTO loginDTO);

}