package com.icesi.edu.users.controller;

import com.icesi.edu.users.api.UserAPI;
import com.icesi.edu.users.constant.ErrorConstants;
import com.icesi.edu.users.dto.UserDTO;
import com.icesi.edu.users.dto.UserNoPassDTO;

import com.icesi.edu.users.error.exception.UserDemoError;
import com.icesi.edu.users.error.exception.UserDemoException;
import com.icesi.edu.users.mapper.UserMapper;
import com.icesi.edu.users.service.UserService;
import lombok.AllArgsConstructor;
import org.passay.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
public class UserController implements UserAPI {


    public final UserService userService;

    public final UserMapper userMapper;

    @Override
    public UserDTO getUser(UUID userId) {
        return userMapper.fromUser(userService.getUser(userId));
    }

    @Override
    public UserDTO createUser(@Valid UserDTO userDTO) {

        //validatePassword(userDTO.getPassword());
        String validation = validateData(userDTO);
        if (validation.length() == 0) {
            return userMapper.fromUser(userService.createUser(userMapper.fromDTO(userDTO)));
        } else {
            validation = getExceptionMessage(validation);
            throw new UserDemoException(HttpStatus.BAD_REQUEST, new UserDemoError(validation.split("%")[0], validation.split("%")[1]));
        }


    }

    private void validatePassword(String password) {

        Properties props = new Properties();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("passay.properties");

        try {

            props.load(inputStream);
            MessageResolver resolver = new PropertiesMessageResolver(props);
            PasswordValidator passwordValidator = new PasswordValidator(resolver, Arrays.asList(
                    // at least one upper-case character
                    new CharacterRule(EnglishCharacterData.UpperCase, 1),
                    // at least one lower-case character
                    new CharacterRule(EnglishCharacterData.LowerCase, 1),
                    // at least one digit character
                    new CharacterRule(EnglishCharacterData.Digit, 1),
                    // at least one symbol (special character)
                    new CharacterRule(EnglishCharacterData.Special, 1))
            );

            RuleResult result = passwordValidator.validate(new PasswordData(password));

            List<String> messages = passwordValidator.getMessages(result);
            String messageTemplate = String.join(",", messages);
            if(!result.isValid()){
                throw new UserDemoException(HttpStatus.BAD_REQUEST, new UserDemoError("CODE_UD_12", messageTemplate));
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public String getExceptionMessage(String exceptionStack) {
        String msg = "";
        String codes = "";
        String[] parts = exceptionStack.split("%");

        for (String str : parts) {

            msg += str.split(":")[1] + " ";
            codes += str.split(":")[0] + " ";
        }

        return codes + "%" + msg;
    }


    public String validateData(UserDTO userDTO) {


        if (userDTO.getEmail() != null && userDTO.getPhoneNumber() != null) {
            return validateEmail(userDTO.getEmail()) + validatePhoneNumber(userDTO.getPhoneNumber()) + validateName(userDTO.getFirstName()) + validateName(userDTO.getLastName());
        } else if (userDTO.getEmail() != null) {
            return validateEmail(userDTO.getEmail()) + validateName(userDTO.getFirstName()) + validateName(userDTO.getLastName());
        } else {
            return validatePhoneNumber(userDTO.getPhoneNumber()) + validateName(userDTO.getFirstName()) + validateName(userDTO.getLastName());
        }


    }

    @Override
    public List<UserNoPassDTO> getUsers() {
        return userService.getUsers().stream().map(userMapper::fromUserNoPass).collect(Collectors.toList());
    }

    private String validateEmail(String email) {

        String output = "";
        String[] parts = email.split("@");
        if (!(parts[0].matches("^[0-9a-zA-Z]+$") && parts[1].equals("icesi.edu.co"))) {
            output = "5:" + ErrorConstants.CODE_UD_05 + "%";
        }

        return output;

    }

    private String validatePhoneNumber(String phoneNumber) {

        String prefix = phoneNumber.substring(0, 3);
        String number = phoneNumber.substring(3);

        if (prefix.equals("+57") && number.matches("^[0-9]+$") && number.length() == 10) {
            return "";
        } else {
            return "6:" + ErrorConstants.CODE_UD_06 + "%";
        }

    }

    private String validateName(String name) {
        if (name.length() <= 120 && name.matches("^[a-zA-Z]+$")) {
            return "";
        } else {
            return "7:" + ErrorConstants.CODE_UD_07 + "%";
        }
    }

}
