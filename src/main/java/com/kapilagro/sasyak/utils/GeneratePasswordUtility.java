package com.kapilagro.sasyak.utils;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class GeneratePasswordUtility {

    public String generateRandomPassword() {
        String upperCaseLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCaseLetters = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String specialCharacters = "!@#$%^&*()-_=+[]{}|;:,.<>?";

        String allChars = upperCaseLetters + lowerCaseLetters + numbers + specialCharacters;
        Random random = new Random();

        // Generate a password of length 12
        char[] password = new char[12];

        // Ensure at least one character from each category
        password[0] = upperCaseLetters.charAt(random.nextInt(upperCaseLetters.length()));
        password[1] = lowerCaseLetters.charAt(random.nextInt(lowerCaseLetters.length()));
        password[2] = numbers.charAt(random.nextInt(numbers.length()));
        password[3] = specialCharacters.charAt(random.nextInt(specialCharacters.length()));

        // Fill the rest randomly
        for (int i = 4; i < 12; i++) {
            password[i] = allChars.charAt(random.nextInt(allChars.length()));
        }

        // Shuffle the password characters
        for (int i = 0; i < password.length; i++) {
            int j = random.nextInt(password.length);
            char temp = password[i];
            password[i] = password[j];
            password[j] = temp;
        }
        //System.out.println("Generated password in generyvgdab: " + new String(password));
        return new String(password);
    }
}
