package com.seichou.logos.service;

import com.seichou.logos.entity.User;
import com.seichou.logos.entity.UserPreferences;
import com.seichou.logos.repository.UserPreferencesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserPreferenceService {

    private final UserPreferencesRepository repository;

    public UserPreferences getPreferences(UUID userId) {
        return repository.findById(userId)
                .orElseGet(() -> {
                    // This should ideally not happen if initialized on registration
                    return UserPreferences.builder()
                            .userId(userId)
                            .theme("dark")
                            .language("en")
                            .aiPersonality("empathetic")
                            .notificationsEnabled(true)
                            .dataPrivacy("standard")
                            .build();
                });
    }

    @Transactional
    public UserPreferences updatePreferences(UUID userId, UserPreferences updates) {
        UserPreferences prefs = getPreferences(userId);
        if (updates.getTheme() != null) prefs.setTheme(updates.getTheme());
        if (updates.getLanguage() != null) prefs.setLanguage(updates.getLanguage());
        if (updates.getAiPersonality() != null) prefs.setAiPersonality(updates.getAiPersonality());
        if (updates.getNotificationsEnabled() != null) prefs.setNotificationsEnabled(updates.getNotificationsEnabled());
        if (updates.getDataPrivacy() != null) prefs.setDataPrivacy(updates.getDataPrivacy());
        return repository.save(prefs);
    }
}
